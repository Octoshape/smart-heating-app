package ch.ethz.smartheating.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.RectF;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import ch.ethz.smartheating.schedule.ScheduleEntry;
import ch.ethz.smartheating.schedule.ScheduleView;
import ch.ethz.smartheating.utilities.Request;
import ch.ethz.smartheating.utilities.Utility;
import ch.ethz.smartheating.utilities.VerticalSeekBar;


public class ScheduleActivity extends ActionBarActivity implements
        ScheduleView.EmptyViewClickListener,
        ScheduleView.ScheduleEntryClickListener,
        ScheduleView.GetCurrentScheduleListener,
        TimePickerDialog.OnTimeSetListener {

    private ScheduleView mScheduleView;
    private boolean mSettingStartTime;
    private boolean mShowingError;
    private final ScheduleActivity mThis = this;
    private View mNewEntryView;
    private SmartheatingDbHelper mDbHelper;
    private int mRoomID;
    private int mServerID;
    private ArrayList<String> thermostats;
    private int mSelectedDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        mDbHelper = new SmartheatingDbHelper(this);

        mRoomID = getIntent().getIntExtra("roomID", -1);
        mServerID = getIntent().getIntExtra("serverID", -1);
        thermostats = getIntent().getStringArrayListExtra("thermostatRFIDs");

        mScheduleView = (ScheduleView) findViewById(R.id.scheduleView);

        mScheduleView.setOnScheduleEntryClickListener(this);
        mScheduleView.setEmptyViewClickListener(this);
        mScheduleView.setGetCurrentScheduleListener(this);

        mScheduleView.goToHour(6);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        newEntry(time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.HOUR_OF_DAY) + 1, false, Utility.DEFAULT_TEMPERATURE);
    }

    @Override
    public void onEmptyViewLongPress(Calendar time) {
        newEntry(time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.HOUR_OF_DAY) + 1, false, Utility.DEFAULT_TEMPERATURE);
    }

    @Override
    public void onScheduleEntryClick(ScheduleEntry entry, RectF eventRect, ScheduleEntry nextEntry, int hourClicked) {
        newEntry(hourClicked, hourClicked + 1, false, Utility.DEFAULT_TEMPERATURE);
    }

    @Override
    public void onScheduleEntryLongPress(ScheduleEntry entry, RectF eventRect) {
        newEntry(entry.getStartTime(), entry.getEndTime(), true, entry.getTemperature());
    }


    @Override
    public List<ScheduleEntry> getCurrentSchedule() {
        List<ScheduleEntry> list = new ArrayList<ScheduleEntry>();

        list.add(new ScheduleEntry(1, 12, 0, 10, Calendar.MONDAY));
        list.add(new ScheduleEntry(2, 15.5, 10, 14, Calendar.MONDAY));
        //list.add(new ScheduleEntry(3, 18, 14, 20, Calendar.MONDAY));
        list.add(new ScheduleEntry(4, 20, 20, 24, Calendar.MONDAY));

        list.add(new ScheduleEntry(1, 25, 0, 12, Calendar.TUESDAY));
        // list.add(new ScheduleEntry(2, 13.5, 12, 15, Calendar.TUESDAY));
        list.add(new ScheduleEntry(3, 17, 15, 20, Calendar.TUESDAY));
        list.add(new ScheduleEntry(4, 19.5, 20, 24, Calendar.TUESDAY));

        list.add(new ScheduleEntry(1, 12.5, 0, 9, Calendar.WEDNESDAY));
        // list.add(new ScheduleEntry(2, 25.5, 9, 12, Calendar.WEDNESDAY));
        list.add(new ScheduleEntry(3, 18, 12, 20, Calendar.WEDNESDAY));
        list.add(new ScheduleEntry(4, 21, 20, 24, Calendar.WEDNESDAY));

        list.add(new ScheduleEntry(1, 13, 0, 6, Calendar.THURSDAY));
        //   list.add(new ScheduleEntry(2, 16.5, 6, 14, Calendar.THURSDAY));
        list.add(new ScheduleEntry(3, 17, 14, 22, Calendar.THURSDAY));
        list.add(new ScheduleEntry(4, 26, 22, 24, Calendar.THURSDAY));

        list.add(new ScheduleEntry(1, 10, 0, 10, Calendar.FRIDAY));
        //  list.add(new ScheduleEntry(2, 10.5, 10, 17, Calendar.FRIDAY));
        list.add(new ScheduleEntry(3, 12, 17, 20, Calendar.FRIDAY));
        list.add(new ScheduleEntry(4, 23, 20, 24, Calendar.FRIDAY));

        list.add(new ScheduleEntry(1, 12.5, 0, 11, Calendar.SATURDAY));
        list.add(new ScheduleEntry(2, 25.5, 11, 18, Calendar.SATURDAY));
        list.add(new ScheduleEntry(3, 14, 18, 22, Calendar.SATURDAY));
        list.add(new ScheduleEntry(4, 20, 22, 24, Calendar.SATURDAY));

        list.add(new ScheduleEntry(1, 19, 0, 11, Calendar.SUNDAY));
        list.add(new ScheduleEntry(2, 16.5, 11, 14, Calendar.SUNDAY));
        list.add(new ScheduleEntry(3, 12, 14, 28, Calendar.SUNDAY));
        list.add(new ScheduleEntry(4, 10, 18, 24, Calendar.SUNDAY));

        return list;
    }

    private void newEntry(final int startTime, final int endTime, boolean editExisting, double temperature) {
        AlertDialog.Builder newEntryDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Light_Dialog));
        LayoutInflater factory = LayoutInflater.from(this);
        mNewEntryView = factory.inflate(R.layout.new_entry_view, null);

        if (editExisting) {
            newEntryDialog.setTitle("Edit existing entry");
        } else {
            newEntryDialog.setTitle("Create new schedule entry");
        }

        VerticalSeekBar seekBar = (VerticalSeekBar) mNewEntryView.findViewById(R.id.new_entry_seekBar);
        final TextView start = (TextView) mNewEntryView.findViewById(R.id.new_entry_start);
        final TextView end = (TextView) mNewEntryView.findViewById(R.id.new_entry_end);
        final TextView temp = (TextView) mNewEntryView.findViewById(R.id.new_entry_temperature);

        start.setText((startTime > 9 ? startTime : "0" + startTime) + ":00");
        end.setText((endTime > 9 ? endTime : "0" + endTime) + ":00");
        temp.setText(temperature + "°");
        double progress = (temperature - Utility.LOWEST_TEMPERATURE) * 2 / Utility.TEMPERATURE_STEPS;
        seekBar.setProgress((int) (progress * seekBar.getMax()));

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSettingStartTime = true;
                TimePickerDialog.newInstance(mThis, startTime, 0, true).show(getFragmentManager(), "timePicker");
            }
        });

        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSettingStartTime = false;
                TimePickerDialog.newInstance(mThis, endTime, 0, true).show(getFragmentManager(), "timePicker");
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

        newEntryDialog.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Will be overwritten later.
            }
        });

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
                if (!mShowingError) {
                    // TODO : Add new entry to database, update existing entries, update ScheduleView.
                    addScheduleEntry(start.getText().toString(),
                                     end.getText().toString(),
                                     temp.getText().toString());
                    Request r = new Request(getApplicationContext());
                    newDialog.dismiss();
                }
            }
        });
    }

    private void addScheduleEntry(String startString, String endString, String tempString) {
        int start = Integer.valueOf(startString.split(":")[0]);
        int end = Integer.valueOf(endString.split(":")[0]);
        double temperature = Double.valueOf(tempString.substring(0, tempString.length() - 1));

        ContentValues values = new ContentValues();
        values.put(Schedules.COLUMN_NAME_START_TIME, start);
        values.put(Schedules.COLUMN_NAME_END_TIME, end);
        values.put(Schedules.COLUMN_NAME_TEMPERATURE, temperature);
        values.put(Schedules.COLUMN_NAME_ROOM_ID, mRoomID);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int entryID = (int) db.insert(Schedules.TABLE_NAME, null, values);

        Request mRequest = new Request(this);
        for (String RFID : thermostats) {
            mRequest.addScheduleEntry(new ScheduleEntry(entryID, temperature, start, end, mSelectedDay), mServerID, RFID);
        }
    }
}
