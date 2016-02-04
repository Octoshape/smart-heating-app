package ch.ethz.smartheating.networking;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import ch.ethz.smartheating.database.SmartheatingDbHelper;
import ch.ethz.smartheating.model.ScheduleEntry;
import ch.ethz.smartheating.utilities.Utility;
import ch.ethz.smartheating.networking.AsyncWorker.Type;

/**
 * The {@link AsyncTask} class for all the background tasks of the application, distinguished by the
 * enum {@link Type}.
 */
public class AsyncWorker extends AsyncTask<HttpRequestBase, Void, String> {

    private final String logTag = "AsyncWorker";
    private final int room_id;
    private final String RFID;
    private final SmartheatingDbHelper mDbHelper;
    private final Type type;
    private Request mRequest;

    public enum Type {
        REGISTER_RESIDENCE,
        REGISTER_USER,
        ADD_ROOM,
        ADD_THERMOSTAT,
        ADD_SCHEDULE,
        UPDATE_ROOMS,
        UPDATE_THERMOSTATS,
        UPDATE_SCHEDULE,
        UPDATE_TEMPERATURES,
        DELETE_ROOM,
        DELETE_THERMOSTAT,
        DELETE_SCHEDULE_ENTRY
    }

    /**
     * Constructor for type: ADD_SCHEDULE
     *
     * @param type     The {@link Type} of the {@link AsyncWorker}.
     * @param dbHelper The {@link SmartheatingDbHelper} for database interactions.
     * @param id       The id of the room.
     * @param RFID     The RFID of the thermostat for adding schedule entries.
     */
    public AsyncWorker(Type type, SmartheatingDbHelper dbHelper, int id, String RFID) {
        this.type = type;
        this.RFID = RFID;
        this.room_id = id;
        this.mDbHelper = dbHelper;
    }

    /**
     * Constructor for types: DELETE_ROOM, DELETE_THERMOSTAT, REGISTER_RESIDENCE and REGISTER_USER
     *
     * @param type The {@link Type} of the {@link AsyncWorker}.
     */
    public AsyncWorker(Type type) {
        this(type, null, 0, null);
    }

    /**
     * Constructor for types: ADD_THERMOSTAT, ADD_ROOM and UPDATE_SCHEDULE
     *
     * @param type     The {@link Type} of the {@link AsyncWorker}.
     * @param dbHelper The {@link SmartheatingDbHelper} for database interactions.
     * @param id       The local id of the room (for updating its server id).
     */
    public AsyncWorker(Type type, SmartheatingDbHelper dbHelper, int id) {
        this(type, dbHelper, id, null);
    }

    /**
     * Constructor for types: UPDATE_ROOMS and UPDATE_THERMOSTATS
     *
     * @param type     The {@link Type} of the {@link AsyncWorker}.
     * @param dbHelper The {@link SmartheatingDbHelper} for database interactions.
     * @param r        The {@link Request} object for making further queries to the server.
     */
    public AsyncWorker(Type type, SmartheatingDbHelper dbHelper, Request r) {
        this(type, dbHelper, 0, null);
        mRequest = r;
    }

    /**
     * Constructor for type: UPDATE_TEMPERATURES
     *
     * @param type     The {@link Type} of the {@link AsyncWorker}.
     * @param dbHelper The {@link SmartheatingDbHelper} for database interactions.
     */
    public AsyncWorker(Type type, SmartheatingDbHelper dbHelper) {
        this(type, dbHelper, 0, null);
    }

    @Override
    protected String doInBackground(HttpRequestBase... params) {
        if (!Utility.isCurrentlyOnline()) {
            return Utility.NO_INTERNET;
        }

        HttpClient client = new DefaultHttpClient();
        String response = "";

        switch (type) {
            case ADD_THERMOSTAT:
            case ADD_SCHEDULE:
                // For adding thermostats or scheduleEntries we need to have a valid server_id in our database.
                int server_id = -1;
                SQLiteDatabase db = mDbHelper.getReadableDatabase();
                while (server_id == -1) { // TODO: add shutdown criteria in case the server ID was never updated.
                    server_id = mDbHelper.getRoomServerID(db, room_id);
                }
                db.close();
                try {
                    if (type == Type.ADD_THERMOSTAT) {
                        params[0].setURI(new URI("http", null, "52.28.68.182", 8000, "/residence/" +
                                Utility.RESIDENCE_RFID + "/room/" + server_id +
                                "/thermostat/", null, null));
                    } else {
                        params[0].setURI(new URI("http", null, "52.28.68.182", 8000, "/residence/" +
                                Utility.RESIDENCE_RFID + "/room/" + server_id +
                                "/thermostat/" + RFID + "/heating_table/", null, null));
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }

        switch (type) {
            case DELETE_ROOM:
            case DELETE_THERMOSTAT:
                try {
                    HttpResponse httpResponse = client.execute(params[0]);
                    HttpEntity entity = httpResponse.getEntity();
                    if (entity == null) {
                        response = "Deleted " + ((type == Type.DELETE_ROOM) ? "Room" : "Thermostat") + " successfully.";
                    } else {
                        response = EntityUtils.toString(entity, "UTF-8");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                try {
                    HttpResponse httpResponse = client.execute(params[0]);
                    response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return response;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (result.equals(Utility.NO_INTERNET)) {
            Log.d(logTag, result);
            return;
        }

        SQLiteDatabase db;
        switch (type) {
            case REGISTER_RESIDENCE:
                Log.d(logTag, "Residence registered: " + result);
                break;
            case REGISTER_USER:
                Log.d(logTag, "User registered: " + result);
                break;
            case ADD_ROOM:
                db = mDbHelper.getWritableDatabase();
                try {
                    JSONObject newRoom = new JSONObject(result);
                    mDbHelper.updateRoomServerID(db, room_id, newRoom.getInt("id"));
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }

                Log.d(logTag, "Room added: " + result);
                break;
            case ADD_THERMOSTAT:
                Log.d(logTag, "Thermostat added: " + result);
                break;
            case ADD_SCHEDULE:
                Log.d(logTag, "ScheduleEntry added: " + result);
                break;
            case UPDATE_ROOMS:
                db = mDbHelper.getWritableDatabase();
                try {
                    JSONArray allRooms = new JSONArray(result);
                    ArrayList<Integer> server_ids = new ArrayList<>();
                    ArrayList<String> names = new ArrayList<>();
                    ArrayList<URI> thermostat_URIs = new ArrayList<>();
                    for (int i = 0; i < allRooms.length(); i++) {
                        JSONObject nextRoom = allRooms.getJSONObject(i);
                        server_ids.add(nextRoom.getInt("id"));
                        names.add(nextRoom.getString("name"));
                        thermostat_URIs.add(new URI(nextRoom.getString("thermostats_url")));
                    }
                    Log.d(logTag, "Updating rooms: " + names.toString());
                    mDbHelper.updateRooms(db, server_ids, names);
                    mRequest.updateThermostats(thermostat_URIs);
                } catch (JSONException | URISyntaxException e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
                break;
            case UPDATE_THERMOSTATS:
                int server_id = -1;
                db = mDbHelper.getWritableDatabase();
                try {
                    JSONArray allThermostats = new JSONArray(result);
                    ArrayList<String> names = new ArrayList<>();
                    ArrayList<String> RFIDs = new ArrayList<>();
                    ArrayList<URI> temperature_URIs = new ArrayList<>();
                    URI heating_schedule_URI = null;
                    for (int i = 0; i < allThermostats.length(); i++) {
                        JSONObject nextThermostat = allThermostats.getJSONObject(i);
                        server_id = nextThermostat.getJSONObject("room").getInt("id");
                        RFIDs.add(nextThermostat.getString("rfid"));
                        names.add(nextThermostat.getString("name"));
                        // We only have 1 Schedule per room, so we only read the last thermostat's heating_table_url
                        heating_schedule_URI = new URI(nextThermostat.getString("heating_table_url"));
                        temperature_URIs.add(new URI(nextThermostat.getString("temperatures_url") + "latest/"));
                    }

                    if (allThermostats.length() == 0) {
                        Log.d(logTag, "No Thermostats found.");
                    } else {
                        Log.d(logTag, "Updating Thermostats for room " + server_id + ": " + names.toString());
                        mDbHelper.updateThermostats(db, server_id, RFIDs, names);
                        mRequest.updateSchedule(heating_schedule_URI, server_id);
                        mRequest.updateTemperatures(temperature_URIs);
                    }
                } catch (JSONException | URISyntaxException e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
                break;
            case UPDATE_SCHEDULE:
                db = mDbHelper.getWritableDatabase();
                try {
                    JSONArray allEntries = new JSONArray(result);
                    ArrayList<ScheduleEntry> entries = new ArrayList<>();
                    if (allEntries.length() > 0) {
                        JSONObject nextEntry = allEntries.getJSONObject(0);
                        for (int i = 1; i < allEntries.length(); i++) {
                            double temp = nextEntry.getDouble("temperature");
                            int day = nextEntry.getInt("day");
                            int start = Integer.valueOf(nextEntry.getString("time").substring(0, 2));
                            nextEntry = allEntries.getJSONObject(i);
                            int end = Integer.valueOf(nextEntry.getString("time").substring(0, 2));
                            entries.add(new ScheduleEntry(temp, start, end, day + 1));
                        }
                        mDbHelper.updateSchedule(db, entries, room_id);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
                break;
            case UPDATE_TEMPERATURES:
                db = mDbHelper.getWritableDatabase();
                try {
                    JSONObject temperatureEntry = new JSONObject(result);
                    if (temperatureEntry.has("value")) {
                        double temperature = temperatureEntry.getDouble("value");
                        String thermostatRFID = temperatureEntry.getJSONObject("thermostat").getString("url").split("/")[8];
                        mDbHelper.updateTemperature(db, thermostatRFID, temperature);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    db.close();
                }
                break;
            default:
                Log.d(logTag, result);
        }
    }
}
