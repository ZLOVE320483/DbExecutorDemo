package com.zlove.db.executor;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.zlove.app.ApplicationUtil;
import com.zlove.db.DatabaseExecutor;
import com.zlove.db.DatabaseManager;
import com.zlove.util.ListUtils;

import java.util.List;

/**
 * Created by ZLOVE on 2015/3/2.
 */
public abstract class BaseDatabaseExecutor extends DatabaseExecutor.ExecuteRobin {

    protected String tableName;
    protected List<SQLSentence> sqlSentences;
    protected List<Uri> notifyUris;

    public void execute(BaseDatabaseExecutor executor) {
        DatabaseExecutor.getInstance().execute(executor);
    }

    public BaseDatabaseExecutor(String tableName, List<SQLSentence> sqlSentences, List<Uri> notifyUris) {
        this.tableName = tableName;
        this.sqlSentences = sqlSentences;
        this.notifyUris = notifyUris;
    }

    @Override
    protected boolean runImp() throws Exception {
        SQLiteDatabase database = DatabaseManager.getInstance().getWritableDatabase();
        runImp(database);
        notifyUri();
        return false;
    }

    protected void notifyUri() {
        if (ListUtils.isEmpty(notifyUris)) {
            return;
        }
        for (Uri uri : notifyUris) {
            Log.d("ZLOVE","Notify Uris");
            ApplicationUtil.getContentResolver().notifyChange(uri, null);
        }
    }

    public static class SQLSentence {
        public String whereClause;
        public String[] whereArgs;
        public ContentValues contentValues;

        public SQLSentence(String whereClause, String[] whereArgs, ContentValues contentValues) {
            this.whereClause = whereClause;
            this.whereArgs = whereArgs;
            this.contentValues = contentValues;
        }
    }

}
