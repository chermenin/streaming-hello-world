package com.epam.sample

import org.apache.spark.streaming.{Seconds, State, StateSpec, Time}
import org.apache.spark.streaming.dstream.DStream

class SparkDurationJob extends SparkBaseJob("local[2]", "Spark Duration Job") {

  override def process(stream: DStream[DeviceMessage]): DStream[IndexMessage] = {
    // Put your code here...


  }
}

object SparkDurationJob {

  def main(args: Array[String]): Unit = {
    new SparkDurationJob().execute()
  }
}
