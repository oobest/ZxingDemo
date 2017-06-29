package com.oobest.study.zxingdemo.model.entity;

import org.joda.time.DateTime;

/**
 * Created by Albert.Ou on 2017/6/26.
 */

public class UpdateResult {
    private DateTime updatedAt;

    public DateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(DateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
