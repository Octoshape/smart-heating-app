package ch.ethz.smartheating.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.IOException;

/**
 * Created by schmisam on 25/05/15.
 */

/**
 * DB Worker class doing SQL Queries in the background.
 *
 */
public class DbWorker extends AsyncTask<Object, String, Void> {

    @Override
    protected Void doInBackground(Object... params) {

        Context context = (Context) params[0];
        boolean doesWrite = (boolean) params[1];
        String query = (String) params[2];

        SQLiteDatabase db = new smartHeatingDbHelper(context).getWritableDatabase();

        db.execSQL(query);

        db.close();

        return null;
    }
}
