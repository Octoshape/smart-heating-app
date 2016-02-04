package ch.ethz.smartheating.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.RectF;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.ethz.smartheating.R;
import ch.ethz.smartheating.database.SmartheatingContract.Schedules;
import ch.ethz.smartheating.database.SmartheatingDbHelper;
import ch.ethz.smartheating.model.ScheduleEntry;
import ch.ethz.smartheating.utilities.ScheduleView;
import ch.ethz.smartheating.networking.Request;
import ch.ethz.smartheating.utilities.Utility;
import ch.ethz.smartheating.utilities.VerticalSeekBar;

/**
 * The ScheduleActivity is in forced landscape mode to accomodate the width of the weekly schedule.
 * Here the user can see the heating schedule for the room he selected. The user can change the schedule
 * in two ways: Either by long clicking an existing schedule and modifying its values or by tapping
 * any time and thus adding a new {@link ScheduleEntry}.
 */
public class ScheduleActivity extends ActionBarActivity implements
        ScheduleView.EmptyViewClickListener,
        ScheduleView.ScheduleEntryClickListener,
        ScheduleView.GetCurrentScheduleListener,
        TimePickerDialog.OnTimeSetListener {

    private ScheduleView mScheduleView;
    private boolean mSettingStartTime;
    private boolean mShowingError;
    private View mNewEntryView;
    private SmartheatingDbHelper mDbHelper;
    private int mRoomID;
    private ArrayList<String> thermostatRFIDs;
    private int mSelectedDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDbHelper = new SmartheatingDbHelper(this);

        mRoomID = getIntent().getIntExtra("roomID", -1);
        thermostatRFIDs = getIntent().getStringArrayListExtra("thermostatRFIDs");
        setTitle("Schedule for " + getIntent().getStringExtra("roomName"));
        mScheduleView = (ScheduleView) findViewById(R.id.scheduleView);

        mScheduleView.setOnScheduleEntryClickListener(this);
        mScheduleView.setEmptyViewClickListener(this);
        mScheduleView.setGetCurrentScheduleListener(this);

        mScheduleView.goToHour(6);
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int i, int i1) {
        int selectedHour = i;
        int selectedMinutes = (i1 / 15) * 15;

        final TextView start = (TextView) mNewEntryView.findViewById(R.id.new_entry_start);
        final TextView end = (TextView) mNewEntryView.findViewById(R.id.new_entry_end);

        if (mSettingStartTime) {
            start.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");
        } else {
            end.setText((selectedHour > 9 ? selectedHour : "0" + selectedHour) + ":" + "00");
        }

        int startTime = Integer.valueOf(start.getText().toString().split(":")[0]);
        int endTime = Integer.valueOf(end.getText().toString().split(":")[0]);

        if (startTime > endTime) {
            end.requestFocus();
            end.setError("The end time of the entry must not be before the start.");
            mShowingError = true;
        } else {
            end.setError(null);
            mShowingError = false;
        }
    }

    @Override
    public void onEmptyViewClicked(Calendar time) {
        mSelectedDay = time.get(Calendar.DAY_OF_WEEK);
        newEntry(time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.HOUR_OF_DAY) + 1, false, Utility.DEFAULT_TEMPERATURE);
    }

    @Override
    public void onEmptyViewLongPress(Calendar time) {
        mSelectedDay = time.get(Calendar.DAY_OF_WEEK);
        newEntry(time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.HOUR_OF_DAY) + 1, false, Utility.DEFAULT_TEMPERATURE);
    }

    @Override
    public void onScheduleEntryClick(ScheduleEntry entry, RectF eventRect, ScheduleEntry nextEntry, int hourClicked) {
        mSelectedDay = entry.getDay();
        newEntry(hourClicked, hourClicked + 1, false, Utility.DEFAULT_TEMPERATURE);
    }

    @Override
    public void onScheduleEntryLongPress(ScheduleEntry entry, RectF eventRect) {
        mSelectedDay = entry.getDay();
        newEntry(entry.getStartTime(), entry.getEndTime(), true, entry.getTemperature());
    }

    @Override
    public List<ScheduleEntry> getCurrentSchedule() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        List<ScheduleEntry> entries = mDbHelper.getCurrentSchedule(db, mRoomID);
        db.close();
        return entries;
    }

    /**
     * The user tapped on the schedule. Show a popup either for creating a new entry or editing an existing one.
     * <p>
     * This method only works if the application has a working internet connection, otherwise a
     * popup is shown.
     *
     * @param startTime    The time the user tapped on
     * @param endTime      One hour after startTime or in case of editing the actual endTime of the entry
     * @param editExisting Whether or not the user is editing an existing entry
     * @param temperature  The default temperature set in the Utility class or the temperature of the existing entry
     */
    private void newEntry(final int startTime, final int endTime, boolean editExisting, double temperature) {
        if (!Utility.isCurrentlyOnline()) {
            Utility.showDisconnectedPopup();
            return;
        }
        AlertDialog.Builder newEntryDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Light_Dialog));
        LayoutInflater factory = LayoutInflater.from(this);
        mNewEntryView = factory.inflate(R.layout.new_schedule_entry, null);

        if (editExisting) {
            newEntryDialog.setTitle("Edit existing entry");
        } else {
            newEntryDialog.setTitle("Create new schedule entry");
        }

        // Load view elements.
        VerticalSeekBar seekBar = (VerticalSeekBar) mNewEntryView.findViewById(R.id.new_entry_seekBar);
        final TextView start = (TextView) mNewEntryView.findViewById(R.id.new_entry_start);
        final TextView end = (TextView) mNewEntryView.findViewById(R.id.new_entry_end);
        final TextView temp = (TextView) mNewEntryView.findViewById(R.id.new_entry_temperature);
        final CheckBox check = (CheckBox) mNewEntryView.findViewById(R.id.checkBox);

        // Fill view elements.
        start.setText((startTime > 9 ? startTime : "0" + startTime) + ":00");
        end.setText((endTime > 9 ? endTime : "0" + endTime) + ":00");
        temp.setText(temperature + "°");
        double progress = (temperature - Utility.LOWEST_TEMPERATURE) * 2 / Utility.TEMPERATURE_STEPS;
        seekBar.setProgress((int) (progress * seekBar.getMax()));

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSettingStartTime = true;
                TimePickerDialog.newInstance(ScheduleActivity.this, startTime, 0, true).show(getFragmentManager(), "timePicker");
            }
        });

        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSettingStartTime = false;
                TimePickerDialog.newInstance(ScheduleActivity.this, endTime, 0, true).show(getFragmentManager(), "timePicker");
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 2f;
                temp.setText(String.valueOf(value + Utility.LOWEST_TEMPERATURE) + "°");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        newEntryDialog.setView(mNewEntryView);
        // Positive button onClickListener will be overwritten later for input checking.
        newEntryDialog.setPositiveButton("Submit", null);
        newEntryDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog newDialog = newEntryDialog.create();
        newDialog.show();
        newDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If endTime is after startTime, mShowingError is false, thus create the new entry,
                // otherwise, keep the dialog open.
                if (!mShowingError) {
                    addScheduleEntry(start.getText().toString(),
                            end.getText().toString(),
                            temp.getText().toString(),
                            check.isChecked());
                    newDialog.dismiss();
                }
            }
        });
    }

    /**
     * Add a new schedule entry to the local database as well as the server.
     *
     * @param startString The String containing the starting time
     * @param endString   The String containing the ending time
     * @param tempString  The String containing the desired temperature
     * @param noHeating   Boolean indicating if the user wants to disable heating in this period
     */
    private void addScheduleEntry(String startString, String endString, String tempString, Boolean noHeating) {
        int start = Integer.valueOf(startString.split(":")[0]);
        int end = Integer.valueOf(endString.split(":")[0]);
        double temperature = Utility.NO_HEATING_TEMPERATURE;

        if (!noHeating) {
            temperature = Double.valueOf(tempString.substring(0, tempString.length() - 1));
        }

        // Add to local database.
        ContentValues values = new ContentValues();
        values.put(Schedules.COLUMN_NAME_START_TIME, start);
        values.put(Schedules.COLUMN_NAME_END_TIME, end);
        values.put(Schedules.COLUMN_NAME_TEMPERATURE, temperature);
        values.put(Schedules.COLUMN_NAME_ROOM_ID, mRoomID);
        values.put(Schedules.COLUMN_NAME_DAY, mSelectedDay);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        db.insert(Schedules.TABLE_NAME, null, values);
        db.close();
        mScheduleView.notifyDatasetChanged();

        // Add to server. The entry must be added to every thermostat in the room.
        Request mRequest = new Request(this);
        for (String RFID : thermostatRFIDs) {
            mRequest.addScheduleEntry(new ScheduleEntry(temperature, start, end, mSelectedDay), mRoomID, RFID);
        }
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
    public void onNewIntent(Intent intent) {
        // Do nothing! Scanning NFC tags in the HomeActivity must not have an effect.
    }
}
