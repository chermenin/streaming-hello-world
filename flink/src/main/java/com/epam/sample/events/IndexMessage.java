package com.epam.sample.events;

import java.util.Date;

public class IndexMessage {
    private Date timestamp;
    private long duration;

    public IndexMessage(Date timestamp, long duration) {
        this.timestamp = timestamp;
        this.duration = duration;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public long getDuration() {
        return duration;
    }
}
