package com.zlove.db.bean;

import android.content.ContentValues;
import android.database.Cursor;

import com.zlove.bean.AppBean;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by ZLOVE on 2015/3/2.
 */
public abstract class BaseBeanDB extends AppBean {
    public BaseBeanDB(){}
    public abstract ContentValues getContentValues();
    public abstract BaseBeanDB readFromCursor(Cursor cursor);

    public static <T extends BaseBeanDB> List<T> readListFromCursor(Cursor cursor, T baseBean) {
        return  readListFromCursor(cursor, baseBean, false);
    }

    public static <T extends BaseBeanDB> List<T> readListFromCursor(Cursor cursor, T baseBean, boolean reverseOrder) {
        if (cursor == null) {
            new IllegalStateException("Read From Cursor cursor null").printStackTrace();
            return null;
        }

        List<T> result = new LinkedList<T>();
        if (!reverseOrder) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                T item = (T) baseBean.readFromCursor(cursor);
                if (item != null) {
                    result.add(item);
                }
            }
        } else {
            for (cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
                T item = (T) baseBean.readFromCursor(cursor);
                if (item != null) {
                    result.add(item);
                }
            }
        }
        return result;
    }

}
