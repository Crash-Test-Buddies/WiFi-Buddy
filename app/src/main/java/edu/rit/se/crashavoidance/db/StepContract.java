package edu.rit.se.crashavoidance.db;

import android.provider.BaseColumns;

/**
 * Created by Chris on 6/12/2016.
 */
public class StepContract {

    public StepContract() {

    }
    public static abstract class StepEntry implements BaseColumns {
        public static final String TABLE_NAME = "Step";
        public static final String COLUMN_NAME_STEP_NAME = "step_name";
    }
}
