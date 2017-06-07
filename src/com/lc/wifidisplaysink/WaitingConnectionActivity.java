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
import com.lc.wifidisplaysink.HidDeviceAdapterService;

public class WaitingConnectionActivity extends Activity {
    private static final String TAG = "WaitingConnectionActivity";
    private BroadcastReceiver mReceiver;
    private WifiP2pManager mWifiP2pManager;
    private Channel mChannel;
    private List<WifiP2pDevice> mPeers = new ArrayList<WifiP2pDevice>();
    private final Handler mHandler = new Handler();

    private boolean mIsWiFiDirectEnabled;
    private boolean mConnected;
    private int mArpRetryCount = 0;
    private static final int MAX_ARP_RETRY_COUNT = 60;
    private int mP2pControlPort = -1;
    private String mP2pInterfaceName;
    private boolean mIsSinkOpened;

    private ImageView mConnectingImageView;
    private TextView mConnectingTextView;
    private AnimationDrawable mConnectingAnimation;

    private Context mContext;
    private WifiManager mWifiManager;

    private Runnable mConnectingChecker = new Runnable() {
        @Override
        public void run() {
            p2pDiscoverPeers(null);
            mHandler.postDelayed(mConnectingChecker, 1*1000*30); //TODO: to give a resonable check time.
        }
    };

    private Runnable mRarpChecker = new Runnable() {
        @Override
        public void run() {
            RarpImpl rarp = new RarpImpl();
            String sourceIp = rarp.execRarp(mP2pInterfaceName);

            if (sourceIp == null) {
                if (++mArpRetryCount > MAX_ARP_RETRY_COUNT) {
                    Log.e(TAG, "failed to do RARP, no source IP found. still trying ....");
                }
                mHandler.postDelayed(mRarpChecker, 1000);
            } else {
                if (!mIsSinkOpened) {
                    //delayedInvokeSink(sourceIp, mP2pControlPort, 1);
                    startWifiDisplaySinkActivity(sourceIp, mP2pControlPort);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_connection);

        runConnectingAnimation();
        utilsCheckP2pFeature();  // final WifiManager
        mContext = this;

        Intent startIntent = new Intent(this, HidDeviceAdapterService.class);
        startService(startIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mConnected = false;
        mIsSinkOpened = false;
        mIsWiFiDirectEnabled = false;

        registerBroadcastReceiver();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mHandler.removeCallbacksAndMessages(null);
        unRegisterBroadcastReceiver();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        System.exit(0);
    }

    private void registerBroadcastReceiver() {
        if (mReceiver == null) {
            Log.d(TAG, "registerBroadcastReceiver()");
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            mReceiver = new WiFiDirectBroadcastReceiver();
            registerReceiver(mReceiver, filter);
        }
    }

    private void unRegisterBroadcastReceiver() {
        if (mReceiver != null) {
            Log.d(TAG, "unRegisterBroadcastReceiver()");
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private void runConnectingAnimation() {
        Log.d(TAG, "runConnectingAnimation()");
        mConnectingTextView = (TextView) findViewById(R.id.connecting_textview);
        mConnectingImageView = (ImageView) findViewById(R.id.connecting_imageview);

        mConnectingAnimation = (AnimationDrawable) mConnectingImageView.getBackground();
        mConnectingImageView.post(new Runnable() {
            @Override
            public void run() {
                mConnectingAnimation.start();
            }
        });

        mConnectingTextView.setText(R.string.connecting_textview);
        mConnectingTextView.setTextColor(Color.parseColor("#ffffff00"));
    }

    private void utilsToastLog(String msg1, String msg2) {
        String log = msg1 + System.getProperty("line.separator") + msg2;
        Toast.makeText(this, log, Toast.LENGTH_SHORT).show();
    }

    private String utilsGetAndroidID() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private String utilsGetMACAddress() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        String mac = wifiInfo.getMacAddress();
        return mac;
    }

    private void utilsCheckP2pFeature() {
        Log.d(TAG, "utilsCheckP2pFeature()");
        Log.d(TAG, "ANDROID_ID: " + utilsGetAndroidID());

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (mWifiManager == null) {
            Log.e(TAG, "we'll exit because, mWifiManager is null");
            finish();
        }

        if (!mWifiManager.isWifiEnabled()) {
            if (!mWifiManager.setWifiEnabled(true)) {
                utilsToastLog("Warning", "Cannot enable wifi");
            }
        }

        Log.d(TAG, "MAC: " + utilsGetMACAddress());

        if (!p2pIsSupported()) {
            utilsToastLog("Warning", "This Package Does Not Supports P2P Feature!!");
            return;
        }
    }

    private boolean p2pIsSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
    }

    private boolean p2pIsNull() {
        if (!mIsWiFiDirectEnabled) {
            utilsToastLog(" Wi-Fi Direct is OFF!", "try Setting Menu");
            return true;
        }

        if (mWifiP2pManager == null) {
            utilsToastLog(" mWifiP2pManager is NULL!", " try getSystemService");
            return true;
        }
        if (mChannel == null) {
            utilsToastLog(" mChannel is NULL!", " try initialize");
            return true;
        }

        return false;
    }

    //NOTE: should call this before other p2p operation.
    public void p2pInitialize() {
        Log.d(TAG, "p2pInitialize()");

        if (mWifiP2pManager != null) {
            Log.d(TAG, "p2p manager have been initialized, please recheck the calling sequence.");
            return;
        }

        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), new ChannelListener() {
            public void onChannelDisconnected() {
                Log.d(TAG, "ChannelListener: onChannelDisconnected()");
            }
        });

        Log.d(TAG, "P2P Channel: "+ mChannel );

         mWifiP2pManager.setDeviceName(mChannel,
                 this.getString(R.string.p2p_device_name),
                 new WifiP2pManager.ActionListener() {
             public void onSuccess() {
                 Log.d(TAG, " device rename success");
             }
             public void onFailure(int reason) {
                 Log.d(TAG, " Failed to set device name");
             }
         });

        mWifiP2pManager.setMiracastMode(WifiP2pManager.MIRACAST_SINK);

        WifiP2pWfdInfo wfdInfo = new WifiP2pWfdInfo();
        wfdInfo.setWfdEnabled(true);
        wfdInfo.setDeviceType(WifiP2pWfdInfo.PRIMARY_SINK);
        wfdInfo.setSessionAvailable(true);
        wfdInfo.setControlPort(7236);
        wfdInfo.setMaxThroughput(50);

        mWifiP2pManager.setWFDInfo(mChannel, wfdInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully set WFD info.");
            }
            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to set WFD info with reason " + reason + ".");
            }
        });
    }


    public void p2pDiscoverPeers(View view) {
        Log.d(TAG, "p2pDiscoverPeers()");
        if (p2pIsNull()) {
            Log.w(TAG, "should call p2p initialize at first.");
            return;
        }

        mWifiP2pManager.discoverPeers(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully discoverPeers.");
            }
            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to discoverPeers with reason " + reason + ".");
            }
        });
    }

    public boolean p2pDeviceIsConnected(WifiP2pDevice device) {
        if (device == null) {
            return false;
        }
        return device.status == WifiP2pDevice.CONNECTED;
    }

    public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received Broadcast: "+action+"");

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                onStateChanged(context, intent);
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                onPeersChanged(context, intent);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                onConnectionChanged(context, intent);
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                onDeviceChanged(context, intent);
            }
        }

        private void onStateChanged(Context context, Intent intent) {
            Log.d(TAG, "***onStateChanged");

            mIsWiFiDirectEnabled = false;
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            switch (state) {
            case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                Log.d(TAG, "state: WIFI_P2P_STATE_ENABLED");
                mIsWiFiDirectEnabled = true;
                p2pInitialize();
                break;
            case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                Log.d(TAG, "state: WIFI_P2P_STATE_DISABLED");
                break;
            default:
                Log.d(TAG, "state: " + state);
                break;
            }
        }

        private void onPeersChanged(Context context, Intent intent) {
            Log.d(TAG, "***onPeersChanged");
            //intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST); or requestPeers anytime
        }

        private void onConnectionChanged(Context context, Intent intent)  {
            Log.d(TAG, "***onConnectionChanged");

            Log.d(TAG, "---------------------------------");
            WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            Log.d(TAG, "WifiP2pInfo:");
            Log.d(TAG, wifiP2pInfo.toString());

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.d(TAG, "NetworkInfo:");
            Log.d(TAG, networkInfo.toString());

            WifiP2pGroup wifiP2pGroupInfo = (WifiP2pGroup) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
            Log.d(TAG, "WifiP2pGroup:");
            Log.d(TAG, wifiP2pGroupInfo.toString());
            Log.d(TAG, "---------------------------------");

            if (!networkInfo.isConnected()) {
                if (mConnected) {
                    mConnected = false;
                    Log.d(TAG, "finish");
                    finish();
                    System.exit(0); // FIXME
                }
            }

            if (networkInfo.isConnected()) {
                if (!mConnected) {
                    mConnected = true;
                    if (mConnectingChecker != null) {
                        Log.d(TAG, "removeCallbacks --- mConnectingChecker");
                        mHandler.removeCallbacks(mConnectingChecker);
                        mConnectingChecker = null;
                    }
                    if (!mIsSinkOpened) {
                        startWfdSink(context, intent);
                    }
                }
            } 
        }

        private void onDeviceChanged(Context context, Intent intent) {
            Log.d(TAG, "***onDeviceChanged");

            WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "WifiP2pDevice:");
            Log.d(TAG, device.toString());

            if (mIsWiFiDirectEnabled && !p2pDeviceIsConnected(device)) {
                if (!mConnected) {
                    Log.d(TAG, "start connecting checker --- do p2pDiscoverPeers");
                    mHandler.postDelayed(mConnectingChecker, 100);
                }
            }
        }

    }

    private void startWfdSink(Context context, Intent intent) {
            WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            //NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            WifiP2pGroup wifiP2pGroupInfo = (WifiP2pGroup) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

            mP2pControlPort = 7236;
            Collection<WifiP2pDevice> p2pDevs = wifiP2pGroupInfo.getClientList();
            for (WifiP2pDevice dev : p2pDevs) {
                WifiP2pWfdInfo wfd = dev.wfdInfo;
                if (wfd != null && wfd.isWfdEnabled()) {
                    int type = wfd.getDeviceType();
                    if (type == WifiP2pWfdInfo.WFD_SOURCE || type == WifiP2pWfdInfo.SOURCE_OR_PRIMARY_SINK) {
                        mP2pControlPort = wfd.getControlPort();
                        Log.d(TAG, "got source port: " + mP2pControlPort);
                        break;
                    }
                } else {
                    continue;
                }
            }

            if (wifiP2pGroupInfo.isGroupOwner()) {
                Log.d(TAG, "GO gets password: " + wifiP2pGroupInfo.getPassphrase());

                //Log.d(TAG, "isGroupOwner, G.O. don't know client IP, so check /proc/net/arp ");
                // it's a pity that the MAC (dev.deviceAddress) is not the one in arp table
                mP2pInterfaceName = wifiP2pGroupInfo.getInterface();
                Log.d(TAG, "GO gets p2p interface: " + mP2pInterfaceName);
                mHandler.postDelayed(mRarpChecker, 1000);
            } else {
                Log.d(TAG, "Client couldn't get password");

                String sourceIp = wifiP2pInfo.groupOwnerAddress.getHostAddress();
                Log.d(TAG, "Client gets GO's IP: " + sourceIp);

                // delayedInvokeSink(sourceIp, mP2pControlPort, 1);
                startWifiDisplaySinkActivity(sourceIp, mP2pControlPort);

            }

    }

    private void delayedInvokeSink(final String ip, final int port, int delaySec) {
        Log.d(TAG, "delayedInvokeSink()");
        mIsSinkOpened = true;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                invokeSink(ip, port);
            }
        }, delaySec*1000);
    }

    private final void startWifiDisplaySinkActivity(String sourceAddr, int sourcePort) {
        Log.d(TAG, "source port: " + sourcePort + "   source ip addr:  " + sourceAddr);

        Intent itent = new Intent(this, WifiDisplaySinkActivity.class);
        itent.putExtra(WifiDisplaySinkConstants.SOURCE_PORT, sourcePort);
        itent.putExtra(WifiDisplaySinkConstants.SOURCE_ADDR, sourceAddr);

        utilsToastLog("source port: " + sourcePort, "source ip addr:  " + sourceAddr);

        startActivity(itent);
        finish();
    }

    private void invokeSink(String ip, int port) {
        Log.d(TAG, "invokeSink() Source Addr["+ip+":"+port+"]");
        new AvoidANRThread(ip, port).start();
        //Toast.makeText(this, "invokeSink() called native_invokeSink("+ip+":"+port+")", Toast.LENGTH_SHORT).show();
    }

    private class AvoidANRThread extends Thread {
        private final String ip;
        private final int port;
        private WifiDisplaySink mSink;

        AvoidANRThread(String _ip, int _port) {
            ip = _ip;
            port = _port;
            mSink = WifiDisplaySink.create(mContext);
        }

        public void run() {
            mSink.invoke(ip, port);
        }
    }
}
