package com.epam.sample.events;

import java.util.Date;

public class ReceivedDeviceMessage extends DeviceMessage {

    public ReceivedDeviceMessage(Date timestamp, String device, String ip) {
        super(timestamp, device, ip);
    }
}
