package com.oobest.study.zxingdemo.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.oobest.study.zxingdemo.model.entity.Parcel;

import java.util.List;

/**
 * Created by Albert.Ou on 2017/7/6.
 */

public class ParcelListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Parcel> data;

    public ParcelListAdapter(List<Parcel> data) {
        this.data = data;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }
}
