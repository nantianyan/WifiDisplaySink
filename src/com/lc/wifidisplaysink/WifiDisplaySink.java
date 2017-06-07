package com.lc.wifidisplaysink;

import android.content.Context;
import java.lang.ref.WeakReference;
import android.util.Log;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.Surface;

public class WifiDisplaySink{
    private final static String TAG = "WifiDisplaySinkJ";

    /* Do not change these values without updating their counterparts
     * in lib\WifiDisplaySink.h!
     */
    private static final int WFD_NOP = 0; // interface test message
    private static final int WFD_ERROR = 100;
    private static final int WFD_INFO = 200;

    public static final int WFD_ERROR_UNKNOWN = 1;
    public static final int WFD_ERROR_SERVER_DIED = 100;

    public static final int WFD_INFO_UNKNOWN = 1;
    public static final int WFD_INFO_RTSP_TEARDOWN = 700;

    private EventHandler mEventHandler;
    private SurfaceHolder mSurfaceHolder;

    public static WifiDisplaySink create(Context context) {
        WifiDisplaySink sink = new WifiDisplaySink();
        return sink;
    }

    public WifiDisplaySink() {
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            Log.d(TAG, "myLooper");
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            Log.d(TAG, "getMainLooper");
            mEventHandler = new EventHandler(this, looper);
        } else {
            Log.e(TAG, "no looper!!!!");
            mEventHandler = null;
        }

        native_setup(new WeakReference<WifiDisplaySink>(this), 0, 0); 
    }

    public void setDisplay(SurfaceHolder sh) {
        mSurfaceHolder = sh;
        Surface surface;
        if (sh != null) {
            surface = sh.getSurface();
        } else {
            surface = null;
        }
        Log.d(TAG, "setDisplay surface = "+surface);
        native_setVideoSurface(surface);
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setKeepScreenOn(true);
        }
    }

    public void invoke(String ip, int port) {
        if (mNativeContext == 0) {
            Log.e(TAG, "native context should have been configured.");
            return;
        }
        native_invokeSink(ip, port);   
        postEventFromNative(null, WFD_NOP, 0, 0, 0); // proguard
    }

    private static void postEventFromNative(Object sink_ref,
                                        int what, int arg1, int arg2, Object obj)
    {
        if (what == WFD_NOP) {
            Log.d("WifiDisplaySink", "test op");
            return;
        }

        WifiDisplaySink sink = (WifiDisplaySink)((WeakReference)sink_ref).get();
        if (sink == null) {
            Log.d("WifiDisplaySink", "sink is empty");
            return;
        }

        if (sink.mEventHandler != null) {
            Log.d("WifiDisplaySink", "postEventFromNative");
            Message m = sink.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            sink.mEventHandler.sendMessage(m);
        }

        return;
/*
        if (what == WFD_INFO && arg1 == WFD_INFO_RTSP_TEARDOWN) {
            Log.d("WifiDisplaySink", "postEventFromNative, WFD_INFO_RTSP_TEARDOWN");
            //native_release();
            System.exit(0); // FIXME
        }

        if (what == WFD_ERROR) {
            Log.d("WifiDisplaySink", "postEventFromNative, WFD_ERROR, we should notify to finish the activity?");
        }

        Log.d("WifiDisplaySink", "postEventFromNative, what:" + what);
        return;
*/
    }

    private static native final void native_init();
    private native final void native_setup(Object sink_this, int special, int isN10);
    private native final void native_invokeSink(String ip, int port);
    private native final void native_setVideoSurface(Surface surface);

    private long mNativeContext;
    private int mListenerContext;
    static {
        System.loadLibrary("WifiDisplaySink");
        native_init();
    }

    public interface OnErrorListener
    {
        /**
         * Called to indicate an error.
         *
         * @param sink    the WifiDisplaySink the error pertains to
         * @param what    the type of error that has occurred
         * @param extra an extra code, specific to the error. Typically
         * implementation dependent.
         * @return True if the method handled the error, false if it didn't.
         */
            boolean onError(WifiDisplaySink sink, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an error has happened
     *
     * @param listener the callback that will be run
     */
    public void setOnErrorListener(OnErrorListener listener)
    {
        mOnErrorListener = listener;
    }

    private OnErrorListener mOnErrorListener;

    private class EventHandler extends Handler
    {
        private WifiDisplaySink mWifiDisplaySink;

        public EventHandler(WifiDisplaySink sink, Looper looper) {
            super(looper);
            mWifiDisplaySink = sink;
        }

        @Override
        public void handleMessage(Message msg) {
            if (mWifiDisplaySink.mNativeContext == 0) {
                Log.w(TAG, "WifiDisplaySink went away with unhandled events");
                return;
            }
            switch(msg.what) {

            case WFD_ERROR:
                Log.e(TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                Log.d("WifiDisplaySink", "postEventFromNative, WFD_ERROR, we should notify to finish the activity?");
                if (mOnErrorListener != null) {
                    Log.d(TAG, "mOnErrorListener.onError");
                    mOnErrorListener.onError(mWifiDisplaySink, msg.arg1, msg.arg2);
                }
                return;

            case WFD_INFO:
                if (msg.arg1 == WFD_INFO_RTSP_TEARDOWN) {
                    Log.d("WifiDisplaySink", "postEventFromNative, WFD_INFO_RTSP_TEARDOWN");
                    //native_release();
                    System.exit(0); // FIXME
                }
                break;

            case WFD_NOP: // interface test message - ignore
                break;

            default:
                Log.e(TAG, "Unknown message type " + msg.what);
                return;
            }
        }
    }
}
