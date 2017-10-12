package com.epam.sample;

import com.epam.sample.events.DeviceMessage;
import com.epam.sample.events.IndexMessage;
import com.epam.sample.events.ReceivedDeviceMessage;
import com.epam.sample.events.SentDeviceMessage;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch5.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.streaming.util.serialization.JSONDeserializationSchema;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class FlinkBaseJob {

    private static final Pattern SENT_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d+)\\s+\\S+\\s+\\[dev\\s#(.+)]\\sSent\\s\\d+\\sbytes\\sto\\s(.+)$");

    private static final Pattern RECEIVED_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d+)\\s+\\S+\\s+\\[(.+)]\\sReceived\\s\\d+\\sbytes\\sfrom\\s(.+)$");

    public void execute() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Properties kafkaProperties = new Properties();
        kafkaProperties.setProperty("bootstrap.servers", "kafka:9092");
        kafkaProperties.setProperty("group.id", "flink-default-" + System.currentTimeMillis());

        FlinkKafkaConsumer010<ObjectNode> sentKafkaConsumer =
                new FlinkKafkaConsumer010<>("flink_sent_log_input", new JSONDeserializationSchema(), kafkaProperties);
        sentKafkaConsumer.setStartFromEarliest();

        FlinkKafkaConsumer010<ObjectNode> receivedKafkaConsumer =
                new FlinkKafkaConsumer010<>("flink_received_log_input", new JSONDeserializationSchema(), kafkaProperties);
        receivedKafkaConsumer.setStartFromEarliest();

        DataStream<String> sentKafkaMessages =
                env.addSource(sentKafkaConsumer)
                        .map((MapFunction<ObjectNode, String>) node -> node.get("message").asText())
                        .returns(String.class);

        DataStream<String> receivedKafkaMessages =
                env.addSource(receivedKafkaConsumer)
                        .map((MapFunction<ObjectNode, String>) node -> node.get("message").asText())
                        .returns(String.class);

        DataStream<DeviceMessage> sentDeviceMessages =
                sentKafkaMessages.flatMap((FlatMapFunction<String, DeviceMessage>) (s, out) -> {
                    Matcher matcher = SENT_PATTERN.matcher(s);
                    if (matcher.matches()) {
                        out.collect(new SentDeviceMessage(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(matcher.group(1)),
                                matcher.group(2), matcher.group(3)));
                    }
                }).returns(DeviceMessage.class);

        DataStream<DeviceMessage> receivedDeviceMessages =
                receivedKafkaMessages.flatMap((FlatMapFunction<String, DeviceMessage>) (s, out) -> {
                    Matcher matcher = RECEIVED_PATTERN.matcher(s);
                    if (matcher.matches()) {
                        out.collect(new ReceivedDeviceMessage(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(matcher.group(1)),
                                matcher.group(3), matcher.group(2)));
                    }
                }).returns(DeviceMessage.class);

        DataStream<IndexMessage> result =
                process(sentDeviceMessages.union(receivedDeviceMessages))
                        .filter((FilterFunction<IndexMessage>) indexMessage -> indexMessage.getDuration() > 0);

        Map<String, String> config = new HashMap<>();
        config.put("bulk.flush.max.actions", "1");
        config.put("cluster.name", "docker-cluster");

        List<InetSocketAddress> transports = new ArrayList<>();
        transports.add(new InetSocketAddress(InetAddress.getByName("elasticsearch"), 9300));

        result.addSink(
                new ElasticsearchSink<>(
                        config, transports,
                        (ElasticsearchSinkFunction<IndexMessage>) (message, context, indexer) -> {
                            IndexRequest request = Requests.indexRequest()
                                    .index("flink-index")
                                    .type("duration_type")
                                    .source("@timestamp", message.getTimestamp(), "value", message.getDuration());

                            indexer.add(request);
                        }
                )
        );

        env.execute("Flink Duration Job");
    }

    abstract protected DataStream<IndexMessage> process(DataStream<DeviceMessage> stream);
}
