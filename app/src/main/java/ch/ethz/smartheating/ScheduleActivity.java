package ch.ethz.smartheating;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.RectF;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.ethz.smartheating.schedule.ScheduleEntry;
import ch.ethz.smartheating.schedule.ScheduleView;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

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

        String startString = start.getText().toString();
        String endString = end.getText().toString();

        int startTime = Integer.valueOf(startString.split(":")[0]);
        int endTime = Integer.valueOf(endString.split(":")[0]);

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

    }

    @Override
    public void onEmptyViewLongPress(Calendar time) {
        Toast.makeText(this, "You pressed: " + time.get(Calendar.DAY_OF_WEEK_IN_MONTH) + " " + time.get(Calendar.HOUR_OF_DAY) + ":" + time.get(Calendar.MINUTE), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScheduleEntryClick(final ScheduleEntry entry, RectF eventRect, ScheduleEntry nextEntry) {
        final AlertDialog.Builder newEntryDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Light_Dialog));
        LayoutInflater factory = LayoutInflater.from(this);
        mNewEntryView = factory.inflate(R.layout.new_entry_view, null);

        final VerticalSeekBar seekBar = (VerticalSeekBar) mNewEntryView.findViewById(R.id.new_entry_seekBar);
        final TextView start = (TextView) mNewEntryView.findViewById(R.id.new_entry_start);
        final TextView end = (TextView) mNewEntryView.findViewById(R.id.new_entry_end);
        final TextView temp = (TextView) mNewEntryView.findViewById(R.id.new_entry_temperature);

        start.setText((entry.getStartTime() > 9 ? entry.getStartTime() : "0" + entry.getStartTime()) + ":00");
        end.setText((entry.getEndTime() > 9 ? entry.getEndTime() : "0" + entry.getEndTime()) + ":00");
        temp.setText(entry.getTemperature() + "°");
        double progress = (entry.getTemperature() - Utility.LOWEST_TEMPERATURE) * 2 / Utility.TEMPERATURE_STEPS;
        seekBar.setProgress((int)(progress * seekBar.getMax()));

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSettingStartTime = true;
                TimePickerDialog.newInstance(mThis, entry.getStartTime(), 0, true).show(getFragmentManager(), "timePicker");
            }
        });

        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSettingStartTime = false;
                TimePickerDialog.newInstance(mThis, entry.getEndTime(), 0, true).show(getFragmentManager(), "timePicker");
            }
        });

        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberPicker np = new NumberPicker(mThis);
                //np.setMinValue();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 2f;
                temp.setText(String.valueOf(value + 10) + "°");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        newEntryDialog.setTitle("Create new schedule entry");
        newEntryDialog.setView(mNewEntryView);
        newEntryDialog.setCancelable(false);

        newEntryDialog.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Will be overwritten later.
            }
        });

        final AlertDialog newDialog = newEntryDialog.create();

        newDialog.show();

        newDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mShowingError) {
                    // TODO : Add new entry to database, update existing entries, update ScheduleView.
                    newDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onScheduleEntryLongPress(ScheduleEntry entry, RectF eventRect) {

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
}
