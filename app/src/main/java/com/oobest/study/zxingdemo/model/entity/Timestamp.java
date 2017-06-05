package com.oobest.study.zxingdemo.model.entity;

import org.joda.time.DateTime;

/**
 * Created by Albert.Ou on 2017/6/5.
 */

public class Timestamp {
    private int timestamp;
    private DateTime datetime;

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public DateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(DateTime datetime) {
        this.datetime = datetime;
    }
}
