package com.wifidemo.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wifidemo.R;

import java.util.List;

/**
 * Created by wyb on 2019-05-21.
 */

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.MyViewHolder> {
    List<ScanResult> scanResults;
    WifiAdapter.OnItemClickListener onItemClickListener;
    RecyclerView recyclerView;
    String[] connectArray = new String[1];
    Context context;

    public void setOnItemClickListener(WifiAdapter.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public TestAdapter(Context context, RecyclerView recyclerView, List<ScanResult> scanResults) {
        this.scanResults = scanResults;
        this.recyclerView = recyclerView;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.wifi_item, viewGroup, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder wifiHolder, int i) {
        if (scanResults.size() > 0) {
            wifiHolder.wifiName.setText(scanResults.get(i).SSID);
            int wifiSSSi = Math.abs(scanResults.get(i).level);
            if (wifiSSSi >= 100) {
                wifiHolder.wifiSssi.setImageResource(R.drawable.wifi_not_connected_low);
            } else if (wifiSSSi >= 70) {
                wifiHolder.wifiSssi.setImageResource(R.drawable.wifi_not_connected_middle);
            } else {
                wifiHolder.wifiSssi.setImageResource(R.drawable.wifi_not_connected_full);
            }
        }
    }

    @Override
    public int getItemCount() {
        return scanResults.size();
    }


     class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView wifiName; //wifi名称
        ImageView wifiSssi; //wifi信号强度

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            wifiName = itemView.findViewById(R.id.tv_name);
            wifiSssi = itemView.findViewById(R.id.wifi_rssi);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, getAdapterPosition());
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

    }

}
