package com.zlove.app;

import android.app.Application;
import android.util.Log;

import com.zlove.configure.DbFactory;

/**
 * Created by ZLOVE on 2015/3/1.
 */
public class DatabaseApplication extends Application {

    private static final String LOG_TAG = DatabaseApplication.class.getSimpleName();
    private static DatabaseApplication instance;

    private DbFactory factory;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initFactory();
    }

    public static DatabaseApplication getInstance() {
        if (instance == null) {
            Log.e(LOG_TAG,"Application is null");
        }
        return instance;
    }

    protected void initFactory() {
        factory = new DbFactory();
        DbFactory.initFactory(factory);
    }
}
