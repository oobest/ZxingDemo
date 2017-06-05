package com.oobest.study.zxingdemo.util;

import android.support.annotation.NonNull;

import com.oobest.study.zxingdemo.model.api.ApiClient;
import com.oobest.study.zxingdemo.model.entity.KdResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Albert.Ou on 2017/6/2.
 */

public class ParcelCheckUtils {

    public static final int RESULT_FAILURE = -1;

    public static final int RESULT_IS_NOT_YT = 0;

    public static final int RESULT_IS_YT = 1;


    public interface Callback {
        void onCallback(int result);
    }

    public static void isYt(String text, @NonNull final Callback checkCallback) {
        ApiClient.kdService.getKdAutoNum(text).enqueue(new retrofit2.Callback<KdResult>() {
            @Override
            public void onResponse(Call<KdResult> call, Response<KdResult> response) {
                KdResult data = response.body();
                int result = RESULT_IS_NOT_YT;
                if (data != null && data.getKdInfoList().size() > 0 && ("yuantong".equals(data.getKdInfoList().get(0).getComCode()))) {
                    result = RESULT_IS_YT;
                }
                checkCallback.onCallback(result);
            }

            @Override
            public void onFailure(Call<KdResult> call, Throwable t) {
                checkCallback.onCallback(RESULT_FAILURE);
            }
        });
    }
}
