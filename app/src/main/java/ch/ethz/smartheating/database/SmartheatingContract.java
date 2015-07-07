package ch.ethz.smartheating.database;

import android.provider.BaseColumns;

/**
 * Created by schmisam on 24/05/15.
 */
public final class SmartheatingContract {

    // To prevent someone from accidentally instantiating the contract class,
    // we give it an empty constructor.
    public SmartheatingContract() {}

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String DOUBLE_TYPE = " DOUBLE";
    private static final String COMMA_SEP = ",";

    public static String GET_AVG_TEMP (int room_id) {
        return "SELECT AVG (" + Thermostats.COLUMN_NAME_TEMPERATURE + ")" +
                " FROM " + Thermostats.TABLE_NAME +
                " WHERE " + Thermostats.COLUMN_NAME_ROOM_ID + " = " + room_id;
    }

    public static String UPDATE_TEMPERATURE (int room_id, double value) {
        return "UPDATE " + Rooms.TABLE_NAME +
                " SET " + Rooms.COLUMN_NAME_TEMPERATURE + " = " + value +
                " WHERE " + Rooms._ID + " = " + room_id;
    }

    public static String UPDATE_SERVER_ID (int room_id, int server_id) {
        return "UPDATE " + Rooms.TABLE_NAME +
                " SET " + Rooms.COLUMN_NAME_SERVER_ID + " = " + server_id +
                " WHERE " + Rooms._ID + " = " + room_id;
    }

    /* Inner class that defines the table contents */
    public static abstract class Rooms implements BaseColumns {

        public static final String TABLE_NAME = "rooms";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_TEMPERATURE = "temperature";
        public static final String COLUMN_NAME_SERVER_ID = "server_id";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + Rooms.TABLE_NAME + " (" +
                        Rooms._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                        Rooms.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                        Rooms.COLUMN_NAME_TEMPERATURE + DOUBLE_TYPE + COMMA_SEP +
                        Rooms.COLUMN_NAME_SERVER_ID + INTEGER_TYPE +
                        " )";

        public static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + Rooms.TABLE_NAME;
    }

    /* Inner class that defines the table contents */
    public static abstract class Thermostats implements BaseColumns {

        public static final String TABLE_NAME = "thermostats";
        public static final String COLUMN_NAME_RFID = "rfid";
        public static final String COLUMN_NAME_ROOM_ID = "room_id";
        public static final String COLUMN_NAME_TEMPERATURE = "temperature";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + Thermostats.TABLE_NAME + " (" +
                        Thermostats._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                        Thermostats.COLUMN_NAME_RFID + TEXT_TYPE + COMMA_SEP +
                        Thermostats.COLUMN_NAME_ROOM_ID + INTEGER_TYPE + COMMA_SEP +
                        Thermostats.COLUMN_NAME_TEMPERATURE + DOUBLE_TYPE + COMMA_SEP +
                        "FOREIGN KEY (" + Thermostats.COLUMN_NAME_ROOM_ID + ") REFERENCES rooms (" + Rooms._ID + ")" +
                        " )";

        public static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + Thermostats.TABLE_NAME;
    }

    /* Inner class that defines the table contents */
    public static abstract class Schedules implements BaseColumns {

        public static final String TABLE_NAME = "schedules";
        public static final String COLUMN_NAME_ROOM_ID = "room_id";
        public static final String COLUMN_NAME_START_TIME = "start_time";
        public static final String COLUMN_NAME_END_TIME = "end_time";
        public static final String COLUMN_NAME_TEMPERATURE = "temperature";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + Schedules.TABLE_NAME + " (" +
                        Schedules._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                        Schedules.COLUMN_NAME_START_TIME + INTEGER_TYPE + COMMA_SEP +
                        Schedules.COLUMN_NAME_END_TIME + INTEGER_TYPE + COMMA_SEP +
                        Schedules.COLUMN_NAME_TEMPERATURE + DOUBLE_TYPE + COMMA_SEP +
                        Schedules.COLUMN_NAME_ROOM_ID + INTEGER_TYPE + COMMA_SEP +
                        "FOREIGN KEY (" + Schedules.COLUMN_NAME_ROOM_ID + ") REFERENCES rooms (" + Rooms._ID + ")" +
                        " )";

        public static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + Schedules.TABLE_NAME;
    }
}
