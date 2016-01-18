package com.zlove.provider;

import android.net.Uri;

import com.zlove.app.ApplicationUtil;

/**
 * Created by ZLOVE on 2015/3/3.
 */
public class DatabaseUris {
    public static String AUTHORITY = ApplicationUtil.getApplicationContext().getPackageName();

    public static final String CONTENT_P = "content://";

    public static final String PATH_RAW_SQL = "raw/sql";
    public static final Uri URI_RAW_SQL = Uri.parse(CONTENT_P + AUTHORITY + "/raw/sql");

    public static final String PATH_STUDENT_INFO = "student/info";
    public static final Uri URI_STUDENT_INFO = Uri.parse(CONTENT_P + AUTHORITY + "/student/info");
}
