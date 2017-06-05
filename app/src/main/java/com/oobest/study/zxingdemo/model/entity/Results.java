package com.oobest.study.zxingdemo.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Albert.Ou on 2017/6/5.
 */

public class Results<T> {
    @SerializedName("results")
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
