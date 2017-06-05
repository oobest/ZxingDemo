package com.oobest.study.zxingdemo;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.oobest.study.zxingdemo.model.api.ApiClient;
import com.oobest.study.zxingdemo.util.ParcelCheckUtils;

public class ParcelActivity extends AppCompatActivity {


    private static final String TAG = "ParcelActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }


    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "handleIntent: query=" + query);
            check(query);
        }
    }

    private void check(String query) {
        ParcelCheckUtils.isYt(query, new ParcelCheckUtils.Callback() {
            @Override
            public void onCallback(int result) {
                switch (result){
                    case ParcelCheckUtils.RESULT_IS_YT:
                        break;
                    case ParcelCheckUtils.RESULT_IS_NOT_YT:
                        break;
                    case ParcelCheckUtils.RESULT_FAILURE:
                        break;
                }
            }
        });
    }
}
