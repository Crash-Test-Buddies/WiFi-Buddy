package edu.rit.se.crashavoidance.db;

import android.provider.BaseColumns;

/**
 * Created by Chris on 6/12/2016.
 */
public class PhoneContract {
    public PhoneContract(){

    }
    public static abstract class PhoneEntry implements BaseColumns{
        public static final String TABLE_NAME = "Phone";
        public static final String COLUMN_NAME_PHONE_NAME = "phone_name";
        public static final String COLUMN_NAME_DEVICE_ADDRESS = "device_address";
    }
}
