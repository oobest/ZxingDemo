package com.oobest.study.zxingdemo;


import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.oobest.study.zxingdemo.model.api.ApiClient;
import com.oobest.study.zxingdemo.model.entity.GetResults;
import com.oobest.study.zxingdemo.model.entity.Parcel;
import com.oobest.study.zxingdemo.model.util.EntityUtils;
import com.oobest.study.zxingdemo.ui.adapter.ParcelListAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L;


/**
 * A simple {@link Fragment} subclass.
 */
public class ParcelListFragment extends Fragment implements RadioGroup.OnCheckedChangeListener, SwipeRefreshLayout.OnRefreshListener {


    public static final String TAG = "ParcelListFragment";

    public ParcelListFragment() {
        // Required empty public constructor
    }


    private RecyclerView parcelContent;
    private ParcelListAdapter parcelListAdapter;

    private int lastVisibleItem = 0;

    private LinearLayoutManager linearLayoutManager = null;

    private String status = "";

    // private String startTime = "";

    private int count = 0;

    private static final int LIMIT = 100;

    private List<Parcel> mData;

    private boolean isEnd = false;


    public static ParcelListFragment newInstance() {
        return new ParcelListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mData = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parcel_list, container, false);
        RadioGroup filterRadioGroup = (RadioGroup) view.findViewById(R.id.filterRadioGroup);
        filterRadioGroup.setOnCheckedChangeListener(this);

        SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this);

        parcelContent = (RecyclerView) view.findViewById(R.id.parcelContent);
        linearLayoutManager = new LinearLayoutManager(this.getContext());
        parcelContent.setLayoutManager(linearLayoutManager);
        parcelListAdapter = new ParcelListAdapter(mData);
        parcelContent.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItem + 1 == parcelListAdapter.getItemCount()) {
                    //// TODO: 2017/7/6
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (linearLayoutManager instanceof LinearLayoutManager) {
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                }
            }
        });
        loadData();
        return view;
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            case R.id.allParcels:
                status = "";
                break;
            case R.id.signedParcels:
                status = "签收";
                break;
            default:
                break;
        }
    }

    private void loadData() {
        if (isEnd) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            if (!TextUtils.isEmpty(status)) {
                jsonObject.put("status", status);
            }
//            if(!TextUtils.isEmpty(startTime)){
//                JSONObject json = new JSONObject();
//                json.put("__type","Date");
//                json.put("iso",startTime);
//                JSONObject valueJson = new JSONObject();
//                valueJson.put("$lt",json);
//                jsonObject.put("createdAt",valueJson);
//            }
            ApiClient.service.getParcelList(jsonObject.toString(), LIMIT, count, "-createdAt").enqueue(new Callback<GetResults>() {
                @Override
                public void onResponse(Call<GetResults> call, Response<GetResults> response) {
                    GetResults data = response.body();
                    if (data != null && data.getResults().size() > 0) {
                        mData.addAll(data.getResults());
                        count += data.getResults().size();
                        if (data.getResults().size() < LIMIT) {
                            isEnd = true;
                        }
                    } else {
                        isEnd = true;
                    }

                    if (isEnd) {
                        // // TODO: 2017/7/6 显示数据加载完成
                    }
                    parcelListAdapter.notifyItemRangeChanged();
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


    @Override
    public void onRefresh() {
        //// TODO: 2017/7/6  下拉刷新
        isEnd = false;
        count = 0;
    }
}
