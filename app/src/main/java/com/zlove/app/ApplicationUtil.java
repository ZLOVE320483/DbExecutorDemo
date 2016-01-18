package com.zlove.app;

import android.content.ContentResolver;
import android.content.Context;

/**
 * Created by ZLOVE on 2015/3/1.
 */
public class ApplicationUtil {

    public static Context getApplicationContext() {
        return DatabaseApplication.getInstance();
    }

    public static ContentResolver getContentResolver() {
        return getApplicationContext().getContentResolver();
    }

}
