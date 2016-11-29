package com.android.newintercom.Utils;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

    private static MyApplication singleton;
    public static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        setmContext(getApplicationContext());
    }

    public static MyApplication getInstance() {
        if (null == singleton) {
            singleton = new MyApplication();
        }
        return singleton;
    }

    public static Context getmContext() {
        return mContext;
    }

    public static void setmContext(Context mContext) {
        MyApplication.mContext = mContext;
    }
}
