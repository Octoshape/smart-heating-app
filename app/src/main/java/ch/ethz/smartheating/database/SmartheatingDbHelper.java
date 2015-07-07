package ch.ethz.smartheating.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by schmisam on 25/05/15.
 */
/* DBHelper class */
public class SmartheatingDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "smartHeating.db";

    public SmartheatingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SmartheatingContract.Rooms.SQL_CREATE_TABLE);
        db.execSQL(SmartheatingContract.Thermostats.SQL_CREATE_TABLE);
        db.execSQL(SmartheatingContract.Schedules.SQL_CREATE_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // We don't do upgrades, simply reset the whole database.
        db.execSQL(SmartheatingContract.Rooms.SQL_DELETE_TABLE);
        db.execSQL(SmartheatingContract.Thermostats.SQL_DELETE_TABLE);
        db.execSQL(SmartheatingContract.Schedules.SQL_DELETE_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // We don't do downgrades, simply reset the whole database.
        onUpgrade(db, oldVersion, newVersion);
    }

    public void updateAllRoomTemps(SQLiteDatabase db) {
        Cursor roomCursor = db.rawQuery("SELECT " + SmartheatingContract.Rooms._ID + " FROM " + SmartheatingContract.Rooms.TABLE_NAME, null);
        roomCursor.moveToFirst();
        while(!roomCursor.isAfterLast()) {
            int nextRoomID = roomCursor.getInt(0);
            updateRoomTemperature(db, nextRoomID);
            roomCursor.moveToNext();
        }
    }

    public void updateRoomTemperature(SQLiteDatabase db, int roomID) {
        Cursor c = db.rawQuery(SmartheatingContract.GET_AVG_TEMP(roomID), null);
        c.moveToFirst();
        double res = c.getDouble(0);
        c.close();
        db.execSQL(SmartheatingContract.UPDATE_TEMPERATURE(roomID, res));
    }

    public void updateRoomServerID (SQLiteDatabase db, int room_id, int server_id) {
        db.execSQL(SmartheatingContract.UPDATE_SERVER_ID(room_id, server_id));
    }
}
