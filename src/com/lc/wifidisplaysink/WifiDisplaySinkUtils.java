package com.lc.wifidisplaysink;

import android.widget.Toast;
import android.content.Context;

public class WifiDisplaySinkUtils {

    public static void toastLog(Context ctx, String msg1, String msg2) {
        String log = msg1 + System.getProperty("line.separator") + msg2;
        Toast.makeText(ctx, log, Toast.LENGTH_SHORT).show();
    }

}
