package com.oobest.study.zxingdemo;

import android.app.Application;

import net.danlew.android.joda.JodaTimeAndroid;

/**
 * Created by Albert.Ou on 2017/5/19.
 */

public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
    }
}
