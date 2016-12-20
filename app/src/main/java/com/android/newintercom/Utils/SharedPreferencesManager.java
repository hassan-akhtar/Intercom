package com.android.newintercom.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.HashMap;

public class SharedPreferencesManager {

    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor ;
    public static final String IS_DEVICE_NAME_SET = "isNameSet";
    public static final String MY_PREFERENCES = "MyPrefs" ;
    public static final String MY_IP = "own_ip";
    public static final String IS_DND = "isdnd";
    public static final String MY_NAME = "own_name";
    public static final String IS_ME = "isme";
    public static final String BROADCAST_IP = "b_ip";
   // public static final String IS_BROADCAST = "isbroadcast";
    public static final String PERMISSION_RECORD_AUDIO = "perRec";
    public static final String IS_RECEIVING_BROADCAST = "isReceiving";


    public SharedPreferencesManager(Context context) {
        sharedpreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();;
        editor = sharedpreferences.edit();
    }


    public  void setString(String key, String value){
        editor.putString(key, value);
        editor.commit();
    }

    public  String getString(String key ){
        return sharedpreferences.getString(key, "Not Found");
    }


    public  void setBoolean(String key, boolean value){
        editor.putBoolean(key,value);
        editor.commit();
    }

    public  boolean getBoolean(String key){
        return sharedpreferences.getBoolean(key, false);
    }

    public  void addDevices(HashMap<String, InetAddress> contacts){
        SharedPreferences.Editor prefsEditor = sharedpreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(contacts);
        prefsEditor.putString("devices", json);
        prefsEditor.commit();
    }

    public  void  removeDevices( HashMap<String, InetAddress> contacts ){
        Gson gson = new Gson();
        String json = sharedpreferences.getString("devices", null);
        Type type = new TypeToken<HashMap<String, InetAddress>>(){}.getType();
        HashMap<String, InetAddress> cantactList = gson.fromJson(json, type);
    //    cantactList.containsKey(contacts.get(contacts.keySet().))
    }

    public  HashMap<String, InetAddress> getDevices( ){
        Gson gson = new Gson();
        String json = sharedpreferences.getString("devices", null);
        Type type = new TypeToken<HashMap<String, InetAddress>>(){}.getType();
        HashMap<String, InetAddress> cantactList = gson.fromJson(json, type);
        return cantactList;
    }

}
