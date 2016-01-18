package com.zlove.app;

import android.app.Application;

import com.zlove.util.LogUtil;

/**
 * Created by ZLOVE on 2015/3/27.
 */
public class CommonFrameApplication extends Application {

    private static final String LOG_TAG = CommonFrameApplication.class.getSimpleName();
    private static CommonFrameApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static CommonFrameApplication getInstance() {
        if (instance == null) {
            LogUtil.e(LOG_TAG, "Application is null");
        }
        return instance;
    }
}
