package edu.rit.se.crashavoidance.db;

import android.provider.BaseColumns;

/**
 * Created by Chris on 6/12/2016.
 */
public class RunContract {
    public RunContract(){}

    public static abstract class RunEntry implements BaseColumns {
        public static final String TABLE_NAME = "Run";
        public static final String COLUMN_NAME_START_TIME = "start_time";
        public static final String COLUMN_NAME_END_TIME = "end_time";

    }

}
