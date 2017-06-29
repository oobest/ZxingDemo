package com.oobest.study.zxingdemo.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.oobest.study.zxingdemo.ParcelActivity;
import com.oobest.study.zxingdemo.R;

import java.util.List;

/**
 * Created by Albert.Ou on 2017/6/27.
 */

public class ParcelAttrAdapter extends RecyclerView.Adapter<ParcelAttrAdapter.ViewHolder> {

    private List<ParcelActivity.ItemRow> itemRows;

    public ParcelAttrAdapter(List<ParcelActivity.ItemRow> itemRows) {
        this.itemRows = itemRows;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parcel_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ParcelActivity.ItemRow itemRow = itemRows.get(position);
        holder.labelView.setText(itemRow.label);
        holder.valueView.setText(itemRow.value);
    }

    @Override
    public int getItemCount() {
        return itemRows == null ? 0 : itemRows.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView labelView;

        public TextView valueView;

        public ViewHolder(View itemView) {
            super(itemView);
            labelView = (TextView) itemView.findViewById(R.id.labelView);
            valueView = (TextView) itemView.findViewById(R.id.valueView);
        }
    }
}
