package com.android.newintercom.Services;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.newintercom.Activities.MainActivity;
import com.android.newintercom.Models.Devices;
import com.android.newintercom.Utils.MyApplication;
import com.android.newintercom.Utils.SharedPreferencesManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AddDeviceService extends Service {
    String TAG = "AddDeviceService";
    public static final int BROADCAST_PORT = 50009; // Socket on which packets are sent/received
    static String UDP_BROADCAST = "AddDeviceService";
    SharedPreferencesManager sharedPreferencesManager;
    InetAddress broadcastAddress;
    DatagramSocket socket;
    static final public String My_RESULT = "com.android.newintercom.Services.PROCESSED";
    static final public String My_MSG = "com.android.newintercom.Services.MY_MSG";

    private void listenAndWaitAndThrowIntent(InetAddress broadcastIP, Integer port) throws Exception {
        byte[] recvBuf = new byte[1500];
        if (null  == socket  || socket.isClosed()   ) {
            socket = new DatagramSocket(port, broadcastIP);
            socket.setBroadcast(true);
        }
        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);

        Log.e(TAG, "Waiting for UDP broadcast");
        socket.receive(packet);

      //  if (!sharedPreferencesManager.getBoolean(SharedPreferencesManager.IS_ME)) {
            String data = new String(recvBuf, 0, packet.getLength());
            Log.i(TAG, "Packet received: " + data);
            String action = data.substring(0, 4);
            if (action.equals("ADD:")) {
                Log.e(TAG, "Listener received ADD request");
               // MainActivity.addDevice(new Devices(data.substring(4, data.length()), false));
                sendResult(data.substring(4, data.length()));
            } else {
                Log.w(TAG, "Listener received invalid request: " + action);
            }

            final String senderIP = packet.getAddress().getHostAddress();
            final String message = new String(packet.getData()).trim();

            Log.e(TAG, "Got UDB broadcast from " + senderIP + ", message: " + message);


/*        } else {
            Log.e(TAG, "Itssss Meeeeeeee ");
            sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_ME, false);
        }*/
        socket.disconnect();
        socket.close();
    }


    Thread UDPBroadcastThread;

    void startListenForUDPBroadcast() {
        UDPBroadcastThread = new Thread(new Runnable() {
            public void run() {
                try {
                    InetAddress broadcastIP = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP)); //172.16.238.42 //192.168.1.255
                    while (shouldRestartSocketListen) {
                        listenAndWaitAndThrowIntent(broadcastIP, BROADCAST_PORT);
                    }
                    //if (!shouldListenForUDPBroadcast) throw new ThreadDeath();
                } catch (Exception e) {
                    Log.e(TAG, "no longer listening for UDP broadcasts cause of error " + e.getMessage());
                }
            }
        });
        UDPBroadcastThread.start();
    }

    private Boolean shouldRestartSocketListen = true;

    void stopListen() {
        shouldRestartSocketListen = false;
        if (null != socket) {
            socket.close();
        }
    }

    public void sendResult(String message){
        Intent in = new Intent(My_RESULT);

        if(message!=null){
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            in.putExtra(My_MSG,message);
            broadcastManager.sendBroadcast(in);
        }

    }
    @Override
    public void onCreate() {

        sharedPreferencesManager = new SharedPreferencesManager(this);

    }

    @Override
    public void onDestroy() {
        stopListen();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldRestartSocketListen = true;
        startListenForUDPBroadcast();
        Log.e(TAG, "Service started");
        return START_STICKY;
    }


    public void broadcastName(final String name, final InetAddress broadcastIP) {
        // Broadcasts the name of the device at a regular interval
        Log.e(TAG, "Broadcasting started!");
        Thread broadcastThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    String request = "ADD:" + name;
                    byte[] message = request.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
                    sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_ME, true);
                    socket.send(packet);
                    Log.e(TAG, "Broadcast packet sent: " + packet.getAddress().toString());
                    Log.e(TAG, "Broadcaster ending!");
                    socket.disconnect();
                    socket.close();
                    return;
                } catch (SocketException e) {

                    Log.e(TAG, "SocketExceltion in broadcast: " + e);
                    Log.i(TAG, "Broadcaster ending!");
                    return;
                } catch (IOException e) {

                    Log.e(TAG, "IOException in broadcast: " + e);
                    Log.i(TAG, "Broadcaster ending!");
                    return;
                }
            }
        });
        broadcastThread.start();
    }

    private InetAddress getBroadcastIp() {
        // Function to return the broadcast address, based on the IP address of the device
        try {

            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            String addressString = toBroadcastIp(ipAddress);
            broadcastAddress = InetAddress.getByName(addressString);
            return broadcastAddress;
        } catch (UnknownHostException e) {

            Log.e(TAG, "UnknownHostException in getBroadcastIP: " + e);
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


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}