package edu.rit.se.crashavoidance.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;

/**
 * Created by Chris on 6/12/2016.
 */
public class WifiDirectDBHelper extends SQLiteOpenHelper {
    // Strings for basic SQL command/keywords
    private static final String TEXT = " TEXT";
    private static final String INTEGER = " INTEGER";
    private static final String FOREIGN_KEY = " FOREIGN KEY(";
    private static final String REFERENCES = "} REFERENCES";
    private static final String COMMA = ",";
    public static final String PRIMARY_KEY = " INTEGER PRIMARY KEY";
    private static final String CREATE_TABLE = "CREATE TABLE ";
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS ";

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "WifiDirect.db";

    // Strings for creating tables
    private static final String SQL_CREATE_PHONE_TABLE =
            CREATE_TABLE + PhoneContract.PhoneEntry.TABLE_NAME + "("
                + PhoneContract.PhoneEntry._ID + PRIMARY_KEY
                + COMMA + PhoneContract.PhoneEntry.COLUMN_NAME_DEVICE_ADDRESS + TEXT
                + COMMA + PhoneContract.PhoneEntry.COLUMN_NAME_PHONE_NAME + TEXT
                + ")";

    private static final String SQL_CREATE_RUN_TABLE =
            CREATE_TABLE + RunContract.RunEntry.TABLE_NAME + "{"
                + RunContract.RunEntry._ID + PRIMARY_KEY
                + COMMA + RunContract.RunEntry.COLUMN_NAME_START_TIME + TEXT
                + COMMA + RunContract.RunEntry.COLUMN_NAME_END_TIME + TEXT
                + ")";
    private static final String SQL_CREATE_STEP_TABLE = "" +
            CREATE_TABLE + StepContract.StepEntry.TABLE_NAME + "("
                + StepContract.StepEntry._ID + PRIMARY_KEY
                + COMMA + StepContract.StepEntry.COLUMN_NAME_STEP_NAME + TEXT
                + ")";

    private static final String SQL_CREATE_STEP_TIMER_TABLE =
            CREATE_TABLE + StepTimerContract.StepEntry._ID + "("
                + StepTimerContract.StepEntry._ID + PRIMARY_KEY
                + COMMA + StepTimerContract.StepEntry.COLUMN_NAME_STEP_ID + INTEGER
                + FOREIGN_KEY + StepTimerContract.StepEntry.COLUMN_NAME_STEP_ID + REFERENCES
                + StepContract.StepEntry.TABLE_NAME + "(" + StepContract.StepEntry._ID + ")) "
                + COMMA + StepTimerContract.StepEntry.COLUMN_NAME_START_TIME + TEXT
                + COMMA + StepTimerContract.StepEntry.TABLE_NAME + TEXT
                + ")";

    // Strings for dropping tables
    private static final String SQL_DROP_PHONE_TABLE =
            DROP_TABLE + PhoneContract.PhoneEntry.TABLE_NAME;
    private static final String SQL_DROP_RUN_TABLE =
            DROP_TABLE + RunContract.RunEntry.TABLE_NAME;
    private static final String SQL_DROP_STEP_TABLE =
            DROP_TABLE + StepContract.StepEntry.TABLE_NAME;
    private static final String SQL_DROP_STEP_TIMER_TABLE =
            DROP_TABLE + StepTimerContract.StepEntry.TABLE_NAME;

    public WifiDirectDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PHONE_TABLE);
        db.execSQL(SQL_CREATE_RUN_TABLE);
        db.execSQL(SQL_CREATE_STEP_TABLE);
        db.execSQL(SQL_CREATE_STEP_TIMER_TABLE);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        dropTables(db);
        onCreate(db);
    }

    private void dropTables(SQLiteDatabase db){
        db.execSQL(SQL_DROP_PHONE_TABLE);
        db.execSQL(SQL_CREATE_RUN_TABLE);
        db.execSQL(SQL_CREATE_STEP_TABLE);
        db.execSQL(SQL_CREATE_STEP_TIMER_TABLE);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    }