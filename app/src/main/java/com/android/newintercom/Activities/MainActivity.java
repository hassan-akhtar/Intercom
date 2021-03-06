package com.android.newintercom.Activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.newintercom.Adapters.DevicesAdapter;
import com.android.newintercom.Models.Devices;
import com.android.newintercom.R;
import com.android.newintercom.Services.AddDeviceService;
import com.android.newintercom.Services.DnDService;
import com.android.newintercom.Services.UDPBroadcastService;
import com.android.newintercom.Utils.BroadcastCall;
import com.android.newintercom.Utils.NetInfo;
import com.android.newintercom.Utils.SharedPreferencesManager;
import com.greysonparrelli.permiso.Permiso;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    LinearLayout llSetDeviceName, llMain, llDevices;
    EditText etDeviceName;
    Button btnStartApp;
    Button cbBroadcast;
    Switch sDnD;
    ListView lvDevices;
    SharedPreferencesManager sharedPreferencesManager;
    String TAG = "MainActivity:";
    TextView tvDeviceName, tvDeviceIp, tvNoTextFound, tvBroadcast, tvBroadcastReceiving;
    private ConnectivityManager connMgr;
    protected NetInfo net = null;
    protected String info_ip_str = "";
    static DevicesAdapter adapter;
    static List<Devices> devicesList = new ArrayList<>();
    RelativeLayout rlHeader;
    //private static final int BROADCAST_INTERVAL = 5000; // For resending the add device broadcast
    //private static final int BROADCAST_INTERVAL_REMOVE = 2000; // For resending the add device broadcast
    public static final int BROADCAST_PORT = 50009; // Socket on which packets are sent/received
    public static final int BROADCAST_PORT_REMOVE = 50010; // Socket on which packets are sent/received
    public static final int BROADCAST_PORT_DND = 50011; // Socket on which packets are sent/received
    private boolean BROADCAST = true;
    //InetAddress broadcastAddress;
    BroadcastReceiver addReceiver, updateDnDReceiver, broadcastReceiver;
    public static BroadcastCall broadcastCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initViews();
        initObj();
        initListeners();

        Permiso.getInstance().setActivity(MainActivity.this);
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {

                if (resultSet.isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
                    sharedPreferencesManager.setBoolean(SharedPreferencesManager.PERMISSION_RECORD_AUDIO, true);
                }

            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
            }
        }, Manifest.permission.RECORD_AUDIO);

        //getBroadcastIp();
        getCurrentIp();
        if (sharedPreferencesManager.getBoolean(SharedPreferencesManager.IS_DEVICE_NAME_SET)) {
            llSetDeviceName.setVisibility(View.GONE);
            tvDeviceName.setText(sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME));
            tvDeviceIp.setText(sharedPreferencesManager.getString(SharedPreferencesManager.MY_IP));
            llMain.setVisibility(View.VISIBLE);
        } else {
            llSetDeviceName.setVisibility(View.VISIBLE);
            llMain.setVisibility(View.GONE);
            rlHeader.setBackgroundColor(getResources().getColor(R.color.green));
        }


/*        if (sharedPreferencesManager.getBoolean(SharedPreferencesManager.IS_BROADCAST)) {
            cbBroadcast.setChecked(true);
            tvBroadcast.setVisibility(View.VISIBLE);
        } else {
            cbBroadcast.setChecked(false);
            tvBroadcast.setVisibility(View.GONE);
        }*/

        if (sharedPreferencesManager.getBoolean(SharedPreferencesManager.IS_DND)) {
            sDnD.setChecked(true);
            //sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_BROADCAST, false);
            cbBroadcast.setEnabled(false);
            llDevices.setVisibility(View.GONE);
            rlHeader.setBackgroundColor(getResources().getColor(R.color.red));
        } else {
            sDnD.setChecked(false);
            cbBroadcast.setEnabled(true);
            llDevices.setVisibility(View.VISIBLE);
            rlHeader.setBackgroundColor(getResources().getColor(R.color.green));
        }
        adapter = new DevicesAdapter(devicesList, MainActivity.this);
        // prepareListData();
        lvDevices.setAdapter(adapter);


        if (0 == devicesList.size()) {
            tvNoTextFound.setVisibility(View.VISIBLE);
        } else {
            tvNoTextFound.setVisibility(View.GONE);
        }


        if (sharedPreferencesManager.getBoolean(SharedPreferencesManager.IS_DEVICE_NAME_SET)) {
            startService(new Intent(MainActivity.this, AddDeviceService.class));
            //startService(new Intent(MainActivity.this, RemoveDeviceService.class));
            startService(new Intent(MainActivity.this, DnDService.class));
            startService(new Intent(MainActivity.this, UDPBroadcastService.class));
            //getBroadcastIp();

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    InetAddress broadcastAddress = null;
                    try {
                        broadcastAddress = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    broadcastName(sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME), broadcastAddress);
                }
            });

            thread.start();

        }

    }

    private void initViews() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        llSetDeviceName = (LinearLayout) findViewById(R.id.llSetDeviceName);
        rlHeader = (RelativeLayout) findViewById(R.id.rlHeader);
        llDevices = (LinearLayout) findViewById(R.id.llDevices);
        llMain = (LinearLayout) findViewById(R.id.llMain);
        etDeviceName = (EditText) findViewById(R.id.etDeviceName);
        btnStartApp = (Button) findViewById(R.id.btnStartApp);
        cbBroadcast = (Button) findViewById(R.id.cbBroadcast);
        sDnD = (Switch) findViewById(R.id.sDnD);
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        tvDeviceName = (TextView) findViewById(R.id.tvDeviceName);
        tvDeviceIp = (TextView) findViewById(R.id.tvDeviceIp);
        tvNoTextFound = (TextView) findViewById(R.id.tvNoTextFound);
        tvBroadcast = (TextView) findViewById(R.id.tvBroadcast);
        tvBroadcastReceiving = (TextView) findViewById(R.id.tvBroadcastReceiving);

    }

    private void initObj() {
        sharedPreferencesManager = new SharedPreferencesManager(MainActivity.this);
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        net = new NetInfo(MainActivity.this);
    }

    private void initListeners() {
        btnStartApp.setOnClickListener(mGlobal_OnClickListener);


        addReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "onReceive Add: ");
                String name = intent.getStringExtra(AddDeviceService.My_MSG);
                String ipAddress = intent.getStringExtra(AddDeviceService.My_IP);
                if (!sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME).equals(name)) {

                    boolean isadded = false;

                    for (Devices d : devicesList) {
                        if (d.getName().equals(name)) {
                            isadded = true;
                            break;
                        }
                    }

                    if (!isadded) {
                        Devices devices = new Devices(name, ipAddress, false);
                        devicesList.add(devices);
                        adapter.notifyDataSetChanged();
                        Log.e("addDevice", "Device Added");
                        tvNoTextFound.setVisibility(View.GONE);
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //getBroadcastIp();
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                InetAddress broadcastAddress = null;
                                try {
                                    broadcastAddress = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                }
                                broadcastName(sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME), broadcastAddress);
                            }
                        });
                        thread.start();

                    } else {
                        Log.i("addDevice", "Device Exists");
                    }
                } else {
                    Log.e(TAG, "Itssss Meeeeeeee ");
                }
            }
        };


        updateDnDReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String name = intent.getStringExtra(DnDService.NAME);
                String status = intent.getStringExtra(DnDService.STATUS);
                String ipAddress = intent.getStringExtra(DnDService.IP);

                if (!sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME).equals(name)) {
                    boolean value = false;
                    if ("true".equals(status)) {
                        value = true;
                    }
                    for (Devices d : devicesList) {
                        if (d.getName().equals(name)) {
                            devicesList.remove(d);
                            Devices devices = new Devices(name, ipAddress, value);
                            devicesList.add(devices);
                            adapter.notifyDataSetChanged();
                            Log.e("removeDevice", "Device Removed");
                            break;
                        }

                    }
                } else {
                    Log.e(TAG, "Itssss Meeeeeeee ");
                }
            }
        };


        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String ipAddress = intent.getStringExtra(UDPBroadcastService.IP);
                String msg = intent.getStringExtra(UDPBroadcastService.MSG);
                String name = "";
                String myIp = "/" + sharedPreferencesManager.getString(SharedPreferencesManager.MY_IP);

                if ("bbb".equals(msg)) {

                    if (!myIp.equals(ipAddress)) {

                        for (Devices d : devicesList) {
                            if (d.getIpAddress().equals(ipAddress)) {

                                name = d.getName();
                                break;
                            }

                        }

                        tvBroadcastReceiving.setVisibility(View.VISIBLE);
                        tvBroadcastReceiving.setText("Receiving broadcast from: " + name);
                        cbBroadcast.setEnabled(false);
                    } else {
                        Log.e(TAG, "Itssss Meeeeeeee ");
                    }
                } else {
                    if (!myIp.equals(ipAddress)) {
                        tvBroadcastReceiving.setVisibility(View.GONE);
                        cbBroadcast.setEnabled(true);
                    } else {
                        Log.e(TAG, "Itssss Meeeeeeee ");
                    }
                }
            }
        };


        sDnD.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    tvBroadcastReceiving.setVisibility(View.GONE);

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InetAddress broadcastAddress = null;
                            try {
                                broadcastAddress = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                            broadcastDnD("DND:", sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME), broadcastAddress);
                        }
                    });
                    thread.start();
                    sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_DND, true);
                   // sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_BROADCAST, false);
                   // cbBroadcast.setChecked(false);
                    cbBroadcast.setEnabled(false);
                    llDevices.setVisibility(View.GONE);
                    rlHeader.setBackgroundColor(getResources().getColor(R.color.red));
                } else {
                    if (sharedPreferencesManager.getBoolean(SharedPreferencesManager.IS_RECEIVING_BROADCAST)) {
                        tvBroadcastReceiving.setVisibility(View.VISIBLE);
                    }

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InetAddress broadcastAddress = null;
                            try {
                                broadcastAddress = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                            broadcastDnD("DDD:", sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME), broadcastAddress);
                        }
                    });
                    thread.start();
                    sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_DND, false);
                    cbBroadcast.setEnabled(true);
                    llDevices.setVisibility(View.VISIBLE);
                    rlHeader.setBackgroundColor(getResources().getColor(R.color.green));
                }

            }
        });


        cbBroadcast.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        tvBroadcast.setVisibility(View.VISIBLE);
                        sendBroadcastMessage("broadcast");

                        Thread thread = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    try {
                                        InetAddress broadcastAddress = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                                        broadcastCall = new BroadcastCall(MainActivity.this, broadcastAddress, sharedPreferencesManager.getString(SharedPreferencesManager.MY_IP));
                                        Thread.sleep(500);
                                        broadcastCall.startCall();
                                    } catch (UnknownHostException e) {
                                        e.printStackTrace();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        thread.start();
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        tvBroadcast.setVisibility(View.GONE);
                        sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_ME, false);
                        sendBroadcastMessage("endbroadcast");
                        broadcastCall.endCall();
                        return true; // if you want to handle the touch event
                }
                return false;
            }
        });
/*
        cbBroadcast.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // if (sharedPreferencesManager.getBoolean(SharedPreferencesManager.PERMISSION_RECORD_AUDIO)) {
                if (isChecked) {
                    tvBroadcast.setVisibility(View.VISIBLE);
                    sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_BROADCAST, true);
                    sendBroadcastMessage("broadcast");

                    Thread thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                try {
                                    InetAddress broadcastAddress = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                                    broadcastCall = new BroadcastCall(MainActivity.this, broadcastAddress, sharedPreferencesManager.getString(SharedPreferencesManager.MY_IP));
                                    Thread.sleep(500);
                                    broadcastCall.startCall();
                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    thread.start();


                } else {
                    tvBroadcast.setVisibility(View.GONE);
                    sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_BROADCAST, false);
                    sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_ME, false);
                    sendBroadcastMessage("endbroadcast");
                    broadcastCall.endCall();

                }
                *//*} else {
                    Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                        @Override
                        public void onPermissionResult(Permiso.ResultSet resultSet) {

                            if (resultSet.isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
                                sharedPreferencesManager.setBoolean(SharedPreferencesManager.PERMISSION_RECORD_AUDIO, true);
                            }

                        }

                        @Override
                        public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                        }
                    }, Manifest.permission.RECORD_AUDIO);*//*
                //}
            }
        });*/
    }

    private void sendBroadcastMessage(final String message) {
        // Creates a thread used for sending notifications
        Thread replyThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    byte[] data = message.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP)), 50555);
                    socket.send(packet);
                    Log.e(TAG, "Sent message( " + message + " ) to " + sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                    socket.disconnect();
                    socket.close();
                } catch (UnknownHostException e) {

                    Log.e(TAG, "Failure. UnknownHostException in sendMessage: " + sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                } catch (SocketException e) {

                    Log.e(TAG, "Failure. SocketException in sendMessage: " + e);
                } catch (IOException e) {

                    Log.e(TAG, "Failure. IOException in sendMessage: " + e);
                }
            }
        });
        replyThread.start();
    }

    final View.OnClickListener mGlobal_OnClickListener = new View.OnClickListener() {
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.btnStartApp: {

                    if ("".equals(etDeviceName.getText().toString().trim())) {
                        showToast("Please enter device name!");
                    } else {
                        sharedPreferencesManager.setString(SharedPreferencesManager.MY_NAME, etDeviceName.getText().toString().trim());
                        sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_DEVICE_NAME_SET, true);
                        tvDeviceName.setText(sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME));
                        getCurrentIp();
                        tvDeviceIp.setText(sharedPreferencesManager.getString(SharedPreferencesManager.MY_IP));

                        llSetDeviceName.setVisibility(View.GONE);
                        llMain.setVisibility(View.VISIBLE);
                        startService(new Intent(MainActivity.this, AddDeviceService.class));
                        //startService(new Intent(MainActivity.this, RemoveDeviceService.class));
                        startService(new Intent(MainActivity.this, DnDService.class));
                        startService(new Intent(MainActivity.this, UDPBroadcastService.class));
                        //getBroadcastIp();
                        getCurrentIp();
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                InetAddress broadcastAddress = null;
                                try {
                                    broadcastAddress = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                }
                                broadcastName(sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME), broadcastAddress);
                            }
                        });
                        thread.start();

                    }

                    break;
                }
            }
        }

    };

    void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

/*    private void prepareListData() {

        devicesList.clear();

        Devices devices = new Devices("Master Bedroom", false);
        devicesList.add(devices);

        devices = new Devices("Dad Bedroom", true);
        devicesList.add(devices);

        devices = new Devices("My Bedroom", false);
        devicesList.add(devices);

        adapter.notifyDataSetChanged();

    }*/


    void getCurrentIp() {
        final NetworkInfo ni = connMgr.getActiveNetworkInfo();
        if (ni != null) {
            //Log.i(TAG, "NetworkState="+ni.getDetailedState());
            if (ni.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                int type = ni.getType();
                //Log.i(TAG, "NetworkType="+type);
                if (type == ConnectivityManager.TYPE_WIFI) {
                    Log.e(TAG, "TYPE_WIFI detected!");// 3G// WIFI
                    net.getWifiInfo();
                    if (net.ssid != null) {
                        net.getIp();
                        //info_ip_str = getString(R.string.net_ip, net.ip, net.cidr, net.intf);
                        info_ip_str = getString(R.string.net_ip_simple, net.ip);
/*                        info_in_str = getString(R.string.net_ssid, net.ssid);
                        info_mo_str = getString(R.string.net_mode, getString(
                                R.string.net_mode_wifi, net.speed, WifiInfo.LINK_SPEED_UNITS));*/
                        Log.e(TAG, "IP: " + "" + info_ip_str);// 3G// WIFI
                        sharedPreferencesManager.setString(SharedPreferencesManager.MY_IP, info_ip_str);

                        String ipp = info_ip_str.replace(".","-");
                        String[] first = ipp.split("-");
                        String frst = first[0];
                        String scnd = first[1];
                        String thrd = first[2];
                        sharedPreferencesManager.setString(SharedPreferencesManager.BROADCAST_IP, frst + "." + scnd + "." + thrd + ".255");
                        Log.e(TAG, "getCurrentIp: "+frst + "." + scnd + "." + thrd + ".255" );
                    }
                } else if (type == 3 || type == 9) { // ETH
                    net.getIp();
                    //info_ip_str = getString(R.string.net_ip, net.ip, net.cidr, net.intf);
                    info_ip_str = getString(R.string.net_ip_simple, net.ip);
                    sharedPreferencesManager.setString(SharedPreferencesManager.MY_IP, info_ip_str);
                    String ipp = info_ip_str.replace(".","-");
                    String[] first = ipp.split("-");
                    String frst = first[0];
                    String scnd = first[1];
                    String thrd = first[2];
                    sharedPreferencesManager.setString(SharedPreferencesManager.BROADCAST_IP, frst + "." + scnd + "." + thrd + ".255");
                    //showToast("Ethernet connectivity detected!");
                    // showToast("IP: " + "" + info_ip_str);// 3G// WIFI
                    Log.e(TAG, "Ethernet connectivity detected!");
                    Log.e(TAG, "getCurrentIp: "+frst + "." + scnd + "." + thrd + ".255" );
                    Log.e(TAG, "IP: " + "" + info_ip_str);// 3G// WIFI
                } else {
                    Log.e(TAG, "Connectivity unknown!");
                }
            } else {
                Log.e(TAG, "Connectivity unknown!");
            }
        } else {
            Log.e(TAG, "Connectivity unknown!");
        }
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


    private String toBroadcastIp(int ip) {
        // Returns converts an IP address in int format to a formatted string
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                "255";
    }

    public static String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }


    @Override
    protected void onStop() {
        //broadcastRemoveName(sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME), broadcastAddress);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopService(new Intent(MainActivity.this, AddDeviceService.class));
                //stopService(new Intent(MainActivity.this, RemoveDeviceService.class));
                stopService(new Intent(MainActivity.this, DnDService.class));
            }
        }, 3000);

        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((addReceiver), new IntentFilter(AddDeviceService.My_RESULT));
        LocalBroadcastManager.getInstance(this).registerReceiver((updateDnDReceiver), new IntentFilter(DnDService.DND_RECEIVER));
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver), new IntentFilter(UDPBroadcastService.BROADCAST_RECEIVER));

    }

    @Override
    protected void onRestart() {
        startService(new Intent(MainActivity.this, AddDeviceService.class));
        //startService(new Intent(MainActivity.this, RemoveDeviceService.class));
        startService(new Intent(MainActivity.this, DnDService.class));
        getCurrentIp();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InetAddress broadcastAddress = null;
                try {
                    broadcastAddress = InetAddress.getByName(sharedPreferencesManager.getString(SharedPreferencesManager.BROADCAST_IP));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                broadcastName(sharedPreferencesManager.getString(SharedPreferencesManager.MY_NAME), broadcastAddress);
            }
        });
        thread.start();

        super.onRestart();
    }

/*    private InetAddress getBroadcastIp() {
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

    }*/

    public static void addDevice(Devices devices) {
        boolean isadded = false;

        for (Devices d : devicesList) {
            if (d.getName().equals(devices.getName())) {
                isadded = true;
                break;
            }
        }

        if (!isadded) {
            devicesList.add(devices);
            adapter.notifyDataSetChanged();
            Log.e("addDevice", "Device Added");
        } else {
            Log.i("addDevice", "Device Exists");
        }

    }

    public static void removeDevice(Devices devices) {

        for (Devices d : devicesList) {
            if (d.getName().equals(devices.getName())) {
                devicesList.remove(d);
                adapter.notifyDataSetChanged();
                Log.e("removeDevice", "Device Removed");
                break;
            }

        }


    }

    public static void updateDnDStatus(Devices devices) {

        for (Devices d : devicesList) {
            if (d.getName().equals(devices.getName())) {
                devicesList.remove(d);
                devicesList.add(devices);
                adapter.notifyDataSetChanged();
                Log.e("removeDevice", "Device Removed");
                break;
            }

        }


    }


    public void broadcastRemoveName(final String name, final InetAddress broadcastIP) {
        // Broadcasts the name of the device at a regular interval
        Log.e(TAG, "Broadcasting remove started!");
        Thread broadcastThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    String request = "BYE:" + name;
                    byte[] message = request.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT_REMOVE);
                    sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_ME, true);
                    socket.send(packet);
                    Log.e(TAG, "Broadcast remove  packet sent: " + packet.getAddress().toString());
                    Log.e(TAG, "Broadcaster remove  ending!");
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


    public void broadcastDnD(final String msg, final String name, final InetAddress broadcastIP) {
        // Broadcasts the name of the device at a regular interval
        Log.e(TAG, "Broadcasting remove started!");
        Thread broadcastThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    String request = msg + name;
                    byte[] message = request.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT_DND);
                    sharedPreferencesManager.setBoolean(SharedPreferencesManager.IS_ME, true);
                    socket.send(packet);
                    Log.e(TAG, "Broadcast remove  packet sent: " + packet.getAddress().toString());
                    Log.e(TAG, "Broadcaster remove  ending!");
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

}
