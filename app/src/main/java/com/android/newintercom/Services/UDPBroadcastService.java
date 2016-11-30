package com.android.newintercom.Services;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;


import com.android.newintercom.Utils.BroadcastCall;
import com.android.newintercom.Utils.SharedPreferencesManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class UDPBroadcastService extends Service {
    static String UDP_BROADCAST = "UDPBroadcast";
    static String LOG_TAG = "UDPBroadcast";
    SharedPreferencesManager sharedPreferencesManager;
    public static final int BROADCAST_PORT = 50555; // Socket on which packets are sent/received
    private static final int BROADCAST_BUF_SIZE = 1024;
    DatagramSocket socket;
    private static final int SAMPLE_RATE = 44100; // Hertz
    private boolean LISTEN = true;
    private BroadcastCall broadcastCall;
    Context mContext;


    @Override
    public void onCreate() {
        mContext = this;
        sharedPreferencesManager = new SharedPreferencesManager(this);
    }

    @Override
    public void onDestroy() {

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("UDP Broadcast", "Service started");
        startListener();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void startListener() {
        // Creates the listener thread
        LISTEN = true;
        final int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        Thread listenThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    Log.e(LOG_TAG, "Listener started for broadcast!");
                    DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
                    byte[] buffer = new byte[bufferSize];
                    DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
                    while (LISTEN) {

                        try {
                                Log.e(LOG_TAG, "Listening for packets");
                                socket.receive(packet);
                            if (!packet.getAddress().equals(sharedPreferencesManager.getString(SharedPreferencesManager.MY_IP))) {
                                String data = new String(buffer, 0, packet.getLength());
                                Log.e(LOG_TAG, "Packet received from " + packet.getAddress() + " with contents: " + data);
                                if ("broadcast".equals(data)) {
                                    broadcastCall = new BroadcastCall(mContext,InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP)),
                                            sharedPreferencesManager.getString(SharedPreferencesManager.MY_IP));
                                    broadcastCall.startCall();
                                } else if ("endbroadcast".equals(data)) {
                                    if (null!=broadcastCall) {
                                        broadcastCall.endCall();
                                    }
                                }
                            }else {
                               Log.e(LOG_TAG, "is meeeeeeee");
                               sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_ME, false);
                            }
                        } catch (IOException e) {

                            Log.e(LOG_TAG, "IOException in Listener " + e);
                        }
                    }
                    Log.e(LOG_TAG, "Listener ending");
                    socket.disconnect();
                    socket.close();
                    return;
                } catch (SocketException e) {

                    Log.e(LOG_TAG, "SocketException in Listener " + e);
                }
            }
        });
        listenThread.start();
    }

    private InetAddress getBroadcastIp() {
        // Function to return the broadcast address, based on the IP address of the device
        try {

            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            String addressString = toBroadcastIp(ipAddress);
            InetAddress broadcastAddress = InetAddress.getByName(addressString);
            return broadcastAddress;
        } catch (UnknownHostException e) {

            Log.e(LOG_TAG, "UnknownHostException in getBroadcastIP: " + e);
            return null;
        }

    }

    private String toBroadcastIp(int ip) {
        // Returns converts an IP address in int format to a formatted string
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                "255";
    }
}