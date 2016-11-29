package com.android.newintercom.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.newintercom.Activities.MainActivity;
import com.android.newintercom.Models.Devices;
import com.android.newintercom.Utils.SharedPreferencesManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DnDService extends Service {
    String TAG = "DnDService";
    public static final int BROADCAST_PORT = 50011; // Socket on which packets are sent/received
    static String UDP_BROADCAST = "AddDeviceService";
    SharedPreferencesManager sharedPreferencesManager;
    static final public String DND_RECEIVER = "com.android.newintercom.Services.DND_RECEIVER";
    static final public String NAME = "com.android.newintercom.Services.NAME";
    static final public String STATUS = "com.android.newintercom.Services.STATUS";

    DatagramSocket socket;

    private void listenAndWaitAndThrowIntent(InetAddress broadcastIP, Integer port) throws Exception {
        byte[] recvBuf = new byte[1500];
        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket(port, broadcastIP);
            socket.setBroadcast(true);
        }
        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);

        Log.e(TAG, "Waiting for UDP broadcast");
        socket.receive(packet);

       // if (!sharedPreferencesManager.getBoolean(SharedPreferencesManager.IS_ME)) {
            String data = new String(recvBuf, 0, packet.getLength());
            Log.i(TAG, "Packet received: " + data);
            String action = data.substring(0, 4);
            if (action.equals("DND:")) {
                Log.e(TAG, "Listener received ADD request");
              //  MainActivity.updateDnDStatus(new Devices(data.substring(4, data.length()), true));
                sendResult(data.substring(4, data.length()),"true");
            } else  if (action.equals("DDD:")) {
                Log.e(TAG, "Listener received ADD request");
                //MainActivity.updateDnDStatus(new Devices(data.substring(4, data.length()), false));
                sendResult(data.substring(4, data.length()),"false");
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
                    InetAddress broadcastIP = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
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


    public void sendResult(String name, String status){
        Intent in = new Intent(DND_RECEIVER);

        if(name!=null){
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            in.putExtra(NAME,name);
            in.putExtra(STATUS,status);
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}