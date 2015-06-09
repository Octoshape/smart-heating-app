package ch.ethz.smartheating.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by schmisam on 25/05/15.
 */
/* DBHelper class */
public class smartHeatingDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "smartHeating.db";

    public smartHeatingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(smartHeatingContract.Rooms.SQL_CREATE_TABLE);
        db.execSQL(smartHeatingContract.Thermostats.SQL_CREATE_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // We don't do upgrades, simply reset the whole database.
        db.execSQL(smartHeatingContract.Rooms.SQL_DELETE_TABLE);
        db.execSQL(smartHeatingContract.Thermostats.SQL_DELETE_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // We don't do downgrades, simply reset the whole database.
        onUpgrade(db, oldVersion, newVersion);
    }

    public void updateAllRoomTemps(SQLiteDatabase db) {
        Cursor roomCursor = db.rawQuery("SELECT " + smartHeatingContract.Rooms._ID + " FROM " + smartHeatingContract.Rooms.TABLE_NAME, null);
        roomCursor.moveToFirst();
        while(!roomCursor.isAfterLast()) {
            int nextRoomID = roomCursor.getInt(0);
            updateRoomTemperature(db, nextRoomID);
            roomCursor.moveToNext();
        }
    }

    public void updateRoomTemperature(SQLiteDatabase db, int roomID) {
        Cursor c = db.rawQuery(smartHeatingContract.GET_AVG_TEMP(roomID), null);
        c.moveToFirst();
        double res = c.getDouble(0);
        c.close();
        db.execSQL(smartHeatingContract.UPDATE_TEMPERATURE(roomID, res));
    }

    public void updateRoomServerID (SQLiteDatabase db, int room_id, int server_id) {
        db.execSQL(smartHeatingContract.UPDATE_SERVER_ID(room_id, server_id));
    }
}
