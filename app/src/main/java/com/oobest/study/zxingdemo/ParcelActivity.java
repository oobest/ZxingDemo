package com.oobest.study.zxingdemo;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.oobest.study.zxingdemo.model.api.ApiClient;
import com.oobest.study.zxingdemo.model.entity.CreateResult;
import com.oobest.study.zxingdemo.model.entity.GetResults;
import com.oobest.study.zxingdemo.model.entity.Parcel;
import com.oobest.study.zxingdemo.model.entity.UpdateResult;
import com.oobest.study.zxingdemo.model.util.EntityUtils;
import com.oobest.study.zxingdemo.util.ParcelCheckUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParcelActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ParcelActivity";

    private String orderId;

    private String objectId;

    private ListView listView;

    private List<ItemRow> itemRows;

    private Button handleButton;

    private Button cancelButton;

    private boolean received = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel);
        listView = (ListView) findViewById(R.id.listView);
        handleButton = (Button) findViewById(R.id.handleButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        handleIntent(getIntent());
        handleButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
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
            itemRows.add(new ItemRow("单号", query));
            check(query);
        }
    }

    private void check(final String query) {
        ParcelCheckUtils.isYt(query, new ParcelCheckUtils.Callback() {
            @Override
            public void onCallback(int result) {
                switch (result) {
                    case ParcelCheckUtils.RESULT_IS_YT:
                        orderId = query;
                        queryParcel(query);
                        break;
                    case ParcelCheckUtils.RESULT_IS_NOT_YT:
                        handleButton.setVisibility(View.INVISIBLE);
                        break;
                    case ParcelCheckUtils.RESULT_FAILURE:
                        handleButton.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        });
    }

    private void queryParcel(String query) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("orderId", query);
            ApiClient.service.getParcelList(jsonObject.toString()).enqueue(new Callback<GetResults>() {
                @Override
                public void onResponse(Call<GetResults> call, Response<GetResults> response) {
                    GetResults data = response.body();
                    if (data != null && data.getResults().size() > 0) {
                        Log.d(TAG, "onResponse: " + EntityUtils.gson.toJson(data));
                        populateData(data.getResults().get(0));
                    }
                }

                @Override
                public void onFailure(Call<GetResults> call, Throwable t) {
                    Log.e(TAG, "onFailure: " + t.getLocalizedMessage(), t);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "queryParcel: " + e.getLocalizedMessage(), e);
        }
    }

    private void populateData(Parcel parcel) {
        itemRows.add(new ItemRow("状态", parcel.getStatus()));
        itemRows.add(new ItemRow("到货时间", parcel.getCreateAt().toString()));
        if ("签收".equals(parcel.getStatus())) {
            itemRows.add(new ItemRow("签收时间", parcel.getCreateAt().toString()));
            handleButton.setVisibility(View.INVISIBLE);
        } else {
            handleButton.setVisibility(View.VISIBLE);
        }
    }


    private void receivedParcel() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("orderId", orderId);
            jsonObject.put("status", "到货");

            ApiClient.service.createParcel(jsonObject).enqueue(new Callback<CreateResult>() {
                @Override
                public void onResponse(Call<CreateResult> call, Response<CreateResult> response) {
                    CreateResult result = response.body();
                    if (result != null) {
                        objectId = result.getObjectId();
                        itemRows.add(new ItemRow("状态", "到货"));
                        itemRows.add(new ItemRow("到货时间", result.getCreateAt().toString()));
                        handleButton.setText("签收");
                    }
                }

                @Override
                public void onFailure(Call<CreateResult> call, Throwable t) {

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sign() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", "签收");
            ApiClient.service.updateParcel(orderId, jsonObject).enqueue(new Callback<UpdateResult>() {
                @Override
                public void onResponse(Call<UpdateResult> call, Response<UpdateResult> response) {
                    UpdateResult result = response.body();
                    if (result != null) {
                        handleButton.setVisibility(View.INVISIBLE);
                        itemRows.add(new ItemRow("签收时间", result.getUpdateAt().toString()));
                    }
                }

                @Override
                public void onFailure(Call<UpdateResult> call, Throwable t) {
                    Log.e(TAG, "sign: ", t);
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "sign: ", e);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.handleButton:
                if (received) {
                    sign();
                } else {
                    receivedParcel();
                }
                break;
            case R.id.cancelButton:
                this.finish();
                break;
        }
    }


    static class ItemRow {
        String label;
        String value;

        public ItemRow(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}
