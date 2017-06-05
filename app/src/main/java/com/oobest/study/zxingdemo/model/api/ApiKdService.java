package com.oobest.study.zxingdemo.model.api;

import com.oobest.study.zxingdemo.model.entity.KdResult;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Albert.Ou on 2017/6/5.
 */

public interface ApiKdService {
    @GET("autonumber/autoComNum")
    Call<KdResult> getKdAutoNum(@Query("text") String text);
}
