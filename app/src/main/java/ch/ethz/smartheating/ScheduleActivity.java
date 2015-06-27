package ch.ethz.smartheating;

import android.app.AlertDialog;
import android.graphics.RectF;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.ethz.smartheating.R;
import ch.ethz.smartheating.schedule.ScheduleEntry;
import ch.ethz.smartheating.schedule.ScheduleView;


public class ScheduleActivity extends ActionBarActivity implements
        ScheduleView.EmptyViewClickListener,
        ScheduleView.ScheduleEntryClickListener,
        ScheduleView.GetCurrentScheduleListener {

    private ScheduleView mScheduleView;

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
    public void onEmptyViewClicked(Calendar time) {

    }

    @Override
    public void onEmptyViewLongPress(Calendar time) {
        Toast.makeText(this, "You pressed: " + time.get(Calendar.DAY_OF_WEEK_IN_MONTH) + " " + time.get(Calendar.HOUR_OF_DAY) + ":" + time.get(Calendar.MINUTE), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScheduleEntryClick(ScheduleEntry entry, RectF eventRect, ScheduleEntry nextEntry) {
        Toast.makeText(this, Utility.getDayString(nextEntry.getDay()) + " " + nextEntry.getStartTime() + " - " + nextEntry.getEndTime(), Toast.LENGTH_SHORT).show();
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
