package com.zlove.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import com.zlove.db.DatabaseConstants;
import com.zlove.db.DatabaseManager;

/**
 * Created by ZLOVE on 2015/3/3.
 */
public class DatabaseProvider extends ContentProvider {

    private static String AUTHORITY = null;
    protected final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName();
        initMatcher();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (DatabaseUris.URI_RAW_SQL.equals(uri)) {
            Cursor cursor = null;
            try {
                // A Kind Of Hacker
                cursor = DatabaseManager.getInstance().doQueryAction(selection, selectionArgs);
                if (cursor != null && sortOrder != null) {
                    cursor.setNotificationUri(getContext().getContentResolver(), Uri.parse(sortOrder));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return cursor;
        } else {
            Cursor cursor = null;
            String tableName = getTableNameFromUri(uri);
            if (!TextUtils.isEmpty(tableName)) {
                SQLiteDatabase db = DatabaseManager.getInstance().getReadableDatabase();
                if (db != null) {
                    cursor = db.query(tableName,projection,selection,selectionArgs,null,null,sortOrder);
                    cursor.setNotificationUri(getContext().getContentResolver(),uri);
                }
            }
            return cursor;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String tableName = getTableNameFromUri(uri);
        if (!TextUtils.isEmpty(tableName)) {
            SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
            if (db != null) {
                long insertId = db.insert(tableName,null,values);
                if (insertId == -1) {
                    db.replace(tableName,null,values);
                }
            }
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String tableName = getTableNameFromUri(uri);
        if (!TextUtils.isEmpty(tableName)) {
            SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
            if (db != null) {
                return db.delete(tableName,selection,selectionArgs);
            }
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String tableName = getTableNameFromUri(uri);
        if(!TextUtils.isEmpty(tableName)) {
            SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
            if (db != null) {
                db.update(tableName,values,selection,selectionArgs);
            }
        }
        return 0;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        String tableName = getTableNameFromUri(uri);
        if (!TextUtils.isEmpty(tableName)) {
            SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
            if (db != null) {
                try {
                    db.beginTransaction();
                    for (ContentValues cv : values) {
                        db.insert(tableName, null, cv);
                    }
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    db.endTransaction();
                }
            }
        }
        return values.length;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    protected void initMatcher() {
        uriMatcher.addURI(AUTHORITY,DatabaseUris.PATH_STUDENT_INFO,TokenConstants.TOKEN_STUDENT_INFO);
    }

    protected String getTableNameFromUri(Uri uri) {
        int match = uriMatcher.match(uri);
        String tableName = null;
        switch (match) {
            case TokenConstants.TOKEN_STUDENT_INFO: {
                tableName = DatabaseConstants.Tables.TAB_NAME_STUDENT;
                break;
            }
        }
        return tableName;
    }
}
