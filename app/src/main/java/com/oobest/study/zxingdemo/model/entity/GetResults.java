package com.oobest.study.zxingdemo.model.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Albert.Ou on 2017/6/5.
 */

public class GetResults {
    @SerializedName("results")
    private List<Parcel> results;

    public List<Parcel> getResults() {
        return results;
    }

    public void setResults(List<Parcel> results) {
        this.results = results;
    }
}
