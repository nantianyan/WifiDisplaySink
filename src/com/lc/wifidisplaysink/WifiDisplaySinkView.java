package com.lc.wifidisplaysink;

import java.lang.Math;
import android.view.*;
//import android.view.SurfaceView;
//import android.view.SurfaceHolder;
import android.content.Context;
import android.util.Log;
import android.util.AttributeSet;

import com.lc.wifidisplaysink.WifiDisplaySink;
import com.lc.wifidisplaysink.WifiDisplaySink.OnErrorListener;
import com.lc.wifidisplaysink.WifiDisplaySinkUtils;
import com.lc.wifidisplaysink.HidDeviceAdapterService;


public class WifiDisplaySinkView extends SurfaceView {
    private static final String TAG = "WifiDisplaySinkView";

    private String mSourceAddr = null;
    private int mSourcePort = 7236;
    private SurfaceHolder mSurfaceHolder = null;
    private Context mContext = null;
    private WifiDisplaySink mSink;
    private WifiDisplaySink.OnErrorListener mOnErrorListener;
    private HidDeviceAdapterService mHidDeviceAdapterService;

    private int         mSurfaceWidth;
    private int         mSurfaceHeight;

    private void initView() {
        getHolder().addCallback(mSHCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        Log.d(TAG, "WifiDisplaySink.create");
        mSink = WifiDisplaySink.create(mContext);

    }

    public WifiDisplaySinkView(Context context) {
        super(context);
        mContext = context;
        initView();
    }

    public WifiDisplaySinkView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        mContext = context;
        initView();
    }

    public WifiDisplaySinkView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initView();
    }

    public void setSourceIpAddr(String sourceAddr, int sourcePort) {
        mSourceAddr = sourceAddr;
        mSourcePort = sourcePort;
        Log.d(TAG, "setSourceIpAddr");
    }

    public void setOnErrorListener(OnErrorListener l)
    {
        mOnErrorListener = l;
    }

    public void setHidDeviceAdapterService(HidDeviceAdapterService service) {
        Log.d(TAG, "setHidDeviceAdapterService");
        mHidDeviceAdapterService = service;
    }

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback()
    {
        public void surfaceChanged(SurfaceHolder holder, int format,
                                    int w, int h)
        {
            Log.d(TAG, "surfaceChanged: size [" + w + " x " + h +"]");
            mSurfaceWidth = w;
            mSurfaceHeight = h;
        }

        public void surfaceCreated(SurfaceHolder holder)
        {
            Log.d(TAG, "surfaceCreated");
            mSurfaceHolder = holder;
            mSink.setDisplay(mSurfaceHolder);
            Log.d(TAG, "WifiDisplaySink.invoke");
            mSink.invoke(mSourceAddr, mSourcePort);
        }

        public void surfaceDestroyed(SurfaceHolder holder)
        {
            Log.d(TAG, "surfaceDestroyed");
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            //release(true);
        }
    };


    private int mLastX= 0xffff, mLastY= 0xffff, mCurrentX= 0xffff, mCurrentY = 0xffff;
    private int mDownX= 0xffff, mDownY= 0xffff, mUpX= 0xffff, mUpY = 0xffff;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
         switch (event.getAction()) {
             case MotionEvent.ACTION_DOWN:
                 Log.d(TAG,"DOWN getX()="+event.getX()+"getY()"+event.getY());
                 mLastX = (int)event.getX();
                 mLastY = (int)event.getY();
                 mDownX = (int)event.getX();
                 mDownY = (int)event.getY();
                 //return false;
                 return true;
             case MotionEvent.ACTION_UP:
                 Log.d(TAG,"UP getX()="+event.getX()+"getY()"+event.getY());

                 mUpX = (int)event.getX();
                 mUpY = (int)event.getY();
                 if(Math.abs(mUpX - mDownX) < 15 && Math.abs(mUpY - mDownY) < 15) {
                     Log.d(TAG, "send click");
                     if (mHidDeviceAdapterService != null) {
                        mHidDeviceAdapterService.sendLeftClickReport();
                     }
                 }
                 //return false;
                 return true;
             case MotionEvent.ACTION_MOVE:
                 Log.d(TAG,"MOVE getX()="+event.getX()+"getY()"+event.getY());
                 mCurrentX = (int)event.getX();
                 mCurrentY = (int)event.getY();

                 int dx= mCurrentX-mLastX;
                 int dy= mCurrentY-mLastY;
                 byte Bdx = (byte)dx;
                 byte Bdy = (byte)dy;

                 //Log.d(TAG, "Mouse after" + "Bdx="+ Bdx + " Bdy=" + Bdy);
                 Log.d(TAG, "send move");
                 if (mHidDeviceAdapterService != null) {
                    mHidDeviceAdapterService.sendMouseMoveReport(Bdx, Bdy);
                 }
                 mLastX=mCurrentX;
                 mLastY=mCurrentY;

                 //return false;
                 return true;
             default:
                 break;
         }
         return false;
    }
}
