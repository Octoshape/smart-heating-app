package ch.ethz.smartheating.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import ch.ethz.smartheating.R;
import ch.ethz.smartheating.database.SmartheatingContract;
import ch.ethz.smartheating.database.SmartheatingDbHelper;
import ch.ethz.smartheating.networking.Request;
import ch.ethz.smartheating.model.ScheduleEntry;

/***
 * This is the Utility class with all of the applications static values and static helper functions.
 */
public class Utility {

    public static final float NO_HEATING_TEMPERATURE = 5.0f;
    public static float LOWEST_TEMPERATURE = 12.0f;
    public static float HIGHEST_TEMPERATURE = 30.0f;
    public static float DEFAULT_TEMPERATURE = 22.0f;
    public static float SLEEPING_TEMPERATURE = 16.0f;
    public static int TEMPERATURE_STEPS = (int) (HIGHEST_TEMPERATURE * 2 - LOWEST_TEMPERATURE * 2);
    private static final int CONNECTIVITY_CHECK_INTERVAL = 5000;
    public static boolean NFC_ADAPTER_AVAILABLE = false;
    private static final int UPDATE_INTERVAL = 900000; // Update every 15 minutes.
    public static Date LATEST_UPDATE;

    public static NfcAdapter adapter;
    public static String RESIDENCE_RFID;
    public static final ArrayList<ScheduleEntry> DEFAULT_HEATING_SCHEDULE = new ArrayList<>();
    public final static String PREFERENCES = "smartHeatingPrefs";
    public final static String NO_INTERNET = "No internet connection available";

    private static final Handler mHandler = new Handler();
    private static Request mRequest;

    private static boolean mIsCurrentlyOnline = true;
    private static Context mContext;

    /**
     * @return Whether the application has a working internet connection or not.
     */
    public static boolean isCurrentlyOnline() {
        return mIsCurrentlyOnline;
    }

    /**
     * Return a color-int from red, green, blue components for a given temperature.
     *
     * @param temp The temperature.
     * @return The color-int.
     */
    public static int getColorForTemperature(double temp) {
        double vmin = LOWEST_TEMPERATURE;
        double vmax = HIGHEST_TEMPERATURE;
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

    /**
     * Extract the RFID of a given {@link Parcelable} raw message from the NFC tags of the system.
     *
     * @param rawMsgs The raw message in {@link Parcelable} form.
     * @return The RFID.
     */
    public static String extractRFID(Parcelable[] rawMsgs) {
        NdefMessage[] messages;

        if (rawMsgs != null) {
            messages = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                messages[i] = (NdefMessage) rawMsgs[i];
            }
            return new String(messages[0].getRecords()[0].getPayload()).trim().substring(10);
        }

        return null;
    }

    /**
     * Set up the foreground dispatch for the NFC tags of the system. This allows the user to be able to scan a tag without having to
     * reopen the application.
     *
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity) {
        if (!NFC_ADAPTER_AVAILABLE) {
            return;
        }

        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Note that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        filters[0].addCategory(Intent.CATEGORY_BROWSABLE);
        filters[0].addDataScheme("http");
        filters[0].addDataAuthority("www.ttag.be", null);

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * Stop the foreground dispatch for NFC tags.
     *
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity) {
        if (!NFC_ADAPTER_AVAILABLE) {
            return;
        }
        adapter.disableForegroundDispatch(activity);
    }

    /**
     * Create rooms with random names and a random amount of thermostats (with random temperatures) in them
     * and adds them to the database and the server.
     *
     * @param nOfRooms       The number of rooms to be created. (Max: 10)
     * @param nOfThermostats The maximum number of thermostats to be added per room (ranges from 1 to nOThermostats)
     */
    public static void createDummyHouse(Context context, int nOfRooms, int nOfThermostats) {

        SmartheatingDbHelper dbHelper = new SmartheatingDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Random r = new Random();
        Request request = new Request(context);
        ArrayList<String> dummyNames = new ArrayList<>(Arrays.asList("Küche", "Schlafzimmer", "Kinderzimmer", "Wohnzimmer", "Esszimmer", "Wäscheraum", "Bad", "Großes Bad", "Büro", "Kitchen", "Bedroom1", "Bedroom2", "Bathroom1", "Bathroom2", "Living Room", "Entry hall", "Laundry room", "Office"));
        ArrayList<String> dummyThermostatNames = new ArrayList<>(Arrays.asList("Fenster", "Neben Bett", "Raum-Mitte", "Eingang", "next to bed", "window", "big one", "small one", "left", "right", "links", "rechts"));

        for (int i = 0; i < nOfRooms; i++) {
            int room = r.nextInt(dummyNames.size());

            ContentValues values = new ContentValues();

            values.put(SmartheatingContract.Rooms.COLUMN_NAME_NAME, dummyNames.get(room));
            values.put(SmartheatingContract.Rooms.COLUMN_NAME_TEMPERATURE, 0);
            values.put(SmartheatingContract.Rooms.COLUMN_NAME_SERVER_ID, -1);

            int room_id = (int) db.insert(SmartheatingContract.Rooms.TABLE_NAME, null, values);
            request.registerRoom(dummyNames.get(room), room_id);
            dummyNames.remove(room);


            int next = r.nextInt(nOfThermostats) + 1;
            for (int j = 0; j < next; j++) {
                values = new ContentValues();

                double temperature = r.nextInt(13) + 13;

                String RFID = String.valueOf(r.nextInt(Integer.MAX_VALUE));
                String name = dummyThermostatNames.get(r.nextInt(dummyThermostatNames.size()));

                values.put(SmartheatingContract.Thermostats.COLUMN_NAME_ROOM_ID, room_id);
                values.put(SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE, temperature);
                values.put(SmartheatingContract.Thermostats.COLUMN_NAME_RFID, RFID);
                values.put(SmartheatingContract.Thermostats.COLUMN_NAME_NAME, name);

                db.insert(SmartheatingContract.Thermostats.TABLE_NAME, null, values);
                request.registerThermostat(room_id, RFID, name);

                for (ScheduleEntry entry : DEFAULT_HEATING_SCHEDULE) {
                    request.addScheduleEntry(entry, room_id, RFID);
                }
            }

            // Setup default heating schedule.
            dbHelper.resetSchedule(db, room_id);
        }
        dbHelper.updateAllRoomTemps(db);
        db.close();
    }

    /**
     * Check for available internet connection and disables all communcation with the server if there is none.
     */
    private static final Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            mIsCurrentlyOnline = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
            updateActionBar();
            mHandler.postDelayed(this, CONNECTIVITY_CHECK_INTERVAL);
        }
    };

    /**
     * Start the continuous check if there is an internet connection available. Changes the color of
     * the to red if there is not.
     *
     * @param context The {@link Context} the user is currently in.
     */
    public static void startConnectivityCheck(final Context context) {
        mContext = context;
        updateActionBar();
        mHandler.postDelayed(mStatusChecker, 0);
    }

    /**
     * Update the {@link android.support.v7.app.ActionBar} of the current {@link Activity} depending
     * on the internet connection available.
     * Showing the latest update time in case of a disconnect and changing its color accordingly.
     * Tapping the title with a disconnected device triggers a popup explaining the current state.
     */
    private static void updateActionBar() {
        ActionBarActivity actionBarActivity = (ActionBarActivity) mContext;
        ActionBar actionBar = actionBarActivity.getSupportActionBar();
        if (mIsCurrentlyOnline) {
            actionBar.setDisplayShowCustomEnabled(false);
            actionBar.setBackgroundDrawable(new ColorDrawable(mContext.getResources().getColor(R.color.background_material_dark)));
        } else {
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.RED));
            actionBar.setCustomView(R.layout.actionbar_no_connection);
            actionBar.setDisplayShowCustomEnabled(true);
            ImageView imageView = (ImageView) actionBar.getCustomView().findViewById(R.id.alert_icon);
            imageView.setImageResource(android.R.drawable.ic_dialog_info);
            actionBar.getCustomView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDisconnectedPopup();
                }
            });
        }
    }

    /**
     * Used for when no internet connection is available. The system cannot make any changes during
     * that time. So whenever the user is trying to change something, this popup will be shown instead.
     */
    public static void showDisconnectedPopup() {
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(mContext.getResources().getString(R.string.no_internet))
                .setMessage(String.format(mContext.getResources().getString(R.string.connection_required), new SimpleDateFormat("HH:mm dd.MM.yyyy").format(Utility.LATEST_UPDATE)))
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Stop the continuous check if there is an internet connection available. Cancels any postDelayed calls to {@code mStatusChecker}.
     */
    public static void stopConnectivityCheck() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    /**
     * The continuous {@link Runnable} for updating the local database with the one from the server.
     */
    private static final Runnable mDatabaseUpdater = new Runnable() {
        @Override
        public void run() {
            mRequest.updateAll();
            mHandler.postDelayed(this, UPDATE_INTERVAL);
            if (mIsCurrentlyOnline) {
                SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit();
                editor.putLong("latest_update", System.currentTimeMillis());
                LATEST_UPDATE = new Date();
                editor.commit();
            }
        }
    };

    /**
     * Start the continuous database updates in a given {@code UPDATE_INTERVAL}.
     *
     * @param r the Request update to execute the updates.
     */
    public static void startUpdates(Request r) {
        mRequest = r;
        mHandler.postDelayed(mDatabaseUpdater, UPDATE_INTERVAL);
    }

    /**
     * Reset the system so the user can register a new residence.
     *
     * @param context The {@link Context} the user is currently in.
     */
    public static void reset(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit();
        SmartheatingDbHelper helper = new SmartheatingDbHelper(context);
        helper.resetDatabase();
        editor.putBoolean("registered", false);
        editor.commit();
    }

    /**
     * Update the temperatures from the {@link SharedPreferences}.
     *
     * @param prefs The {@link SharedPreferences} from the application.
     */
    public static void updateTemps(SharedPreferences prefs) {
        LOWEST_TEMPERATURE = prefs.getInt("lowest_temp", 12);
        HIGHEST_TEMPERATURE = prefs.getInt("highest_temp", 30);
        SLEEPING_TEMPERATURE = prefs.getInt("sleeping_temp", 16);
        DEFAULT_TEMPERATURE = prefs.getInt("default_temp", 22);
    }
}
