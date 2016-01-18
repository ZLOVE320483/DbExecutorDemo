package com.zlove.configure;

import android.content.Context;

import com.zlove.db.DatabaseManager;

/**
 * Created by ZLOVE on 2015/3/2.
 */
public class DbFactory {
    private static DbFactory instance;

    public static void initFactory(DbFactory factory) {
        instance = factory;
    }

    public static DbFactory getInstance() {
        return instance;
    }

    public DbFactory() {

    }

    public DatabaseManager getDatabaseManager(Context applicationContext, String databaseName) {
        return new DatabaseManager(applicationContext, databaseName);
    }
}
