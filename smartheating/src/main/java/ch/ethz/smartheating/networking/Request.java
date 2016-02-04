package ch.ethz.smartheating.networking;

import android.content.Context;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.smartheating.database.SmartheatingDbHelper;
import ch.ethz.smartheating.model.ScheduleEntry;
import ch.ethz.smartheating.utilities.Utility;

/**
 * The Request class handles all the communication with the server and sets up the background tasks
 * for receiving and sending data between the server and the application.
 */
public class Request {

    private final Context mContext;
    private static final String logTag = "Request.java";
    private SmartheatingDbHelper mDbHelper = null;

    public Request(Context context) {
        mDbHelper = new SmartheatingDbHelper(context);
        mContext = context;
    }

    /**
     * Register a new residence on the server with the RFID from {@code Utility.RESIDENCE_RFID}.
     */
    public void registerResidence() {

        HttpPost request = null;
        URI uri;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/", null, null);

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("rfid", Utility.RESIDENCE_RFID));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.REGISTER_RESIDENCE);
        worker.execute(request);
    }

    /**
     * Register a new user with the given IMEI in the registered residence on the server.
     *
     * @param IMEI The user's IMEI.
     */
    public void registerUser(String IMEI) {
        HttpPost request = null;
        URI uri;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/" + Utility.RESIDENCE_RFID + "/user/", null, null);

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("imei", IMEI));
            params.add(new BasicNameValuePair("name", "UserName"));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.REGISTER_USER);
        worker.execute(request);
    }

    /**
     * Register a new room on the server.
     *
     * @param name The name of the new room.
     * @param id   The local id in the database of the new room.
     */
    public void registerRoom(String name, int id) {
        HttpPost request = null;
        URI uri;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/" + Utility.RESIDENCE_RFID + "/room/", null, null);

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("name", name));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.ADD_ROOM, new SmartheatingDbHelper(mContext), id);
        worker.execute(request);
    }

    /**
     * Register a new thermostat on the server.
     *
     * @param roomID The local ID of the room the thermostat is in.
     * @param RFID   The RFID of the new thermostat.
     * @param name   The name of the new thermostat.
     */
    public void registerThermostat(int roomID, String RFID, String name) {

        HttpPost request = null;
        URI uri;
        try {
            uri = new URI("");

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("rfid", RFID));
            params.add(new BasicNameValuePair("name", name));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.ADD_THERMOSTAT, new SmartheatingDbHelper(mContext), roomID);
        worker.execute(request);
    }

    /**
     * Add a {@link ScheduleEntry} to a room on the server.
     *
     * @param entry  The {@link ScheduleEntry} to be added.
     * @param roomID The local ID of the room the schedule is for.
     * @param RFID   The RFID of the thermostat the schedule is for on the server.
     */
    public void addScheduleEntry(ScheduleEntry entry, int roomID, String RFID) {
        HttpPost request = null;
        URI uri;
        try {
            uri = new URI("");

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("day", String.valueOf(((entry.getDay() + 6) % 7))));
            params.add(new BasicNameValuePair("time", entry.getStartTime() + ":00"));
            params.add(new BasicNameValuePair("temperature", String.valueOf(entry.getTemperature())));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.ADD_SCHEDULE, new SmartheatingDbHelper(mContext), roomID, RFID);
        worker.execute(request);
    }

    /**
     * Delete a room on the server.
     *
     * @param roomID The id of the room on the server.
     */
    public void deleteRoom(int roomID) {
        HttpDelete request = null;
        URI uri;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/" + Utility.RESIDENCE_RFID + "/room/" + roomID + "/", null, null);
            request = new HttpDelete(uri);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.DELETE_ROOM);
        worker.execute(request);
    }

    /**
     * Delete a thermostat on the server.
     *
     * @param roomID The id of the room on the server the thermostat to be deleted belongs to.
     * @param RFID   The RFID of the thermostat to be deleted.
     */
    public void deleteThermostat(int roomID, String RFID) {
        HttpDelete request = null;
        URI uri;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/" + Utility.RESIDENCE_RFID + "/room/" + roomID + "/thermostat/" + RFID + "/", null, null);
            request = new HttpDelete(uri);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.DELETE_THERMOSTAT);
        worker.execute(request);
    }

    /**
     * Update all the rooms, thermostats, schedules and temperatures of the local database with
     * the most recent data from the server.
     * <p>
     * This feature sets up an {@link AsyncWorker} which will call {@code updateThermostats},
     * {@code updateSchedule} and {@code updateTemperatures} for all the rooms on the server and
     * {@code deleteRoom} and {@code deleteThermostat} for the rooms which are removed on the server.
     */
    public void updateAll() {
        HttpGet request = null;
        URI uri;

        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/" + Utility.RESIDENCE_RFID + "/room/", null, null);
            Log.d(logTag, "Update started for " + uri);
            request = new HttpGet(uri);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.UPDATE_ROOMS, new SmartheatingDbHelper(mContext), this);
        worker.execute(request);
    }

    /**
     * Update all the thermostats in a single room.
     * <p>
     * This function gets called by an {@link AsyncWorker}.
     *
     * @param thermostat_URIs A {@link List} of {@link URI}s of the thermostats.
     */
    public void updateThermostats(List<URI> thermostat_URIs) {
        HttpGet request;

        for (URI nextURI : thermostat_URIs) {
            request = new HttpGet(nextURI);
            Log.d(logTag, "Updating Thermostats for " + nextURI);
            // execute request via AsyncTask
            AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.UPDATE_THERMOSTATS, new SmartheatingDbHelper(mContext), this);
            worker.execute(request);
        }
    }

    /**
     * Update the schedule for a single room. The server has a schedule for each thermostat. The
     * first thermostat's schedule is taken and used for the whole room.
     * <p>
     * This function gets called by an {@link AsyncWorker}.
     *
     * @param uri       The {@link URI} of the schedule.
     * @param server_id The server id of the room.
     */
    public void updateSchedule(URI uri, int server_id) {
        HttpGet request = new HttpGet(uri);
        // execute request via AsyncTask
        Log.d(logTag, "Updating Schedule for " + uri);
        AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.UPDATE_SCHEDULE, new SmartheatingDbHelper(mContext), server_id);
        worker.execute(request);
    }

    /**
     * Update the Temperatures for all thermostats.
     * <p>
     * This function gets called by an {@link AsyncWorker}.
     *
     * @param temperature_URIs A {@link List} of {@link URI}s of the most recent temperature.
     */
    public void updateTemperatures(List<URI> temperature_URIs) {
        HttpGet request;
        for (URI nextURI : temperature_URIs) {
            request = new HttpGet(nextURI);
            Log.d(logTag, "Updating Temperatures for " + nextURI);
            // execute request via AsyncTask
            AsyncWorker worker = new AsyncWorker(AsyncWorker.Type.UPDATE_TEMPERATURES, mDbHelper);
            worker.execute(request);
        }
    }
}
