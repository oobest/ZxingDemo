package com.oobest.study.zxingdemo.model.entity;

import org.joda.time.DateTime;

/**
 * Created by Albert.Ou on 2017/6/5.
 */

public class Result {
    private String objectId;
    private DateTime createAt;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public DateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(DateTime createAt) {
        this.createAt = createAt;
    }
}
