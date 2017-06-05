package com.oobest.study.zxingdemo.model.api;

import android.util.Log;

import com.oobest.study.zxingdemo.model.entity.Parcel;
import com.oobest.study.zxingdemo.model.entity.Results;

import org.junit.Test;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

/**
 * Created by Albert.Ou on 2017/6/5.
 */
public class ApiServiceTest {
    private static final String TAG = "ApiServiceTest";
    @Test
    public void getParcelList() throws Exception {
        ApiClient.service.getParcelList("").enqueue(new Callback<Results<List<Parcel>>>() {
            @Override
            public void onResponse(Call<Results<List<Parcel>>> call, Response<Results<List<Parcel>>> response) {
                Log.d(TAG, "onResponse: ");
            }

            @Override
            public void onFailure(Call<Results<List<Parcel>>> call, Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    @Test
    public void getParcel() throws Exception {

    }

    @Test
    public void createParcel() throws Exception {

    }

}