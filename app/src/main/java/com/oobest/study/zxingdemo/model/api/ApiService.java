package com.oobest.study.zxingdemo.model.api;

import com.google.gson.JsonObject;
import com.oobest.study.zxingdemo.model.entity.KdResult;
import com.oobest.study.zxingdemo.model.entity.Parcel;
import com.oobest.study.zxingdemo.model.entity.Result;
import com.oobest.study.zxingdemo.model.entity.Results;
import com.oobest.study.zxingdemo.model.entity.Timestamp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/**
 * Created by Albert.Ou on 2017/6/2.
 */

public interface ApiService {
    @GET("1/classes/Parcel")
    Call<Results<List<Parcel>>> getParcelList(@Query("where") String where);

    @GET("1/classes/Parcel/{objectId}")
    Call<Parcel> getParcel(@Query("objectId") String objectId);

    @POST("1/classes/Parcel")
    @Headers("Content-Type:application/json")
    Call<Result> createParcel(@Body JsonObject jsonObject);

    @PUT("1/classes/Parcel/{objectId}")
    @Headers("Content-Type:application/json")
    Call<Result> updateParcel(@Body JsonObject jsonObject);

    @GET("1/timestamp")
    Call<Timestamp> getTimestamp();
}
