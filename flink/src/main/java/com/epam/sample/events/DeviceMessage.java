package com.epam.sample.events;

import java.util.Date;

public class DeviceMessage {
    private Date timestamp;
    private String device;
    private String ip;

    public DeviceMessage(Date timestamp, String device, String ip) {
        this.timestamp = timestamp;
        this.device = device;
        this.ip = ip;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getDevice() {
        return device;
    }

    public String getIp() {
        return ip;
    }
}
