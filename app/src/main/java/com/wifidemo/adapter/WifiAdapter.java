package com.wifidemo.adapter;

import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wifidemo.R;

import java.util.HashMap;
import java.util.List;

/**
 * Created by wyb on 2019-05-21.
 */

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.WifiHolder> {

    List<ScanResult> scanResults;
    OnItemClickListener onItemClickListener;
    RecyclerView recyclerView;
    String[] connectArray = new String[1];


    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public WifiAdapter(RecyclerView recyclerView, List<ScanResult> scanResults) {
        this.scanResults = scanResults;
        this.recyclerView = recyclerView;
    }

//    @NonNull
//    @Override
//    public WifiHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
//        return null;
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull WifiHolder wifiHolder, int i) {
//
//    }
//
//    @Override
//    public int getItemCount() {
//        return 0;
//    }
    @NonNull
    @Override
    public WifiHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.wifi_item, null);
        return new WifiHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiHolder wifiHolder, int i) {
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

    public void notifyWifiAdapter(int positon) {
        if (recyclerView != null && scanResults.size() > 0) {
            ImageView itemImg = recyclerView.getChildAt(positon).findViewById(R.id.wifi_rssi);
            int wifiSSSi = Math.abs(scanResults.get(positon).level);
            if (wifiSSSi >= 100) {
                itemImg.setImageResource(R.drawable.wifi_connect_low);
            } else if (wifiSSSi >= 70) {
                itemImg.setImageResource(R.drawable.wifi_connect_middle);
            } else {
                itemImg.setImageResource(R.drawable.wifi_connect_full);
            }
            //保存连接wifi的信息？？业务逻辑？自动重连？
            connectArray[0] = scanResults.get(positon).SSID;
            recyclerView.getAdapter().notifyItemChanged(positon);
        }
    }

    public void clearWifiConnected() {
        connectArray[0] = "";
    }

    public String getCipherType(int position) {
        if (position >= scanResults.size()) {
            return null;
        }
        String wifiPasswordType = scanResults.get(position).capabilities;
        if (TextUtils.isEmpty(wifiPasswordType)){
            return null;
        }
        if (wifiPasswordType.contains("WEP")||wifiPasswordType.contains("wep")){
            return "WEP";
        }else if (wifiPasswordType.contains("WPA")||wifiPasswordType.contains("wpa")){
            return "WPA";
        }else {
            return "OPEN";
        }
    }


    @Override
    public int getItemCount() {
        return scanResults == null ? 0 : scanResults.size();
    }

    public class WifiHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView wifiName; //wifi名称
        ImageView wifiSssi; //wifi信号强度

        /**
         * 这里得到信号强度就靠wifiinfo.getRssi()这个方法。
         * 得到的值是一个0到-100的区间值，是一个int型数据，
         * 其中0到-50表示信号最好，-50到-70表示信号偏差，小于-70表示最差，有可能连接不上或者掉线，一般Wifi已断则值为-200。
         **/

        public WifiHolder(@NonNull View itemView) {
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
