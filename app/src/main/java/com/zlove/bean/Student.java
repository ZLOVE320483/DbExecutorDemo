package com.zlove.bean;

import android.content.ContentValues;
import android.database.Cursor;

import com.zlove.db.DatabaseConstants;
import com.zlove.db.bean.BaseBeanDB;

/**
 * Created by ZLOVE on 2015/3/2.
 */
public class Student extends BaseBeanDB {

    private int studentId;
    private String studentName;
    private String studentSex;
    private int studentAge;

    public Student(){}

    public Student(int studentId, String studentName, String studentSex, int studentAge) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentSex = studentSex;
        this.studentAge = studentAge;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public void setStudentSex(String studentSex) {
        this.studentSex = studentSex;
    }

    public void setStudentAge(int studentAge) {
        this.studentAge = studentAge;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentSex() {
        return studentSex;
    }

    public int getStudentAge() {
        return studentAge;
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_ID, getStudentId());
        contentValues.put(DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_NAME, getStudentName());
        contentValues.put(DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_SEX, getStudentSex());
        contentValues.put(DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_AGE, getStudentAge());
        return contentValues;
    }

    @Override
    public BaseBeanDB readFromCursor(Cursor cursor) {
        Student student = new Student();
        int index = -1;
        index = cursor.getColumnIndex(DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_ID);
        if (index != -1) {
            student.setStudentId(cursor.getInt(index));
        }
        index = cursor.getColumnIndex(DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_NAME);
        if (index != -1) {
            student.setStudentName(cursor.getString(index));
        }
        index = cursor.getColumnIndex(DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_SEX);
        if (index != -1) {
            student.setStudentSex(cursor.getString(index));
        }
        index = cursor.getColumnIndex(DatabaseConstants.StudentColumns.COLUMN_NAME_STUDENT_AGE);
        if (index != -1) {
            student.setStudentAge(cursor.getInt(index));
        }
        return student;
    }

}
