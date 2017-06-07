package com.lc.wifidisplaysink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.Color;
import java.lang.ref.WeakReference;
import android.os.Message;
import android.os.Looper;
import com.lc.wifidisplaysink.WifiDisplaySink;
import com.lc.wifidisplaysink.WifiDisplaySinkConstants;
import com.lc.wifidisplaysink.WifiDisplaySinkUtils;
import com.lc.wifidisplaysink.HidDeviceAdapterService;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;

public class WifiDisplaySinkActivity extends Activity {
    private final String TAG = "WifiDisplaySinkActivity";

    private String mSourceAddr;
    private int mSourcePort;
    private WifiDisplaySink mSink;

    private WifiDisplaySinkView mWifiDisplaySinkView;

    HidDeviceAdapterService mHidDeviceAdapterService;

    private ServiceConnection mConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            if (service != null) {
                mHidDeviceAdapterService = ((HidDeviceAdapterService.HidDeviceAdapterBinder) service).getService();
                if (mHidDeviceAdapterService != null)
                    mWifiDisplaySinkView.setHidDeviceAdapterService(mHidDeviceAdapterService);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mHidDeviceAdapterService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifidisplay_sink);

        View rootView = findViewById(R.id.movie_view_root); 

        mWifiDisplaySinkView = (WifiDisplaySinkView) rootView.findViewById(R.id.surface_view);


        if (mWifiDisplaySinkView == null) {
            Log.e(TAG, "mWifiDisplaySinkView is null");
            WifiDisplaySinkUtils.toastLog(this, TAG, "Error: mWifiDisplaySinkView is null");
        }


        // TODO:
        mSourceAddr = "192.168.49.46";
        mSourcePort = 7236;

        mSourcePort = getIntent().getIntExtra(WifiDisplaySinkConstants.SOURCE_PORT, 7236);
        mSourceAddr = getIntent().getStringExtra(WifiDisplaySinkConstants.SOURCE_ADDR);
        Log.d(TAG, "addr:" + mSourceAddr + " | port:" + mSourcePort);

        mWifiDisplaySinkView.setSourceIpAddr(mSourceAddr, mSourcePort);

        Intent intent = new Intent(this, HidDeviceAdapterService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // System.exit(0);  // ?
    }

    @Override
    protected void onDestroy() {
        unbindService(mConn);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        unbindService(mConn);
        finish();
        System.exit(0);
    }

}
