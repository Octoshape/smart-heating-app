package ch.ethz.smartheating.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.ethz.smartheating.model.Room;
import ch.ethz.smartheating.model.ScheduleEntry;
import ch.ethz.smartheating.model.Thermostat;
import ch.ethz.smartheating.networking.Request;

/**
 * The database helper class. This class handles all the connections and requests to the database.
 * Most of the functions require a {@link SQLiteDatabase} as a parameter to execute the requests on.
 * The caller himself is required to make sure the database gets closed after calling these functions.
 */
public class SmartheatingDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "smartHeating.db";

    public SmartheatingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Create the database, adding all the tables.
     *
     * @param db The {@link SQLiteDatabase}. Must be WriteableDatabase.
     */
    public void onCreate(SQLiteDatabase db) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.onCreate needs a writeable database.");
        }
        db.execSQL(SmartheatingContract.Rooms.SQL_CREATE_TABLE);
        db.execSQL(SmartheatingContract.Thermostats.SQL_CREATE_TABLE);
        db.execSQL(SmartheatingContract.Schedules.SQL_CREATE_TABLE);
    }

    /**
     * Called on upgrading the database. Not used in this application.
     */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Called on downgrading the database. Not used in this application.
     */
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Reset the database. Deleting all the tables first, then calling {@code onCreate} again.
     */
    public void resetDatabase() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(SmartheatingContract.Rooms.SQL_DELETE_TABLE);
        db.execSQL(SmartheatingContract.Thermostats.SQL_DELETE_TABLE);
        db.execSQL(SmartheatingContract.Schedules.SQL_DELETE_TABLE);
        onCreate(db);
        db.close();
    }

    /**
     * Update all the rooms' temperatures by taking the average values from all the thermostats for
     * one room.
     *
     * @param db The {@link SQLiteDatabase}. Must be WriteableDatabase.
     */
    public void updateAllRoomTemps(SQLiteDatabase db) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.updateAllRoomTemps needs a writeable database.");
        }
        Cursor roomCursor = db.rawQuery("SELECT " + SmartheatingContract.Rooms._ID + " FROM " + SmartheatingContract.Rooms.TABLE_NAME, null);
        roomCursor.moveToFirst();
        while (!roomCursor.isAfterLast()) {
            int nextRoomID = roomCursor.getInt(roomCursor.getColumnIndex(SmartheatingContract.Rooms._ID));
            updateRoomTemperature(db, nextRoomID);
            roomCursor.moveToNext();
        }
        roomCursor.close();
    }

    /**
     * Update the temperature for a single room by taking the average values from all thermostats in
     * that room.
     *
     * @param db     The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param roomID The local id of the room to be updated.
     */
    private void updateRoomTemperature(SQLiteDatabase db, int roomID) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.updateRoomTemperature needs a writeable database.");
        }
        Cursor c = db.rawQuery("SELECT AVG (" + SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE + ")" +
                " FROM " + SmartheatingContract.Thermostats.TABLE_NAME +
                " WHERE " + SmartheatingContract.Thermostats.COLUMN_NAME_ROOM_ID + " = " + roomID +
                " AND " + SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE + " <> " + 0, null);
        c.moveToFirst();
        double temperature = 0;
        while (!c.isAfterLast()) {
            temperature = c.getDouble(0);
            c.moveToNext();
        }
        c.close();

        ContentValues values = new ContentValues();
        values.put(SmartheatingContract.Rooms.COLUMN_NAME_TEMPERATURE, temperature);

        db.update(SmartheatingContract.Rooms.TABLE_NAME, values, SmartheatingContract.Rooms._ID + " = " + roomID, null);
    }

    /**
     * Update the server id column for a room.
     *
     * @param db        The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param room_id   The local id of the room to be updated.
     * @param server_id The server id of the room to be updated.
     */
    public void updateRoomServerID(SQLiteDatabase db, int room_id, int server_id) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.updateRoomServerID needs a writeable database.");
        }
        ContentValues values = new ContentValues();
        values.put(SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID, server_id);
        db.update(SmartheatingContract.Rooms.TABLE_NAME, values, SmartheatingContract.Rooms._ID + " = " + room_id, null);
    }

    /**
     * Return the server id of a room.
     *
     * @param db      The {@link SQLiteDatabase}.
     * @param room_id The local id of the room in question.
     * @return The server id of the room.
     */
    public int getRoomServerID(SQLiteDatabase db, int room_id) {
        Cursor c = db.rawQuery("SELECT " + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID +
                " FROM " + SmartheatingContract.Rooms.TABLE_NAME +
                " WHERE " + SmartheatingContract.Rooms._ID +
                " = " + room_id, null);

        int server_id = -1;
        c.moveToFirst();
        while (!c.isAfterLast()) {
            server_id = c.getInt(c.getColumnIndex(SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID));
            c.moveToNext();
        }
        c.close();
        return server_id;
    }

    /**
     * Add a new room to the local database.
     *
     * @param db   The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param name The name of the new room.
     * @return The local id of the new room.
     */
    public int addRoom(SQLiteDatabase db, String name) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.addRoom needs a writeable database.");
        }

        ContentValues values = new ContentValues();
        values.put(SmartheatingContract.Rooms.COLUMN_NAME_NAME, name);
        values.put(SmartheatingContract.Rooms.COLUMN_NAME_TEMPERATURE, 0);
        values.put(SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID, -1);

        return (int) db.insert(SmartheatingContract.Rooms.TABLE_NAME, null, values);
    }

    /**
     * Add a new thermostat to the local database.
     *
     * @param db      The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param RFID    The RFID of the new thermostat.
     * @param name    The name of the new thermostat.
     * @param room_id The local id of the room the new thermostat is in.
     * @return The local id of the new thermostat.
     */
    public int addThermostat(SQLiteDatabase db, String RFID, String name, int room_id) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.addThermostat needs a writeable database.");
        }

        ContentValues values = new ContentValues();
        values.put(SmartheatingContract.Thermostats.COLUMN_NAME_RFID, RFID);
        values.put(SmartheatingContract.Thermostats.COLUMN_NAME_NAME, name);
        values.put(SmartheatingContract.Thermostats.COLUMN_NAME_ROOM_ID, room_id);
        values.put(SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE, 0);

        return (int) db.insert(SmartheatingContract.Thermostats.TABLE_NAME, null, values);
    }

    /**
     * Upsert a room in the local database. This method will insert a new room into the database or
     * replace its values if it already exists. This method is called by {@code updateRooms}.
     *
     * @param db        The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param server_id The server id of the room to be upserted.
     * @param name      The name of the room to be upserted (needed in case it's a new room).
     */
    private void upsertRoom(SQLiteDatabase db, int server_id, String name) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.upsertRoom needs a writeable database.");
        }

        db.execSQL("INSERT OR REPLACE INTO " + SmartheatingContract.Rooms.TABLE_NAME
                + "(" + SmartheatingContract.Rooms._ID + ", "
                + SmartheatingContract.Rooms.COLUMN_NAME_NAME + ", "
                + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID + ", "
                + SmartheatingContract.Rooms.COLUMN_NAME_TEMPERATURE + ")"
                + " VALUES((SELECT " + SmartheatingContract.Rooms._ID + " FROM " + SmartheatingContract.Rooms.TABLE_NAME
                + " WHERE " + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID + " = " + server_id + "), '"
                + name + "', "
                + server_id + ", "
                + "COALESCE((SELECT " + SmartheatingContract.Rooms.COLUMN_NAME_TEMPERATURE + " FROM " + SmartheatingContract.Rooms.TABLE_NAME
                + " WHERE " + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID + " = " + server_id + "), 0))");
    }

    /**
     * Update all the rooms in the system. This method will call {@code upsertRoom} for every room
     * in the system.
     *
     * @param db         The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param server_ids A {@link List} with the server ids of the rooms.
     * @param names      A {@link List} with the names of the rooms.
     */
    public void updateRooms(SQLiteDatabase db, List<Integer> server_ids, List<String> names) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.updateRooms needs a writeable database.");
        }
        // Delete any rooms that are in the db but not on the server.
        db.execSQL("DELETE FROM " + SmartheatingContract.Rooms.TABLE_NAME
                + " WHERE " + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID
                + " NOT IN (" + TextUtils.join(",", server_ids) + ")");

        // Upsert the rooms from the server into the db.
        for (int i = 0; i < server_ids.size(); i++) {
            upsertRoom(db, server_ids.get(i), names.get(i));
        }
    }

    /**
     * Upsert a thermostat in the local database. This method will insert a new thermostat into the
     * database or replace its values if it already exists. This method is called by {@code updateThermostats}.
     *
     * @param db        The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param server_id The server_id of the room the thermostat belongs to.
     * @param name      The name of the thermostat (needed if it's a new thermostat).
     * @param RFID      The RFID of the thermostat.
     */
    private void upsertThermostat(SQLiteDatabase db, int server_id, String name, String RFID) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.upsertThermostat needs a writeable database.");
        }

        db.execSQL("INSERT OR REPLACE INTO " + SmartheatingContract.Thermostats.TABLE_NAME
                + "(" + SmartheatingContract.Thermostats.COLUMN_NAME_RFID + ", "
                + SmartheatingContract.Thermostats.COLUMN_NAME_ROOM_ID + ", "
                + SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE + ", "
                + SmartheatingContract.Thermostats.COLUMN_NAME_NAME + ") "
                + " VALUES('" + RFID + "', "
                + "(SELECT " + SmartheatingContract.Rooms._ID + " FROM " + SmartheatingContract.Rooms.TABLE_NAME
                + " WHERE " + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID + " = " + server_id + "), "
                + "(SELECT " + SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE + " FROM " + SmartheatingContract.Thermostats.TABLE_NAME
                + " WHERE " + SmartheatingContract.Thermostats.COLUMN_NAME_RFID + " = '" + RFID + "'), '"
                + name + "')");
    }

    /**
     * Update all the thermostats in a room. This method will call {@code upsertThermostat} for all
     * thermostats in the room.
     *
     * @param db        The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param server_id The server id of the room.
     * @param RFIDs     A {@link List} with RFIDs of the thermostats.
     * @param names     A {@link List} with names of the thermostats.
     */
    public void updateThermostats(SQLiteDatabase db, int server_id, List<String> RFIDs, List<String> names) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheadingDbHelper.updateThermostats needs a writeable database.");
        }

        // Delete any thermostats that are in the db but not on the server.
        db.execSQL("DELETE FROM " + SmartheatingContract.Thermostats.TABLE_NAME
                + " WHERE " + SmartheatingContract.Thermostats.COLUMN_NAME_ROOM_ID + " = "
                + "(SELECT " + SmartheatingContract.Rooms._ID + " FROM " + SmartheatingContract.Rooms.TABLE_NAME
                + " WHERE " + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID + " = " + server_id + ")"
                + " AND " + SmartheatingContract.Thermostats.COLUMN_NAME_RFID
                + " NOT IN ('" + TextUtils.join("','", RFIDs) + "')");

        for (int i = 0; i < RFIDs.size(); i++) {
            upsertThermostat(db, server_id, names.get(i), RFIDs.get(i));
        }
    }

    /**
     * Update the schedule for a room.
     *
     * @param db        The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param entries   A {@link List} of {@link ScheduleEntry}s which make up the schedule.
     * @param server_id The server id of the room.
     */
    public void updateSchedule(SQLiteDatabase db, List<ScheduleEntry> entries, int server_id) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.resetSchedule needs a writeable database.");
        }

        int room_id = -1;
        Cursor c = db.rawQuery("SELECT " + SmartheatingContract.Rooms._ID + " FROM " + SmartheatingContract.Rooms.TABLE_NAME
                + " WHERE " + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID + " = " + server_id, null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            room_id = c.getInt(c.getColumnIndex(SmartheatingContract.Rooms._ID));
            c.moveToNext();
        }
        c.close();

        // Delete schedule to be updated.
        db.delete(SmartheatingContract.Schedules.TABLE_NAME, SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID + " = " + room_id, null);

        // Add new schedule.
        ContentValues values = new ContentValues();
        for (ScheduleEntry entry : entries) {
            values.put(SmartheatingContract.Schedules.COLUMN_NAME_START_TIME, entry.getStartTime());
            values.put(SmartheatingContract.Schedules.COLUMN_NAME_END_TIME, entry.getEndTime());
            values.put(SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE, entry.getTemperature());
            values.put(SmartheatingContract.Schedules.COLUMN_NAME_DAY, entry.getDay());
            values.put(SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID, room_id);
            db.insert(SmartheatingContract.Schedules.TABLE_NAME, null, values);
        }
    }

    /**
     * Update the temperature of a Thermostat.
     *
     * @param db    The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param RFID  The RFID of the thermostat.
     * @param value The value of the new temperature.
     */
    public void updateTemperature(SQLiteDatabase db, String RFID, double value) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.updateTemperature needs a writeable database.");
        }

        db.execSQL("UPDATE " + SmartheatingContract.Thermostats.TABLE_NAME + " SET " + SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE
                + " = " + value + " WHERE " + SmartheatingContract.Thermostats.COLUMN_NAME_RFID + " = '" + RFID + "'");
    }

    /**
     * Reset the schedule of the given room to the default heating schedule.
     *
     * @param db      The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param room_id The room to reset the schedule for.
     */
    public void resetSchedule(SQLiteDatabase db, int room_id) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.resetSchedule needs a writeable database.");
        }

        db.delete(SmartheatingContract.Schedules.TABLE_NAME, SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID + " = " + room_id, null);

        db.execSQL("INSERT INTO " + SmartheatingContract.Schedules.TABLE_NAME
                + " (" + SmartheatingContract.Schedules.COLUMN_NAME_DAY + "," + SmartheatingContract.Schedules.COLUMN_NAME_START_TIME
                + "," + SmartheatingContract.Schedules.COLUMN_NAME_END_TIME + "," + SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID
                + "," + SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE + ") SELECT "
                + SmartheatingContract.Schedules.COLUMN_NAME_DAY + "," + SmartheatingContract.Schedules.COLUMN_NAME_START_TIME
                + "," + SmartheatingContract.Schedules.COLUMN_NAME_END_TIME + "," + room_id
                + "," + SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE + " FROM " + SmartheatingContract.Schedules.TABLE_NAME
                + " WHERE " + SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID + " = " + SmartheatingContract.Rooms.DEFAULT_ID);
    }

    /**
     * Return the schedule of a room.
     *
     * @param db      The {@link SQLiteDatabase}.
     * @param room_id The local id of the room.
     * @return An {@link ArrayList} of {@link ScheduleEntry}s which make up the schedule.
     */
    public ArrayList<ScheduleEntry> getSchedule(SQLiteDatabase db, int room_id) {
        Cursor c = db.rawQuery("SELECT " + SmartheatingContract.Schedules.COLUMN_NAME_START_TIME + ", "
                + SmartheatingContract.Schedules.COLUMN_NAME_END_TIME + ", "
                + SmartheatingContract.Schedules.COLUMN_NAME_DAY + ", "
                + SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE
                + " FROM " + SmartheatingContract.Schedules.TABLE_NAME
                + " WHERE " + SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID + " = " + room_id, null);

        ArrayList<ScheduleEntry> entries = new ArrayList<>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            int start = c.getInt(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_START_TIME));
            int end = c.getInt(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_END_TIME));
            int day = c.getInt(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_DAY));
            double temp = c.getDouble(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE));
            entries.add(new ScheduleEntry(temp, start, end, day));
            c.moveToNext();
        }
        c.close();

        return entries;
    }

    /**
     * Return all the rooms of the heating system.
     *
     * @param db The {@link SQLiteDatabase}.
     * @return An {@link ArrayList} of the {@link Room}s of the system.
     */
    public ArrayList<Room> getRooms(SQLiteDatabase db) {
        ArrayList<Room> rooms = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT " + SmartheatingContract.Rooms.COLUMN_NAME_NAME
                + ", " + SmartheatingContract.Rooms.COLUMN_NAME_TEMPERATURE
                + ", " + SmartheatingContract.Rooms._ID
                + " FROM " + SmartheatingContract.Rooms.TABLE_NAME
                + " WHERE " + SmartheatingContract.Rooms._ID
                + " <> " + SmartheatingContract.Rooms.DEFAULT_ID, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            rooms.add(new Room(cursor.getString(cursor.getColumnIndex(SmartheatingContract.Rooms.COLUMN_NAME_NAME)),
                    cursor.getInt(cursor.getColumnIndex(SmartheatingContract.Rooms._ID)),
                    Math.round(cursor.getDouble(cursor.getColumnIndex(SmartheatingContract.Rooms.COLUMN_NAME_TEMPERATURE)) * 2) / 2d));
            cursor.moveToNext();
        }
        cursor.close();

        return rooms;
    }

    /**
     * Return all the thermostats of a room.
     *
     * @param db     The {@link SQLiteDatabase}.
     * @param roomID The local id of the room.
     * @return An {@link ArrayList} of the {@link Thermostat}s of the room.
     */
    public ArrayList<Thermostat> getThermostats(SQLiteDatabase db, int roomID) {
        ArrayList<Thermostat> thermostats = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT " + SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE + ", "
                + SmartheatingContract.Thermostats.COLUMN_NAME_RFID + ", "
                + SmartheatingContract.Thermostats.COLUMN_NAME_NAME + " FROM "
                + SmartheatingContract.Thermostats.TABLE_NAME + " WHERE "
                + SmartheatingContract.Thermostats.COLUMN_NAME_ROOM_ID + " LIKE " + roomID, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String RFID = cursor.getString(cursor.getColumnIndex(SmartheatingContract.Thermostats.COLUMN_NAME_RFID));
            String name = cursor.getString(cursor.getColumnIndex(SmartheatingContract.Thermostats.COLUMN_NAME_NAME));
            double temperature = cursor.getDouble(cursor.getColumnIndex(SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE));

            thermostats.add(new Thermostat(RFID, roomID, name, temperature));
            cursor.moveToNext();
        }
        cursor.close();

        return thermostats;
    }

    /**
     * Removes a room from the local database as well as from the server.
     *
     * @param db      The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param roomID  The local id of the room.
     * @param context The {@link Context} of the application.
     */
    public void deleteRoom(SQLiteDatabase db, int roomID, Context context) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.deleteRoom needs a writeable database.");
        }
        Cursor cursor = db.rawQuery("SELECT " + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID
                + " FROM " + SmartheatingContract.Rooms.TABLE_NAME
                + " WHERE " + SmartheatingContract.Rooms._ID
                + " = " + roomID, null);
        cursor.moveToFirst();
        int serverID = -1;
        while (!cursor.isAfterLast()) {
            serverID = cursor.getInt(cursor.getColumnIndex(SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID));
            cursor.moveToNext();
        }
        cursor.close();
        if (serverID != -1) {
            new Request(context).deleteRoom(serverID);
        }

        db.delete(SmartheatingContract.Rooms.TABLE_NAME, SmartheatingContract.Rooms._ID + " = " + roomID, null);
    }

    /**
     * Removes a thermostat from the local database as well as from the server.
     *
     * @param db      The {@link SQLiteDatabase}. Must be WriteableDatabase.
     * @param RFID    The RFID of the thermostat to be deleted.
     * @param roomID  The local id of the room where the thermostat is located.
     * @param context The {@link Context} of the application.
     */
    public void deleteThermostat(SQLiteDatabase db, String RFID, int roomID, Context context) {
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("SmartheatingDbHelper.deleteThermostat needs a writeable database.");
        }

        db.delete(SmartheatingContract.Thermostats.TABLE_NAME, SmartheatingContract.Thermostats.COLUMN_NAME_RFID + " = " + RFID, null);
        Cursor c = db.rawQuery("SELECT " + SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID
                + " FROM " + SmartheatingContract.Rooms.TABLE_NAME
                + " WHERE " + SmartheatingContract.Rooms._ID + " = " + roomID, null);
        c.moveToFirst();
        int serverID = -1;
        while (!c.isAfterLast()) {
            serverID = c.getInt(c.getColumnIndex(SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID));
            c.moveToNext();
        }
        c.close();
        new Request(context).deleteThermostat(serverID, RFID);
    }


    /**
     * Return the current target temperature for the given room.
     *
     * @param db     The {@link SQLiteDatabase}.
     * @param roomID The local id of the room.
     * @return The temperature of the corresponding room.
     */
    public double getCurrentTargetTemperature(SQLiteDatabase db, int roomID) {
        double targetTemp = 0;
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentDay = now.get(Calendar.DAY_OF_WEEK) - 1;
        if (currentDay == 0) currentDay = 7;
        Cursor c = db.rawQuery("SELECT " + SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE
                + " FROM " + SmartheatingContract.Schedules.TABLE_NAME
                + " WHERE " + SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID + " = " + roomID
                + " AND " + SmartheatingContract.Schedules.COLUMN_NAME_DAY + " = " + currentDay
                + " AND " + SmartheatingContract.Schedules.COLUMN_NAME_START_TIME + " <= " + currentHour
                + " AND " + SmartheatingContract.Schedules.COLUMN_NAME_END_TIME + " >= " + currentHour, null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            double tempValRaw = c.getDouble(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE));
            targetTemp = Math.round(tempValRaw * 2) / 2d;
            c.moveToNext();
        }
        c.close();
        return targetTemp;
    }

    /**
     * Return the current temperature of a room.
     *
     * @param db     The {@link SQLiteDatabase}.
     * @param roomID The local id of the room.
     * @return The current temperature of the room.
     */
    public double getRoomTemperature(SQLiteDatabase db, int roomID) {
        double temperature = 0;
        Cursor c = db.rawQuery("SELECT " + SmartheatingContract.Rooms.COLUMN_NAME_TEMPERATURE +
                " FROM " + SmartheatingContract.Rooms.TABLE_NAME +
                " WHERE " + SmartheatingContract.Rooms._ID +
                " = " + roomID, null);

        c.moveToFirst();
        while (!c.isAfterLast()) {
            temperature = c.getDouble(c.getColumnIndex(SmartheatingContract.Rooms.COLUMN_NAME_TEMPERATURE));
            c.moveToNext();
        }
        c.close();
        return temperature;
    }

    /**
     * Return the current schedule for a room.
     *
     * @param db     The {@link SQLiteDatabase}.
     * @param roomID The local id of the room.
     * @return The current schedule of the room.
     */
    public ArrayList<ScheduleEntry> getCurrentSchedule(SQLiteDatabase db, int roomID) {
        ArrayList<ScheduleEntry> entries = new ArrayList<>();

        Cursor c = db.rawQuery("SELECT * FROM " + SmartheatingContract.Schedules.TABLE_NAME +
                " WHERE " + SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID + " = " + roomID, null);
        c.moveToFirst();

        while (!c.isAfterLast()) {
            int start = c.getInt(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_START_TIME));
            int end = c.getInt(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_END_TIME));
            int day = c.getInt(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_DAY));
            double temp = c.getDouble(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE));

            entries.add(new ScheduleEntry(temp, start, end, day));
            c.moveToNext();
        }
        c.close();

        return entries;
    }

    /**
     * Return the current ScheduleEntry.
     *
     * @param db     The {@link SQLiteDatabase}.
     * @param roomID The local id of the room.
     * @return The current schedule of the room.
     */
    public ScheduleEntry getCurrentScheduleEntry(SQLiteDatabase db, int roomID) {
        ScheduleEntry entry = new ScheduleEntry(0, 0, 0, 0);
        int currentDay = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) - 1;
        if (currentDay == 0) currentDay = 7;
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        Cursor c = db.rawQuery("SELECT * FROM " + SmartheatingContract.Schedules.TABLE_NAME
                + " WHERE " + SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID + " = " + roomID
                + " AND " + SmartheatingContract.Schedules.COLUMN_NAME_DAY + " = " + currentDay
                + " AND " + SmartheatingContract.Schedules.COLUMN_NAME_START_TIME + " <= " + currentHour
                + " AND " + SmartheatingContract.Schedules.COLUMN_NAME_END_TIME + " >= " + currentHour, null);
        c.moveToFirst();

        while (!c.isAfterLast()) {
            int start = c.getInt(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_START_TIME));
            int end = c.getInt(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_END_TIME));
            int day = c.getInt(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_DAY));
            double temp = c.getDouble(c.getColumnIndex(SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE));

            entry = new ScheduleEntry(temp, start, end, day);
            c.moveToNext();
        }
        c.close();

        return entry;
    }

    /**
     * Set the target temperature of a room by modifying its current schedule.
     *
     * @param db         The {@link SQLiteDatabase}.
     * @param targetTemp The desired target temperature.
     * @param roomID     The local id of the room.
     */
    public void setCurrentTargetTemperature(SQLiteDatabase db, double targetTemp, int roomID) {
        int currentDay = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) - 1;
        if (currentDay == 0) currentDay = 7;
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        db.execSQL("UPDATE " + SmartheatingContract.Schedules.TABLE_NAME + " SET "
                + SmartheatingContract.Schedules.COLUMN_NAME_TEMPERATURE + " = " + targetTemp
                + " WHERE " + SmartheatingContract.Schedules.COLUMN_NAME_ROOM_ID + " = " + roomID
                + " AND " + SmartheatingContract.Schedules.COLUMN_NAME_DAY + " = " + currentDay
                + " AND " + SmartheatingContract.Schedules.COLUMN_NAME_START_TIME + " <= " + currentHour
                + " AND " + SmartheatingContract.Schedules.COLUMN_NAME_END_TIME + " >= " + currentHour);
    }
}
