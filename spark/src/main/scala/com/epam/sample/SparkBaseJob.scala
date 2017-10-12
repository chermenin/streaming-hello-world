package com.epam.sample

import java.util.Date

import org.elasticsearch.spark._
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.joda.time.DateTime

case class DeviceMessage(timestamp: Date, device: String, ip: String)

case class IndexMessage(timestamp: Date, value: Long)

abstract class SparkBaseJob(master: String, appName: String) {

  def execute(): Unit = {
    val ssc = new StreamingContext(new SparkContext(master, appName), Seconds(1))
    ssc.sparkContext.getConf.set("es.index.auto.create", "true")
    ssc.checkpoint("/tmp/spark/checkpoints/duration-job")

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "kafka:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> ("spark-default-" + System.currentTimeMillis()),
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val sentInputStream = KafkaUtils.createDirectStream[String, String](
      ssc, PreferConsistent, Subscribe[String, String](Seq("spark_sent_log_input"), kafkaParams)
    ).map(_.value())

    val receivedInputStream = KafkaUtils.createDirectStream[String, String](
      ssc, PreferConsistent, Subscribe[String, String](Seq("spark_received_log_input"), kafkaParams)
    ).map(_.value())

    val sentRegexp = "^(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d+)\\s+\\S+\\s+\\[dev\\s\\#(.+)\\]\\sSent\\s\\d+\\sbytes\\sto\\s(.+)".r
    val receivedRegexp = "^(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d+)\\s+\\S+\\s+\\[(.+)\\]\\sReceived\\s\\d+\\sbytes\\sfrom\\s(.+)".r
    val deviceMessages = sentInputStream.union(receivedInputStream).flatMap {
      case sentRegexp(date, device, ip) => Seq(DeviceMessage(DateTime.parse(date).toDate, device, ip))
      case receivedRegexp(date, ip, device) => Seq(DeviceMessage(DateTime.parse(date).toDate, device, ip))
      case _ => Seq()
    }

    val result = process(deviceMessages)

    result.foreachRDD(_.saveToEs("spark-index/duration_type",
      Map[String, String]("es.mapping,timestamp" -> "timestamp")))

    ssc.start()
    ssc.awaitTerminationOrTimeout(180000)
  }

  def process(stream: DStream[DeviceMessage]): DStream[IndexMessage]
}
