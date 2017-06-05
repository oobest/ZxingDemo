package com.oobest.study.zxingdemo.model.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Albert.Ou on 2017/6/5.
 */

public class KdResult {
    private String comCode;
    private String num;
    @SerializedName("auto")
    private List<KdInfo> kdInfoList;

    public String getComCode() {
        return comCode;
    }

    public void setComCode(String comCode) {
        this.comCode = comCode;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public List<KdInfo> getKdInfoList() {
        return kdInfoList;
    }

    public void setKdInfoList(List<KdInfo> kdInfoList) {
        this.kdInfoList = kdInfoList;
    }
}
