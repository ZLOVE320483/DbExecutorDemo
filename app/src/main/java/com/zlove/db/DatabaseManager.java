package com.zlove.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.zlove.app.ApplicationUtil;
import com.zlove.configure.DbFactory;

import java.util.Arrays;

/**
 * Created by ZLOVE on 2015/3/1.
 */
public class DatabaseManager extends SQLiteOpenHelper {

    private static final String LOG_TAG = DatabaseManager.class.getSimpleName();
    private static DatabaseManager instance;
    private String databaseName;

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (LOG_TAG) {
                if (instance == null) {
                    initDatabase(100);
                }
            }
        }
        return instance;
    }

    public static void initDatabase(int userId) {
        instance = DbFactory.getInstance().getDatabaseManager(ApplicationUtil.getApplicationContext(),String.format(DatabaseConstants.DATABASE_NAME_FORMAT, String.valueOf(userId)));
    }

    public static void releaseInstance() {
        synchronized (LOG_TAG) {
            if (instance != null) {
                instance.close();
                instance = null;
            }
        }
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Only Can be instance once.
     *
     * @param ctx
     */
    public DatabaseManager(Context ctx, String databaseName) {
        this(ctx, databaseName, null, DatabaseConstants.DATABASE_VERSION);
    }

    protected DatabaseManager(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.databaseName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("ZLOVE", "Create Database Success!");
        createStudentTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private void createStudentTable(SQLiteDatabase db) {
        String createSql = "CREATE table " + DatabaseConstants.Tables.TAB_NAME_STUDENT
                + "("
                + DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_ID + " int,"
                + DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_NAME + " varchar(20),"
                + DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_SEX + " varchar(5),"
                + DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_AGE + " int"
                + ");";
        db.execSQL(createSql);

        Log.d("ZLOVE","Create Table Success!");
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        try {
            return super.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.w("db", "getWritableDatabase exception. ReCreate DB...");
        ApplicationUtil.getApplicationContext().deleteDatabase(getDatabaseName());
        return super.getWritableDatabase();
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        try {
            return super.getReadableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.w("db", "getReadableDatabase exception. ReCreate DB...");
        ApplicationUtil.getApplicationContext().deleteDatabase(getDatabaseName());
        return super.getReadableDatabase();
    }

    public void finalize() throws Throwable {
        this.close();
        if (null != instance) {
            instance = null;
        }
        super.finalize();
    }

    public void clearDB() {
        ApplicationUtil.getApplicationContext().deleteDatabase(getDatabaseName());
    }

    public Cursor doQueryAction(String sql, String[] selectionArgs) {
        try {
            return getReadableDatabase().rawQuery(sql,selectionArgs);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (selectionArgs != null) {
                    Log.e(LOG_TAG, "query_exception:" + sql + "," + Arrays.asList(selectionArgs).toString() + " " + e.getMessage());
                } else {
                    Log.e(LOG_TAG, "query_exception:" + sql + ", " + e.getMessage());
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }
}
