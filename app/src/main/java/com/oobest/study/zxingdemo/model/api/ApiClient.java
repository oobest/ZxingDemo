package com.oobest.study.zxingdemo.model.api;

import com.oobest.study.zxingdemo.BuildConfig;
import com.oobest.study.zxingdemo.model.util.EntityUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Albert.Ou on 2017/6/2.
 */

public class ApiClient {
    private ApiClient() {
    }

    public static final ApiService service = new Retrofit.Builder()
            .baseUrl(ApiDefine.DATA_API_BASE_URL)
            .client(new OkHttpClient.Builder()
                    .addInterceptor(createUserAgentInterceptor())
                    .addInterceptor(createHttpLoggingInterceptor())
                    .build())
            .addConverterFactory(GsonConverterFactory.create(EntityUtils.gson))
            .build()
            .create(ApiService.class);

    public static final ApiKdService kdService = new Retrofit.Builder()
            .baseUrl(ApiDefine.KD_API_URL)
            .client(new OkHttpClient.Builder()
                    .addInterceptor(createHttpLoggingInterceptor())
                    .build())
            .addConverterFactory(GsonConverterFactory.create(EntityUtils.gson))
            .build()
            .create(ApiKdService.class);

    private static Interceptor createUserAgentInterceptor() {
        return new Interceptor() {

            //private static final String HEADER_USER_AGENT = "User-Agent";

            @Override
            public Response intercept(Chain chain) throws IOException {
                return chain.proceed(chain.request().newBuilder()
                        .header("X-Bmob-Application-Id", "cf8c17ec899431b98d035d5cdf55b07b")
                        .header("X-Bmob-REST-API-Key", "9f5d0eeaa165b1b086692180997f4780")
                        .build());
            }

        };
    }

    private static Interceptor createHttpLoggingInterceptor() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        return loggingInterceptor;
    }
}
