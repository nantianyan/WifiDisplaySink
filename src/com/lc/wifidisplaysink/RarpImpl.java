package com.lc.wifidisplaysink;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.util.Log;

/**
 * RARP: MAC --> IP
 * /proc/net/arp
 * IP address       HW type     Flags       HW address            Mask     Device
 * 192.168.49.208   0x1         0x2         a2:0b:ba:ba:c4:d1     *        p2p-wlan0-8
 */
public class RarpImpl {

    private String TAG = "RarpImpl";

    public final String ARP_PATH = "/proc/net/arp";

    /**
     * rarp 
     * @param netIf
     * @return IP address
     */
    public String execRarp(String netIf) {
        ArrayList<String> lines = readFile(ARP_PATH);
        if (lines == null || lines.size() == 0) {
            Log.w(TAG, "execRarp() readFile("+ARP_PATH+") returns 0");
            return null;
        }

        int i = 0;
        for (String line : lines) {
            Log.d(TAG, "execRarp() [" + (i++) + "]" + line);
        }

        ArrayList<ArpType> arps = parseArp(lines);
        if (arps == null || arps.size() == 0) {
            Log.w(TAG, "execRarp() parseArp("+lines+") returns 0");
            return null;
        }

        ArpType arp = searchArp(arps, netIf);
        if (arp == null) {
            Log.w(TAG, "execRarp() searchArp() "+netIf+" Not Found!");
            return null;
        }

        return arp.mIPaddress;
    }

    /**
     * Arp
     */
    public ArrayList<ArpType> getArpTable() {
        ArrayList<String> lines = readFile(ARP_PATH);
        if (lines == null || lines.size() == 0) {
            Log.w(TAG, "getArpTable() readFile("+ARP_PATH+") returns 0");
            return null;
        }

        int i = 0;
        for (String line : lines) {
            Log.d(TAG, "getArpTable() [" + (i++) + "]" + line);
        }

        ArrayList<ArpType> arps = parseArp(lines);
        if (arps == null || arps.size() == 0) {
            Log.w(TAG, "getArpTable() parseArp("+lines+") returns 0");
            return null;
        }
        return arps;
    }

    /**
     * 
     * @param path
     * @return null
     */
    private ArrayList<String> readFile(String path) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path)), "UTF-8"), 128);
            ArrayList<String> lines = new ArrayList<String>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();
            return lines;
        } catch(FileNotFoundException e) {
            Log.e(TAG, "readFile() " + e);
        } catch(UnsupportedEncodingException e) {
            Log.e(TAG, "readFile() " + e);
        } catch (IOException e) {
            Log.e(TAG, "readFile() " + e);
        }
        return null;
    }

    /**
     * /proc/net/arp
     */
    private ArrayList<ArpType> parseArp(ArrayList<String> arpLines) {
        if (arpLines == null || arpLines.size() == 1) {
            return null;
        }

        ArrayList<ArpType> arps = new ArrayList<ArpType>();
        for (String line : arpLines) {
            ArpType arp = parseArpLine(line);
            if (arp == null) {
                continue;
            }
            arps.add(arp);
        }
        return arps;
    }

    private ArpType parseArpLine(String line) {
        if (line == null) {
            Log.e(TAG, "parseArpLine() line is null!");
            return null;
        }

        String[] seps = line.split(" +"); //
        if (seps == null || seps.length == 0) {
            Log.e(TAG, "parseArpLine() split error!"+line+"]");
            return null;
        }

        int len = seps.length;
        // arp行
        if (len == 6) {
            ArpType arp = new ArpType(seps[0], seps[1], seps[2], seps[3], seps[4], seps[5]);
            Log.d(TAG, "parseArpLine() created arp["+arp.toString()+"]");
            return arp;
        } else {
            // ヘッダ
            if (seps.length == 9 && seps[0].equals("IP") && seps[8].equals("Device")) {
                Log.i(TAG, "parseArpLine() this is header line. don't create arp["+line+"]");
            } else {
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < seps.length; i++) {
                    buf.append(String.format("[%02d](%s)", i, seps[i]));
                }
                Log.e(TAG, "parseArpLine() Unknown Line! Seps["+buf.toString()+"]");
            }
            return null;
        }
    }

    private ArpType searchArp(ArrayList<ArpType> arps, String netIf) {
        if (arps == null || arps.size() == 0 || netIf == null) {
            return null;
        }

        ArpType lastarp = null;
        for (ArpType arp : arps) {
            //if (arp.mHWaddress.equals(macAddress)) {
            //    return arp;
            //}
            if (arp.mDevice.equals(netIf)) {
                if (!arp.mHWaddress.equals("00:00:00:00:00:00")) {
                    return arp;
                }
            }
            lastarp = arp;
        }

        if (lastarp != null) {
            //return lastarp;
        }
        return null;
    }


    public class ArpType {

        public String mIPaddress;
        String mHWType;
        String mFlags;
        String mHWaddress;
        String mMask;
        String mDevice;

        ArpType() {
        }

        ArpType(String ipaddress, String hwtype, String flags, String hwaddress, String mask, String device) {
            mIPaddress = ipaddress;
            mHWType = hwtype;
            mFlags = flags;
            mHWaddress = hwaddress;
            mMask = mask;
            mDevice = device;
        }

        public String toString() {
            String ret = 
                " IP address:" + mIPaddress + "¥n" +
                " HW type:" + mHWType + "¥n" +
                " Flags:" + mFlags + "¥n" +
                " HW address:" + mHWaddress + "¥n" +
                " Mask:" + mMask + "¥n" +
                " Device:" + mDevice;
            return ret;
        }
    }

}
