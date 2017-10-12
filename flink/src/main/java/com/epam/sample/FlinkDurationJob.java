package com.epam.sample;

import com.epam.sample.events.DeviceMessage;
import com.epam.sample.events.IndexMessage;
import com.epam.sample.events.ReceivedDeviceMessage;
import com.epam.sample.events.SentDeviceMessage;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternFlatSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.time.Time;

public class FlinkDurationJob extends FlinkBaseJob {

    public static void main(String[] args) throws Exception {
        new FlinkDurationJob().execute();
    }

    @Override
    protected DataStream<IndexMessage> process(DataStream<DeviceMessage> stream) {
      // Put your code here...

      
    }
}
