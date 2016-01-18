package com.zlove.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.zlove.util.executor.ScheduledPriThreadPoolExecutor;

/**
 * Created by ZLOVE on 2015/3/2.
 */
public class DatabaseExecutor extends ScheduledPriThreadPoolExecutor {

    private static final String LOG_TAG = DatabaseExecutor.class.getSimpleName();
    private static DatabaseExecutor instance;

    public static DatabaseExecutor getInstance() {
        if (instance == null) {
            instance = new DatabaseExecutor();
        }
        return instance;
    }

    private DatabaseExecutor() {
        super(1);
    }

    public abstract static class ExecuteRobin implements Runnable, Priority {

        private boolean cancel = false;
        private boolean isDone = false;

        // Return false when done
        protected abstract boolean runImp() throws Exception;

        public abstract boolean runImp(SQLiteDatabase db) throws Exception;

        public void cancel() {
            cancel = true;
        }

        @Override
        public void run() {
            Log.d(LOG_TAG, "ExecuteRoundRobin: " + getClass().getName());
            int pri = android.os.Process.getThreadPriority(android.os.Process.myTid());
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                if (!cancel) {
                    runImp();
                    isDone = true;
                } else {
                    Log.w(LOG_TAG, "ExecuteRoundRobin canceled: " + this.getClass().getName());
                }
            } catch (Throwable t) {
                t.printStackTrace();
                isDone = true;
                Log.d(LOG_TAG, "ExecuteRoundRobin exception: " + this.getClass().getSimpleName() + ": " + t.toString());
            } finally {
                android.os.Process.setThreadPriority(pri);
            }
        }

        @Override
        public int getPriority() {
            return Priority.HIGH;
        }

        public boolean isDone() {
            return isDone;
        }
    }

}
