package com.oobest.study.zxingdemo;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.oobest.study.zxingdemo.model.api.ApiClient;
import com.oobest.study.zxingdemo.model.entity.CreateResult;
import com.oobest.study.zxingdemo.model.entity.GetResults;
import com.oobest.study.zxingdemo.model.entity.Parcel;
import com.oobest.study.zxingdemo.model.entity.UpdateResult;
import com.oobest.study.zxingdemo.model.util.EntityUtils;
import com.oobest.study.zxingdemo.ui.adapter.ParcelAttrAdapter;
import com.oobest.study.zxingdemo.util.ParcelCheckUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParcelActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ParcelActivity";

    private String orderId;

    private String objectId;

    private RecyclerView recyclerView;

    private List<ItemRow> itemRows;

    private Button handleButton;

    private Button cancelButton;

    private ParcelAttrAdapter parcelAttrAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        handleButton = (Button) findViewById(R.id.handleButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        itemRows = new ArrayList<>();
        parcelAttrAdapter = new ParcelAttrAdapter(itemRows);
        recyclerView.setAdapter(parcelAttrAdapter);


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
            itemRows.add(new ItemRow("单号：", query));
            check(query);
        }
        parcelAttrAdapter.notifyDataSetChanged();
        // parcelAttrAdapter.notifyItemChanged(0); 局部刷新
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
                        Snackbar.make(cancelButton, "网络连接失败", Snackbar.LENGTH_INDEFINITE)
                                .setAction("重试", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        check(query);
                                    }
                                }).show();
                        break;
                }
            }
        });
    }

    private void queryParcel(final String query) {
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
                    }else{
                        handleButton.setText("到货");
                        handleButton.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(Call<GetResults> call, Throwable t) {
                    Log.e(TAG, "onFailure: " + t.getLocalizedMessage(), t);
                    Snackbar.make(cancelButton, "网络连接失败", Snackbar.LENGTH_INDEFINITE)
                            .setAction("重试", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    check(query);
                                }
                            }).show();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "queryParcel: " + e.getLocalizedMessage(), e);
        }
    }

    private void populateData(Parcel parcel) {
        objectId = parcel.getObjectId();
        itemRows.add(new ItemRow("状态：", parcel.getStatus()));
        itemRows.add(new ItemRow("到货时间：", parcel.getCreatedAt().toString(EntityUtils.df)));
        if ("签收".equals(parcel.getStatus())) {
            itemRows.add(new ItemRow("签收时间：", parcel.getUpdatedAt().toString(EntityUtils.df)));
            handleButton.setVisibility(View.INVISIBLE);
        } else {
            handleButton.setText("签收");
            handleButton.setVisibility(View.VISIBLE);
        }
        parcelAttrAdapter.notifyDataSetChanged();
    }


    private void receivedParcel() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("orderId", orderId);
            jsonObject.put("status", "到货");
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
            ApiClient.service.createParcel(requestBody).enqueue(new Callback<CreateResult>() {
                @Override
                public void onResponse(Call<CreateResult> call, Response<CreateResult> response) {
                    CreateResult result = response.body();
                    if (result != null) {
                        objectId = result.getObjectId();
                        itemRows.add(new ItemRow("状态：", "到货"));
                        itemRows.add(new ItemRow("到货时间：", result.getCreatedAt().toString(EntityUtils.df)));
                        handleButton.setText("签收");
                        parcelAttrAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<CreateResult> call, Throwable t) {
                    Snackbar.make(cancelButton, "网络连接失败", Snackbar.LENGTH_INDEFINITE)
                            .setAction("重试", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    receivedParcel();
                                }
                            }).show();
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
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
            ApiClient.service.updateParcel(objectId, requestBody).enqueue(new Callback<UpdateResult>() {
                @Override
                public void onResponse(Call<UpdateResult> call, Response<UpdateResult> response) {
                    UpdateResult result = response.body();
                    if (result != null) {
                        handleButton.setVisibility(View.INVISIBLE);
                        itemRows.get(1).value = "签收";
                        itemRows.add(new ItemRow("签收时间：", result.getUpdatedAt().toString(EntityUtils.df)));
                        parcelAttrAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<UpdateResult> call, Throwable t) {
                    Log.e(TAG, "sign: ", t);
                    Snackbar.make(cancelButton, "网络连接失败", Snackbar.LENGTH_INDEFINITE)
                            .setAction("重试", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    sign();
                                }
                            }).show();
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
                if (!TextUtils.isEmpty(objectId)) {
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


    public static class ItemRow {
        public String label;
        public String value;

        public ItemRow(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}
