package com.zlove.db;

/**
 * Created by ZLOVE on 2015/3/2.
 */
public class DatabaseConstants {

    public static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME_FORMAT = "db_love_%s.db";

    public static class Tables {
        public static final String TAB_NAME_STUDENT = "_student";

    }

    public interface StudentColumns {
        public static final String COLUMN_NAME_STUDENT_ID = "_student_id";
        public static final String COLUMN_NAME_STUDENT_NAME = "_student_name";
        public static final String COLUMN_NAME_STUDENT_SEX = "_student_sex";
        public static final String COLUMN_NAME_STUDENT_AGE = "_student_age";
    }

}
