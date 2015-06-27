package ch.ethz.smartheating.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

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

        SQLiteDatabase db = new SmartheatingDbHelper(context).getWritableDatabase();

        db.execSQL(query);

        db.close();

        return null;
    }
}
