package ch.ethz.smartheating.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import ch.ethz.smartheating.R;
import ch.ethz.smartheating.database.SmartheatingContract.Thermostats;
import ch.ethz.smartheating.database.SmartheatingContract.Schedules;
import ch.ethz.smartheating.database.SmartheatingContract.Rooms;
import ch.ethz.smartheating.database.SmartheatingDbHelper;
import ch.ethz.smartheating.networking.Request;
import ch.ethz.smartheating.model.ScheduleEntry;
import ch.ethz.smartheating.utilities.Utility;

/**
 * The WelcomeAcitivity is shown when the app is opened for the first time. It contains introductional
 * text telling the user how to setup the heating system. The user can start the setup by scanning the
 * tag on the raspberry pi (if an NFC adapter is available) or by simply typing in the RFID on the pi
 * manually.
 * <p>
 * The user is then presented with some setup questions before he gets transferred to the {@link HomeActivity}.
 */
public class WelcomeActivity extends ActionBarActivity implements TimePickerDialog.OnTimeSetListener {

    private View mQuestionnaireView;
    private int mQuestionnaireProgress;
    private SharedPreferences mSharedPreferences;
    private ArrayList<Integer> mQuestionnaireAnswers;
    private final SmartheatingDbHelper mDbHelper = new SmartheatingDbHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup
        mSharedPreferences = this.getSharedPreferences(Utility.PREFERENCES, Context.MODE_PRIVATE);
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Utility.adapter = nfcAdapter;
        Utility.NFC_ADAPTER_AVAILABLE = nfcAdapter != null;
        Utility.updateTemps(mSharedPreferences);
        Utility.LATEST_UPDATE = new Date(mSharedPreferences.getLong("latest_update", System.currentTimeMillis()));

        if (mSharedPreferences.getBoolean("registered", false)) {
            Utility.RESIDENCE_RFID = mSharedPreferences.getString("residence_rfid", "");
            Intent homeIntent = new Intent(this, HomeActivity.class);
            startActivity(homeIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);


        TextView mWelcomeTextView = (TextView) findViewById(R.id.welcomeTextView);
        mWelcomeTextView.setText(R.string.welcome_text);
        ImageView imageView = (ImageView) findViewById(R.id.scanImageView);
        imageView.setImageDrawable(getResources().getDrawable(R.drawable.raspi_scan));

        mQuestionnaireAnswers = new ArrayList<>(4);
        mQuestionnaireAnswers.add(7);
        mQuestionnaireAnswers.add(8);
        mQuestionnaireAnswers.add(16);
        mQuestionnaireAnswers.add(22);
    }

    /**
     * Set up the foreground dispatch to prevent NFC tag scans belonging to the system to open
     * the application a second time.
     * <p>
     * Initiate the constantly running check for a working internet connection. This has to be done
     * in every Activity anew because the check, once failing, needs to change the currently shown
     * action bar.
     */
    @Override
    public void onResume() {
        super.onResume();
        Utility.setupForegroundDispatch(this);
        Utility.startConnectivityCheck(this);
    }

    /**
     * Disable the foreground dispatch for this activity, the next one will handle NFC tag scans itself.
     * <p>
     * Cancel the connectivity check for this activity, the next one will handle connectivity checks
     * itself.
     */
    @Override
    public void onPause() {
        Utility.stopForegroundDispatch(this);
        Utility.stopConnectivityCheck();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_register_dummy_residence:
                registerDummyResidence();
                return true;
            case R.id.action_manual_tag_entry:
                showManualTagPopup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Used for testing the application or maybe a demo mode for the user itself. The application
     * continues as if the user had registered a regular Raspberry Pi but creates 10 random rooms
     * with up to 3 thermostats so the user can see how things work.
     * <p>
     * This method only works if the application has a working internet connection, otherwise a
     * popup is shown.
     */
    private void registerDummyResidence() {
        if (!Utility.isCurrentlyOnline()) {
            Utility.showDisconnectedPopup();
            return;
        }

        Random r = new Random();
        Utility.RESIDENCE_RFID = String.valueOf(r.nextInt(Integer.MAX_VALUE));
        new Request(this).registerResidence();
        new Request(this).registerUser(String.valueOf(r.nextInt(Integer.MAX_VALUE)));

        SharedPreferences.Editor editor = mSharedPreferences.edit();

        editor.putBoolean("add_dummy_house", true);
        editor.putString("residence_rfid", Utility.RESIDENCE_RFID);
        editor.putBoolean("registered", true);
        editor.commit();

        showQuestionnaire();
    }

    /**
     * Handle the NFC-tag scan intents. If a Raspberry Pi is scanned, proceed with registration and setup
     * questions.
     * <p>
     * This only works if the application has a working internet connection, otherwise a
     * popup is shown.
     *
     * @param intent The intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            if (!Utility.isCurrentlyOnline()) {
                Utility.showDisconnectedPopup();
                return;
            }

            Parcelable[] parcels = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            registerPi(Utility.extractRFID(parcels));

            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean("registered", true);
            editor.putString("residence_rfid", Utility.RESIDENCE_RFID);
            editor.commit();

            showQuestionnaire();
        }
    }

    /**
     * Register the scanned Raspberry Pi with the server and add the user to it, using its ANDROID_ID
     *
     * @param RFID The RFID of the Pi
     */
    private void registerPi(String RFID) {
        Utility.RESIDENCE_RFID = RFID;
        String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        Request mRequest = new Request(this);

        mRequest.registerResidence();
        mRequest.registerUser(android_id);
    }

    /**
     * Show a popup for the user to enter an RFID tag manually instead of scanning it.
     * <p>
     * This method only works if the application has a working internet connection, otherwise a
     * popup is shown.
     */
    private void showManualTagPopup() {
        if (!Utility.isCurrentlyOnline()) {
            Utility.showDisconnectedPopup();
            return;
        }
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Manual RFID tag entry")
                .setMessage("Please enter RFID number of the tag on the raspberry pi.")
                .setView(input)
                .setCancelable(false)

                        // Positive button onClickListener is overwritten later for input checking.
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })

                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String RFID = input.getText().toString().trim();
                // In case the RFID is invalid, keep the dialog open.
                if (RFID.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter an RFID.", Toast.LENGTH_SHORT).show();
                } else {
                    dialog.dismiss();
                    registerPi(RFID);
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean("registered", true);
                    editor.putString("residence_rfid", Utility.RESIDENCE_RFID);
                    editor.commit();

                    showQuestionnaire();
                }
            }
        });
    }

    /**
     * Show the initial setup questions after the raspberry Pi has been registered as a popup.
     * <p>
     * The user can use the next and previous buttons to switch through the questions.
     */
    private void showQuestionnaire() {
        AlertDialog.Builder newEntryDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Light_Dialog));
        LayoutInflater factory = LayoutInflater.from(this);
        mQuestionnaireView = factory.inflate(R.layout.questionnaire, null);
        mQuestionnaireProgress = 0;
        newEntryDialog.setTitle("Setup Questions");

        final TextView question = (TextView) mQuestionnaireView.findViewById(R.id.question);
        final TextView time = (TextView) mQuestionnaireView.findViewById(R.id.time);

        question.setText(R.string.questionnaire_wake);
        int selectedHour = mQuestionnaireAnswers.get(mQuestionnaireProgress);
        time.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");

        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog.newInstance(WelcomeActivity.this, 7, 0, true).show(getFragmentManager(), "timePicker");
            }
        });

        newEntryDialog.setView(mQuestionnaireView);

        newEntryDialog.setNegativeButton("Previous", null);
        newEntryDialog.setPositiveButton("Next", null);

        final AlertDialog newDialog = newEntryDialog.create();
        newDialog.show();
        newDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

        newDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuestionnaireProgress--;
                int selectedHour = 0;
                switch (mQuestionnaireProgress) {
                    case 0:
                        question.setText(R.string.questionnaire_wake);
                        selectedHour = mQuestionnaireAnswers.get(0);
                        time.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");
                        newDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                        break;
                    case 1:
                        question.setText(R.string.questionnaire_work);
                        selectedHour = mQuestionnaireAnswers.get(1);
                        time.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");
                        break;
                    case 2:
                        question.setText(R.string.questionnaire_home);
                        selectedHour = mQuestionnaireAnswers.get(2);
                        time.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");
                        newDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Next");
                        break;
                    default:
                        break;
                }
                final int initialHour = selectedHour;
                time.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TimePickerDialog.newInstance(WelcomeActivity.this, initialHour, 0, true).show(getFragmentManager(), "timePicker");
                    }
                });
            }
        });

        newDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuestionnaireProgress++;
                switch (mQuestionnaireProgress) {
                    case 1:
                        question.setText(R.string.questionnaire_work);
                        newDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                        int selectedHour = mQuestionnaireAnswers.get(1);
                        time.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");
                        break;
                    case 2:
                        question.setText(R.string.questionnaire_home);
                        selectedHour = mQuestionnaireAnswers.get(2);
                        time.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");
                        break;
                    case 3:
                        question.setText(R.string.questionnaire_sleep);
                        selectedHour = mQuestionnaireAnswers.get(3);
                        time.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");
                        newDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Finish");
                        break;
                    case 4:
                        // User clicked Finish button.
                        setupDefaultHeatingSchedule();

                        Intent homeIntent = new Intent(WelcomeActivity.this, HomeActivity.class);
                        startActivity(homeIntent);
                        finish();
                        return;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int i, int i1) {
        int selectedHour = i;

        final TextView time = (TextView) mQuestionnaireView.findViewById(R.id.time);

        time.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");

        mQuestionnaireAnswers.set(mQuestionnaireProgress, selectedHour);
    }

    /**
     * Set up the default heating schedule for all the rooms. This schedule will be added to the local
     * database only and for a specially marked default room. When creating new rooms, this schedule
     * will be added to the new rooms.
     * <p>
     * This speciall marked room could later be used when copying schedules fromm room to room is
     * implemented. For now, we simply store the default schedule in the Utility class.
     */
    private void setupDefaultHeatingSchedule() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(Rooms.COLUMN_NAME_NAME, "default");
        values.put(Rooms.COLUMN_NAME_TEMPERATURE, 0);
        values.put(Rooms.COLUMN_NAME_SERVER_ID, -1);
        values.put(Rooms._ID, Rooms.DEFAULT_ID);

        db.insert(Rooms.TABLE_NAME, null, values);

        values.clear();
        values.put(Thermostats.COLUMN_NAME_RFID, Thermostats.DEFAULT_RFID);
        values.put(Thermostats.COLUMN_NAME_ROOM_ID, Rooms.DEFAULT_ID);
        values.put(Thermostats.COLUMN_NAME_TEMPERATURE, -1);

        db.insert(Thermostats.TABLE_NAME, null, values);

        values.clear();
        values.put(Schedules.COLUMN_NAME_ROOM_ID, Rooms.DEFAULT_ID);

        int start = 0;
        int end = 0;
        double temp = 0;

        for (int i = 0; i <= mQuestionnaireAnswers.size(); i++) {
            switch (i) {
                case 0:
                    start = 0;
                    end = mQuestionnaireAnswers.get(0);
                    temp = Utility.SLEEPING_TEMPERATURE;
                    break;
                case 1:
                    start = end;
                    end = mQuestionnaireAnswers.get(1);
                    temp = Utility.DEFAULT_TEMPERATURE;
                    break;
                case 2:
                    start = end;
                    end = mQuestionnaireAnswers.get(2);
                    temp = Utility.NO_HEATING_TEMPERATURE;
                    break;
                case 3:
                    start = end;
                    end = mQuestionnaireAnswers.get(3);
                    temp = Utility.DEFAULT_TEMPERATURE;
                    break;
                case 4:
                    start = end;
                    end = 24;
                    temp = Utility.SLEEPING_TEMPERATURE;
                    break;
                default:
                    break;
            }

            values.put(Schedules.COLUMN_NAME_START_TIME, start);
            values.put(Schedules.COLUMN_NAME_END_TIME, end);
            values.put(Schedules.COLUMN_NAME_TEMPERATURE, temp);
            for (int day = 1; day <= 7; day++) {
                if (day > 5) {
                    // On weekends the schedule stays on default temperature throughout the whole day
                    if (i == 1) {
                        // Default temperature should last until the user goes to sleep
                        values.put(Schedules.COLUMN_NAME_END_TIME, mQuestionnaireAnswers.get(3));
                    } else if (i > 1 && i < mQuestionnaireAnswers.size()) {
                        // The schedule changes during the day are not needed, except for the last one.
                        continue;
                    }
                }
                values.put(Schedules.COLUMN_NAME_DAY, day);
                db.insert(Schedules.TABLE_NAME, null, values);
                ScheduleEntry nextEntry = new ScheduleEntry(temp, start, end, day);
                // Simply store in Utility. Copying schedules is not implemented yet.
                Utility.DEFAULT_HEATING_SCHEDULE.add(nextEntry);
            }
        }
        db.close();
    }
}


