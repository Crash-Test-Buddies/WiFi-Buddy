package edu.rit.se.crashavoidance.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Chris on 6/12/2016.
 */
public class WifiDirectDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "WfdDbHelper";
    // Strings for basic SQL command/keywords
    private static final String TEXT = " TEXT";
    private static final String INTEGER = " INTEGER";
    private static final String FOREIGN_KEY = " FOREIGN KEY(";
    private static final String REFERENCES = ") REFERENCES ";
    private static final String COMMA = ", ";
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
            CREATE_TABLE + RunContract.RunEntry.TABLE_NAME + "("
                + RunContract.RunEntry._ID + PRIMARY_KEY
                + COMMA + RunContract.RunEntry.COLUMN_NAME_START_TIME + INTEGER
                + COMMA + RunContract.RunEntry.COLUMN_NAME_END_TIME + INTEGER
                + ")";
    private static final String SQL_CREATE_STEP_TABLE = "" +
            CREATE_TABLE + StepContract.StepEntry.TABLE_NAME + "("
                + StepContract.StepEntry._ID + PRIMARY_KEY
                + COMMA + StepContract.StepEntry.COLUMN_NAME_STEP_NAME + TEXT
                + ")";

    private static final String SQL_CREATE_STEP_TIMER_TABLE =
            CREATE_TABLE + StepTimerContract.StepEntry.TABLE_NAME + "("
                + StepTimerContract.StepEntry._ID + PRIMARY_KEY
                + COMMA + StepTimerContract.StepEntry.COLUMN_NAME_STEP_ID + INTEGER
                + COMMA + StepTimerContract.StepEntry.COLUMN_NAME_PHONE_ID + INTEGER
                + COMMA + StepTimerContract.StepEntry.COLUMN_NAME_START_TIME + INTEGER
                + COMMA + StepTimerContract.StepEntry.COLUMN_NAME_END_TIME + INTEGER
                + COMMA + FOREIGN_KEY + StepTimerContract.StepEntry.COLUMN_NAME_STEP_ID + REFERENCES
                + StepContract.StepEntry.TABLE_NAME + "(" + StepContract.StepEntry._ID + ")"
                + COMMA + FOREIGN_KEY + StepTimerContract.StepEntry.COLUMN_NAME_PHONE_ID + REFERENCES
                + PhoneContract.PhoneEntry.TABLE_NAME + "(" + PhoneContract.PhoneEntry._ID + ")"
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

    /**
     * Called when database is opened (i.e. getWritableDatabase is called)
     * @param db
     */
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PHONE_TABLE);
        db.execSQL(SQL_CREATE_RUN_TABLE);
        db.execSQL(SQL_CREATE_STEP_TABLE);
        db.execSQL(SQL_CREATE_STEP_TIMER_TABLE);
        try {
            populateSteps(db);
        } catch (Exception e) {
            Log.e(TAG, "Exception parsing Step file", e);
        }
    }

    /**
     * Called when the database file exists but the stored version number is lower than requested
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTables(db);
        onCreate(db);
    }

    /**
     * Read sql statements from a file and execute
     * @param db
     * @throws IOException
     */
    public void populateSteps(SQLiteDatabase db) throws IOException {
        URL url = getClass().getResource("StepEntries");
        File file = new File(url.getPath());
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = "";
        db.beginTransaction();
        ContentValues values = new ContentValues();
        while ((line = bufferedReader.readLine()) != null){
            values.put(StepContract.StepEntry.COLUMN_NAME_STEP_NAME, line);
            // Insert the new row, returning the primary key value of the new row
            long newRowId;
            newRowId = db.insert(
                    StepContract.StepEntry.TABLE_NAME
                    ,null
                    ,values);

        }
        db.endTransaction();
    }


    /**
     * Drop all database tables
     * @param db
     */
    private void dropTables(SQLiteDatabase db){
        db.execSQL(SQL_DROP_PHONE_TABLE);
        db.execSQL(SQL_CREATE_RUN_TABLE);
        db.execSQL(SQL_CREATE_STEP_TABLE);
        db.execSQL(SQL_CREATE_STEP_TIMER_TABLE);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public ArrayList<Cursor> getData(String Query){
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[] { "mesage" };
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
        MatrixCursor Cursor2= new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);


        try{
            String maxQuery = Query ;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);


            //add value to cursor2
            Cursor2.addRow(new Object[] { "Success" });

            alc.set(1,Cursor2);
            if (null != c && c.getCount() > 0) {


                alc.set(0,c);
                c.moveToFirst();

                return alc ;
            }
            return alc;
        } catch(SQLException sqlEx){
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        } catch(Exception ex){

            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+ex.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        }


    }


}