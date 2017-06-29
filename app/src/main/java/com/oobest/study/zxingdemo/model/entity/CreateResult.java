package com.oobest.study.zxingdemo.model.entity;

import org.joda.time.DateTime;

/**
 * Created by Albert.Ou on 2017/6/5.
 */

public class CreateResult {
    private String objectId;
    private DateTime createdAt;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

}
