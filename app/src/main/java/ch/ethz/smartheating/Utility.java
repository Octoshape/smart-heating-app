package ch.ethz.smartheating;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import ch.ethz.smartheating.db.smartHeatingContract;
import ch.ethz.smartheating.db.smartHeatingDbHelper;

/**
 * Created by schmisam on 21/05/15.
 */
public class Utility {

    public static NfcAdapter adapter;
    public static String RESIDENCE_RFID;

    public static int getColorForTemperature(double temp) {
        double vmin = 10.0;
        double vmax = 30.0;
        Double r = 1.0, g = 1.0, b = 1.0;
        double dv;

        if (temp < vmin)
            temp = vmin;
        if (temp > vmax)
            temp = vmax;
        dv = vmax - vmin;

        if (temp < (vmin + 0.25 * dv)) {
            r = 0d;
            g = 4 * (temp - vmin) / dv;
        } else if (temp < (vmin + 0.5 * dv)) {
            r = 0d;
            b = 1 + 4 * (vmin + 0.25 * dv - temp) / dv;
        } else if (temp < (vmin + 0.75 * dv)) {
            r = 4 * (temp - vmin - 0.5 * dv) / dv;
            b = 0d;
        } else {
            g = 1 + 4 * (vmin + 0.75 * dv - temp) / dv;
            b = 0d;
        }

        r *= 255;
        g *= 255;
        b *= 255;

        return Color.rgb(r.intValue(), g.intValue(), b.intValue());
    }

    public static String extractRFID(Parcelable[] rawMsgs) {
        NdefMessage[] messages = null;

        if (rawMsgs != null) {
            messages = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                messages[i] = (NdefMessage) rawMsgs[i];
            }
        }

        return new String(messages[0].getRecords()[0].getPayload()).trim().substring(10);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        filters[0].addCategory(Intent.CATEGORY_BROWSABLE);
        filters[0].addDataScheme("http");
        filters[0].addDataAuthority("www.ttag.be", null);

        //adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity) {
        //adapter.disableForegroundDispatch(activity);
    }

    /**
     * Creates rooms with random names and a random amount of thermostats (with random temperatures) in them
     * and adds them to the database and the server.
     *
     * @param nOfRooms      The number of rooms to be created. (Max: 10)
     * @param nOfThermostats The maximum number of thermostats to be added per room (ranges from 1 to nOThermostats)
     */
    public static void createDummyHouse(Context context, int nOfRooms, int nOfThermostats) {

        smartHeatingDbHelper dbHelper = new smartHeatingDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Random r = new Random();
        Request request = new Request(context);
        ArrayList<String> dummyNames = new ArrayList<String>(Arrays.asList("Küche", "Schlafzimmer", "Kinderzimmer", "Wohnzimmer", "Esszimmer", "Wäscheraum", "Bad", "Großes Bad", "Büro", "Kitchen", "Bedroom1", "Bedroom2", "Bathroom1", "Bathroom2", "Living Room", "Entry hall", "Laundry room", "Office"));

        for (int i = 0; i < nOfRooms; i++) {
            int room = r.nextInt(dummyNames.size());

            ContentValues values = new ContentValues();

            values.put(smartHeatingContract.Rooms.COLUMN_NAME_NAME, dummyNames.get(room));
            values.put(smartHeatingContract.Rooms.COLUMN_NAME_TEMPERATURE, 0);
            values.put(smartHeatingContract.Rooms.COLUMN_NAME_SERVER_ID, -1);

            int room_id = (int)db.insert(smartHeatingContract.Rooms.TABLE_NAME, null, values);
            request.registerRoom(dummyNames.get(room), room_id);
            dummyNames.remove(room);

            int next = r.nextInt(nOfThermostats) + 1;
            for (int j = 0; j < next; j++) {
                values = new ContentValues();

                double temperature = r.nextInt(13) + 13;

                values.put(smartHeatingContract.Thermostats.COLUMN_NAME_ROOM_ID, room_id);
                values.put(smartHeatingContract.Thermostats.COLUMN_NAME_TEMPERATURE, temperature);
                values.put(smartHeatingContract.Thermostats.COLUMN_NAME_RFID, j);

                db.insert(smartHeatingContract.Thermostats.TABLE_NAME, null, values);
                request.registerThermostat(room_id, String.valueOf(r.nextInt(Integer.MAX_VALUE)));
            }
        }
        dbHelper.updateAllRoomTemps(db);
        db.close();
    }
}
