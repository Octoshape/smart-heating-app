package ch.ethz.smartheating.utilities;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.smartheating.database.SmartheatingContract.Rooms;
import ch.ethz.smartheating.database.SmartheatingDbHelper;
import ch.ethz.smartheating.schedule.ScheduleEntry;

/**
 * Created by schmisam on 19/05/15.
 */
public class Request {

    private static final Boolean DEBUG = false;
    private static final String logTag = "Request.java";
    private SmartheatingDbHelper mDbHelper = null;

    public Request(Context context) {
        mDbHelper = new SmartheatingDbHelper(context);
    }

    public void registerResidence () {

        HttpPost request = null;
        URI uri = null;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/", null, null);

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("rfid", Utility.RESIDENCE_RFID));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker();
        worker.execute(request);
    }

    public void registerUser (String IMEI) {
        HttpPost request = null;
        URI uri = null;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/" + Utility.RESIDENCE_RFID + "/user/", null, null);

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("imei", IMEI));
            params.add(new BasicNameValuePair("name", "UserName"));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker();
        worker.execute(request);
    }

    public void registerRoom (String name, int id) {
        HttpPost request = null;
        URI uri = null;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/" + Utility.RESIDENCE_RFID + "/room/", null, null);

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("name", name));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        NewRoomWorker worker = new NewRoomWorker(id);
        worker.execute(request);
    }

    public void registerThermostat (int roomID, String RFID) {

        Log.d(logTag, "RFID:" + RFID);
        HttpPost request = null;
        URI uri = null;
        try {
            uri = new URI("");

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("rfid", RFID));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        NewThermostatWorker worker = new NewThermostatWorker(roomID);
        worker.execute(request);
    }

    public void addScheduleEntry (ScheduleEntry entry, int roomID, String RFID) {
        HttpPost request = null;
        URI uri = null;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/" + Utility.RESIDENCE_RFID + "/room/" + roomID + "/thermostat/" + RFID + "/heating_table/", null, null);

            request = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("day", String.valueOf((entry.getDay() + 5 % 7))));
            params.add(new BasicNameValuePair("time", entry.getStartTime() + ":00"));
            params.add(new BasicNameValuePair("temperature", String.valueOf(entry.getTemperature())));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker();
        worker.execute(request);
    }

    public class NewThermostatWorker extends AsyncTask<HttpRequestBase, Void, String> {

        private int room_id;

        public NewThermostatWorker (int id) {
            room_id = id;
        }

        @Override
        protected String doInBackground(HttpRequestBase... params) {
            // use library to create and send request
            HttpClient client = new DefaultHttpClient();

            int server_id = -1;
            while (server_id == -1) {
                Cursor c = mDbHelper.getReadableDatabase().rawQuery("SELECT " + Rooms.COLUMN_NAME_SERVER_ID +
                        " FROM " + Rooms.TABLE_NAME +
                        " WHERE " + Rooms._ID +
                        " = " + room_id, null);

                c.moveToFirst();
                server_id = c.getInt(0);
                c.close();
            }

            try {
                params[0].setURI(new URI("http", null, "52.28.68.182", 8000, "/residence/" +
                        Utility.RESIDENCE_RFID + "/room/" + server_id +
                        "/thermostat/", null, null));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            String response = "";

            try {
                HttpResponse httpResponse = client.execute(params[0]);
                response = EntityUtils.toString(httpResponse.getEntity());

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // return response
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(logTag, "Thermostat added: " + result);
        }
    }

    public class NewRoomWorker extends AsyncTask<HttpRequestBase, Void, String> {

        private int room_id;

        public NewRoomWorker (int id) {
            room_id = id;
        }

        @Override
        protected String doInBackground(HttpRequestBase... params) {
            // use library to create and send request
            HttpClient client = new DefaultHttpClient();

            String response = "";

            try {
                HttpResponse httpResponse = client.execute(params[0]);
                response = EntityUtils.toString(httpResponse.getEntity());

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // return response
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                JSONObject newRoom = new JSONObject(result);

                mDbHelper.updateRoomServerID(mDbHelper.getWritableDatabase(), room_id, newRoom.getInt("id"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(logTag, "Room added: " + result);
        }
    }

    public class AsyncWorker extends AsyncTask<HttpRequestBase, Void, String> {

        @Override
        protected String doInBackground(HttpRequestBase... params) {
            // use library to create and send request
            HttpClient client = new DefaultHttpClient();

            String response = "";

            try {
                HttpResponse httpResponse = client.execute(params[0]);
                response = EntityUtils.toString(httpResponse.getEntity());

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // return response
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            JSONTokener tokener = new JSONTokener(result);

            String firstChar = String.valueOf(result.charAt(0));

            JSONArray array;
            JSONObject object;

            try {
                if (firstChar.equals("[")) {
                    array = new JSONArray(tokener);
                } else {
                    object = new JSONObject(tokener);
                }

            } catch (JSONException e) {
                System.out.println("JSON parse problem");
                e.printStackTrace();
            }

            Log.d(logTag, result);
        }
    }
}
