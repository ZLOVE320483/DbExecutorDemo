package com.zlove.main;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zlove.app.ApplicationUtil;
import com.zlove.app.DatabaseApplication;
import com.zlove.bean.Student;
import com.zlove.db.DatabaseConstants;
import com.zlove.db.DatabaseManager;
import com.zlove.db.bean.BaseBeanDB;
import com.zlove.db.executor.BaseDatabaseExecutor;
import com.zlove.db.executor.DatabaseInsertExecutor;
import com.zlove.provider.DatabaseUris;
import com.zlove.util.FileUtils;
import com.zlove.util.ListUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private TextView tvDbName = null;
    private TextView tvName = null;
    private TextView tvSex = null;
    private TextView tvAge = null;
    private Button exportDbView = null;
    private Button insertDbView = null;
    private List<Uri> notifyUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvName = (TextView) findViewById(R.id.tv_name);
        tvSex = (TextView) findViewById(R.id.tv_sex);
        tvAge = (TextView) findViewById(R.id.tv_age);

        exportDbView = (Button) findViewById(R.id.export_db_btn);
        tvDbName = (TextView) findViewById(R.id.tv_db_name);
        insertDbView = (Button) findViewById(R.id.insert_db_btn);
        exportDbView.setOnClickListener(this);
        insertDbView.setOnClickListener(this);

        SQLiteDatabase db = DatabaseManager.getInstance().getReadableDatabase();
        if (db != null) {
            tvDbName.setText("Database is exist");
        } else {
            tvDbName.setText("Database is null");
        }
        getSupportLoaderManager().initLoader(R.id.id_loader_student_info,savedInstanceState,new StudentLoaderCallback());
        notifyUris.add(DatabaseUris.URI_STUDENT_INFO);
    }

    private class StudentLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(ApplicationUtil.getApplicationContext(), DatabaseUris.URI_STUDENT_INFO,null,null,null,null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            List<Student> students = BaseBeanDB.readListFromCursor(cursor, new Student());
            showStudentInfo(students);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    }

    private void showStudentInfo(List<Student> students) {
        String name = "";
        String sex = "";
        String age = "";
        if (!ListUtils.isEmpty(students)) {
            for(Student student : students) {
                name = name + student.getStudentName() + "  ";
                sex = sex + student.getStudentSex() + "  ";
                age = age + student.getStudentAge() + "  ";
                tvName.setText(name);
                tvSex.setText(sex);
                tvAge.setText(age);
            }
        }
    }

    private void insertDataToDB() {
        Student student = new Student(1,"张三","男",20);
        BaseDatabaseExecutor.SQLSentence sqlSentence = new BaseDatabaseExecutor.SQLSentence(null,null,student.getContentValues());
        List<BaseDatabaseExecutor.SQLSentence> sqlSentences = new ArrayList<>();
        sqlSentences.add(sqlSentence);
        DatabaseInsertExecutor executor = new DatabaseInsertExecutor(DatabaseConstants.Tables.TAB_NAME_STUDENT, sqlSentences, notifyUris);
        executor.execute(executor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v == exportDbView) {
            new ExportDatabaseAsyncTask().execute();
        } else if (v == insertDbView) {
            insertDataToDB();
        }
    }

    private static class ExportDatabaseAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            Toast.makeText(ApplicationUtil.getApplicationContext(), "Export Begin...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            File dbFile = DatabaseApplication.getInstance().getDatabasePath(DatabaseManager.getInstance().getDatabaseName());
            try {
                FileUtils.copy(dbFile, new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "copy_"
                        + DatabaseManager.getInstance().getDatabaseName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(ApplicationUtil.getApplicationContext(), "Export Complete!", Toast.LENGTH_SHORT).show();
        }
    }
}
