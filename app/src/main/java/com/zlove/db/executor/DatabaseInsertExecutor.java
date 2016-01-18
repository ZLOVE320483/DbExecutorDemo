package com.zlove.db.executor;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.zlove.util.ListUtils;

import java.util.List;

/**
 * Created by ZLOVE on 2015/3/2.
 */
public class DatabaseInsertExecutor extends  BaseDatabaseExecutor {

    public DatabaseInsertExecutor(String tableName, List<SQLSentence> sqlSentences, List<Uri> notifyUris) {
        super(tableName,sqlSentences,notifyUris);
    }

    @Override
    public boolean runImp(SQLiteDatabase db) throws Exception {
        if (!ListUtils.isEmpty(sqlSentences)) {
            if (db != null) {
                for(SQLSentence sql : sqlSentences) {
                    Log.d("ZLOVE","Insert Is Running...");
                    db.insert(tableName, null, sql.contentValues);
                }
            }
        }
        return false;
    }
}
