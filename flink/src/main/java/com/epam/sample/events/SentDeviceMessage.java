package com.epam.sample.events;

import java.util.Date;

public class SentDeviceMessage extends DeviceMessage {

    public SentDeviceMessage(Date timestamp, String device, String ip) {
        super(timestamp, device, ip);
    }
}
