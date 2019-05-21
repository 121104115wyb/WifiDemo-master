package com.wifidemo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.WorkSource;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.wifidemo.adapter.TestAdapter;
import com.wifidemo.adapter.WifiAdapter;
import com.wifidemo.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.List;

public class WifiConnectActivity extends AppCompatActivity {
    private static final String TAG = "WifiConnectActivity";
    WifiAdapter wifiAdapter;
    //    SwipeRefreshLayout wifi_refresh;
    RecyclerView wifi_recycle;
    Switch wifi_switch;
    List<ScanResult> scanResultList = new ArrayList<>();
    private WifiBroadcastReceiver wifiReceiver;
    private WifiManager mWifiManager;
    private int SCAN_WIFI_ONCE_TIME_START = 100;
    private int SCAN_WIFI_ONCE_TIME_END = SCAN_WIFI_ONCE_TIME_START + 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_list_layout);
        initViews();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //注册广播
        wifiReceiver = new WifiBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);//监听wifi是开关变化的状态
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);//监听wifiwifi连接状态广播
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);//监听wifi列表变化（开启一个热点或者关闭一个热点）
        registerReceiver(wifiReceiver, filter);
    }

    void initViews() {
//        wifi_refresh = findViewById(R.id.wifi_refresh);
//        wifi_refresh.setRefreshing(false);
//        wifi_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                if (mWifiManager==null){
//                    wifi_refresh.setRefreshing(false);
//                    Toast.makeText(WifiConnectActivity.this, "wifi错误！", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                if (mWifiManager.isWifiEnabled()){
//                    scanWifi.sendEmptyMessageDelayed(SCAN_WIFI_ONCE_TIME,2000);
//                }else {
//                    wifi_refresh.setRefreshing(false);
//                    Toast.makeText(WifiConnectActivity.this, "请先打开wifi开关", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
        wifi_recycle = findViewById(R.id.wifi_recycle);
        wifi_switch = findViewById(R.id.wifi_switch);
        //wifi_switch.setChecked(false);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        wifi_recycle.setLayoutManager(manager);
        wifiAdapter = new WifiAdapter(wifi_recycle, scanResultList);
        wifi_recycle.setAdapter(wifiAdapter);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiAdapter.setOnItemClickListener((v, p) -> {
            if (checkWifi(scanResultList.get(p).SSID)) {
                Toast.makeText(this, "您已经连接此WIFI", Toast.LENGTH_SHORT).show();
            } else {
                showWifiDialog(p);
            }
            //showDialog();
        });

        wifi_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sacnwifi();
                } else {
                    if (null != mWifiManager) {
                        mWifiManager.setWifiEnabled(false);
                    }
                    scanResultList.clear();
                    wifiAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    void sacnwifi() {
        //要定位权限才能搜索wifi
        PermissionUtil.requestEach(this, new PermissionUtil.OnPermissionListener() {
            @Override
            public void onSucceed() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //授权成功后打开wifi
                        if (null != mWifiManager) {
                            mWifiManager.setWifiEnabled(true);
                            //开始扫描要延迟几秒钟
                            mWifiManager.startScan();
                            scanWifi.sendEmptyMessage(SCAN_WIFI_ONCE_TIME_START);
                        }
                    }
                }).start();
            }

            @Override
            public void onFailed(boolean showAgain) {

            }
        }, PermissionUtil.LOCATION);

    }

    private Handler scanWifi = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    //显示进度条
                    //间隔2s后发送结束的信息
                    scanWifi.sendEmptyMessageDelayed(SCAN_WIFI_ONCE_TIME_END, 2000);
                    break;
                case 101:
                    //取消进度条
                    scanResultList.clear();
                    scanResultList.addAll(mWifiManager.getScanResults());
                    wifiAdapter.notifyDataSetChanged();
                    break;
            }
            return false;
        }
    });


    /**
     * 连接wifi
     *
     * @param targetSsid wifi的SSID
     * @param targetPsd  密码
     * @param enc        加密类型
     */
    @SuppressLint("WifiManagerLeak")
    public void connectWifi(String targetSsid, String targetPsd, String enc) {
        // 1、注意热点和密码均包含引号，此处需要需要转义引号
        String ssid = "\"" + targetSsid + "\"";
        String psd = "\"" + targetPsd + "\"";

        //2、配置wifi信息
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = ssid;
        switch (enc) {
            case "WEP":
                // 加密类型为WEP
                conf.wepKeys[0] = psd;
                conf.wepTxKeyIndex = 0;
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                break;
            case "WPA":
                // 加密类型为WPA
                conf.preSharedKey = psd;
                break;
            case "OPEN":
                //开放网络
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        //3、链接wifi
//        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiManager.addNetwork(conf);
        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();

        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals(ssid)) {
                mWifiManager.disconnect();
                Boolean isConnect = mWifiManager.enableNetwork(i.networkId, true);
                Log.d(TAG, "connectWifi: 是否连接成功： " + isConnect);
//                mWifiManager.reconnect();
                break;
            }
        }
    }

    //监听wifi状态广播接收器
    public class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {

                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (state) {
                    /**
                     * WIFI_STATE_DISABLED    WLAN已经关闭
                     * WIFI_STATE_DISABLING   WLAN正在关闭
                     * WIFI_STATE_ENABLED     WLAN已经打开
                     * WIFI_STATE_ENABLING    WLAN正在打开
                     * WIFI_STATE_UNKNOWN     未知
                     */
                    case WifiManager.WIFI_STATE_DISABLED: {
                        Log.i(TAG, "已经关闭");
                        break;
                    }
                    case WifiManager.WIFI_STATE_DISABLING: {
                        Log.i(TAG, "正在关闭");
                        break;
                    }
                    case WifiManager.WIFI_STATE_ENABLED: {
                        Log.i(TAG, "已经打开");
//                        sortScaResult();
                        break;
                    }
                    case WifiManager.WIFI_STATE_ENABLING: {
                        Log.i(TAG, "正在打开");
                        break;
                    }
                    case WifiManager.WIFI_STATE_UNKNOWN: {
                        Log.i(TAG, "未知状态");
                        break;
                    }
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.i(TAG, "--NetworkInfo--" + info.toString());
                if (NetworkInfo.DetailedState.DISCONNECTED == info.getDetailedState()) {//wifi没连接上
                    Log.i(TAG, "wifi没连接上");
                } else if (NetworkInfo.DetailedState.CONNECTED == info.getDetailedState()) {//wifi连接上了

                    Log.i(TAG, "wifi连接上了" + info.getExtraInfo());
                } else if (NetworkInfo.DetailedState.CONNECTING == info.getDetailedState()) {//正在连接
                    Log.i(TAG, "wifi正在连接");
                }
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                Log.i(TAG, "网络扫描完成,网络列表变化了");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanWifi.removeMessages(SCAN_WIFI_ONCE_TIME_END);
        scanWifi.removeMessages(SCAN_WIFI_ONCE_TIME_START);

    }


//    class ConnectAsyncTask extends AsyncTask<Void, Void, Boolean> {
//        private String ssid;
//        private String password;
//        private WifiAutoConnectManager.WifiCipherType type;
//        WifiConfiguration tempConfig;
//
//        public ConnectAsyncTask(String ssid, String password, WifiAutoConnectManager.WifiCipherType type) {
//            this.ssid = ssid;
//            this.password = password;
//            this.type = type;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            progressbar.setVisibility(View.VISIBLE);
//        }
//
//        @Override
//        protected Boolean doInBackground(Void... voids) {
//            // 打开wifi
//            mWifiAutoConnectManager.openWifi();
//            // 开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以要等到wifi
//            // 状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
//            while (mWifiAutoConnectManager.wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
//                try {
//                    // 为了避免程序一直while循环，让它睡个100毫秒检测……
//                    Thread.sleep(100);
//
//                } catch (InterruptedException ie) {
//                    Log.e("wifidemo", ie.toString());
//                }
//            }
//
//            tempConfig = mWifiAutoConnectManager.isExsits(ssid);
//            //禁掉所有wifi
//            for (WifiConfiguration c : mWifiAutoConnectManager.wifiManager.getConfiguredNetworks()) {
//                mWifiAutoConnectManager.wifiManager.disableNetwork(c.networkId);
//            }
//            if (tempConfig != null) {
//                Log.d("wifidemo", ssid + "配置过！");
//                boolean result = mWifiAutoConnectManager.wifiManager.enableNetwork(tempConfig.networkId, true);
//                if (!isLinked && type != WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS) {
//                    try {
//                        Thread.sleep(5000);//超过5s提示失败
//                        if (!isLinked) {
//                            Log.d("wifidemo", ssid + "连接失败！");
//                            mWifiAutoConnectManager.wifiManager.disableNetwork(tempConfig.networkId);
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    progressbar.setVisibility(View.GONE);
//                                    Toast.makeText(getApplicationContext(), "连接失败!请在系统里删除wifi连接，重新连接。", Toast.LENGTH_SHORT).show();
//                                    new AlertDialog.Builder(MainActivity.this)
//                                            .setTitle("连接失败！")
//                                            .setMessage("请在系统里删除wifi连接，重新连接。")
//                                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//                                                    dialog.dismiss();
//                                                }
//                                            })
//                                            .setPositiveButton("好的", new DialogInterface.OnClickListener() {
//                                                public void onClick(DialogInterface dialog, int which) {
//                                                    Intent intent = new Intent();
//                                                    intent.setAction("android.net.wifi.PICK_WIFI_NETWORK");
//                                                    startActivity(intent);
//                                                }
//                                            }).show();
//                                }
//                            });
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//                Log.d("wifidemo", "result=" + result);
//                return result;
//            } else {
//                Log.d("wifidemo", ssid + "没有配置过！");
//                if (type != WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            final EditText inputServer = new EditText(MainActivity.this);
//                            new AlertDialog.Builder(MainActivity.this)
//                                    .setTitle("请输入密码")
//                                    .setView(inputServer)
//                                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            dialog.dismiss();
//                                        }
//                                    })
//                                    .setPositiveButton("连接", new DialogInterface.OnClickListener() {
//
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            password = inputServer.getText().toString();
//                                            new Thread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    WifiConfiguration wifiConfig = mWifiAutoConnectManager.createWifiInfo(ssid, password,
//                                                            type);
//                                                    if (wifiConfig == null) {
//                                                        Log.d("wifidemo", "wifiConfig is null!");
//                                                        return;
//                                                    }
//                                                    Log.d("wifidemo", wifiConfig.SSID);
//
//                                                    int netID = mWifiAutoConnectManager.wifiManager.addNetwork(wifiConfig);
//                                                    boolean enabled = mWifiAutoConnectManager.wifiManager.enableNetwork(netID, true);
//                                                    Log.d("wifidemo", "enableNetwork status enable=" + enabled);
////                                                    Log.d("wifidemo", "enableNetwork connected=" + mWifiAutoConnectManager.wifiManager.reconnect());
////                                                    mWifiAutoConnectManager.wifiManager.reconnect();
//                                                }
//                                            }).start();
//                                        }
//                                    }).show();
//                        }
//                    });
//                } else {
//                    WifiConfiguration wifiConfig = mWifiAutoConnectManager.createWifiInfo(ssid, password, type);
//                    if (wifiConfig == null) {
//                        Log.d("wifidemo", "wifiConfig is null!");
//                        return false;
//                    }
//                    Log.d("wifidemo", wifiConfig.SSID);
//                    int netID = mWifiAutoConnectManager.wifiManager.addNetwork(wifiConfig);
//                    boolean enabled = mWifiAutoConnectManager.wifiManager.enableNetwork(netID, true);
//                    Log.d("wifidemo", "enableNetwork status enable=" + enabled);
////                    Log.d("wifidemo", "enableNetwork connected=" + mWifiAutoConnectManager.wifiManager.reconnect());
////                    return mWifiAutoConnectManager.wifiManager.reconnect();
//                    return enabled;
//                }
//                return false;
//
//
//            }
//        }
//
//        @Override
//        protected void onPostExecute(Boolean aBoolean) {
//            super.onPostExecute(aBoolean);
//            mConnectAsyncTask = null;
//        }
//    }

    private boolean checkWifi(String wifiName) {
        if (TextUtils.isEmpty(wifiName)) {
            return false;
        }
        try {
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            String infoExtra = null;
            if (connMgr != null) {
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo.isConnected()) {
                    //通过这个方法获取到的wifi的名称都会多出一个分号，要把它去掉
                    infoExtra = networkInfo.getExtraInfo();
                    infoExtra = infoExtra.substring(1, infoExtra.length() - 1);
                }
            }
            return wifiName.equalsIgnoreCase(infoExtra);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    Dialog dialog = null;

    private void showWifiDialog(int position) {
        Dialog dialog1 = new Dialog(this,R.style.dialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.wifi_connected_dialog, null);
        dialog1.setContentView(dialogView);
        EditText wifiPassword = dialogView.findViewById(R.id.wifi_password);
        ImageView notice = dialogView.findViewById(R.id.wifi_notice_img);
        TextView noticeText = dialogView.findViewById(R.id.wifi_notice_text);
        LinearLayout wifi_nitce_layout = dialogView.findViewById(R.id.wifi_nitce_layout);
        ImageView close = dialogView.findViewById(R.id.close_wifi_dialog);

        close.setOnClickListener(v -> {
            if (null != dialog1) {
                dialog1.cancel();
            }
        });

        ImageView connect = dialogView.findViewById(R.id.wifi_connect);
        connect.setOnClickListener(v -> {
            if (TextUtils.isEmpty(wifiPassword.getText().toString())) {
                //红色错误提示
//                notice.setImageResource();
                noticeText.setTextColor(Color.RED);
                noticeText.setText("请输入wifi密码");
                wifi_nitce_layout.setVisibility(View.VISIBLE);
                return;
            } else {
                ScanResult scanResult = scanResultList.get(position);
                connectWifi(scanResult.SSID, "ILOVEYOU", wifiAdapter.getCipherType(position));
            }
        });

        dialog1.show();

    }


}
