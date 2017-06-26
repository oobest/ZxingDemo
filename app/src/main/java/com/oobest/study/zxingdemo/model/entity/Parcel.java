package com.oobest.study.zxingdemo.model.entity;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

/**
 * Created by Albert.Ou on 2017/6/5.
 */

public class Parcel {
    private String objectId;
    private String orderId;
    private String status;
    private String comCode;
    private DateTime updateAt;
    private DateTime createAt;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(DateTime updateAt) {
        this.updateAt = updateAt;
    }

    public DateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(DateTime createAt) {
        this.createAt = createAt;
    }

    public String getComCode() {
        return comCode;
    }

    public void setComCode(String comCode) {
        this.comCode = comCode;
    }
}
