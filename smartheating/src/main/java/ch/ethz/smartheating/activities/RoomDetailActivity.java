package ch.ethz.smartheating.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

import ch.ethz.smartheating.R;
import ch.ethz.smartheating.adapters.ThermostatAdapter;
import ch.ethz.smartheating.database.SmartheatingDbHelper;
import ch.ethz.smartheating.networking.Request;
import ch.ethz.smartheating.model.ScheduleEntry;
import ch.ethz.smartheating.utilities.Utility;
import ch.ethz.smartheating.utilities.VerticalSeekBar;

/**
 * The RoomDetailActivity shows details about a selected room. The upper section shows the
 * registered thermostats for this room and their current temperature. Below the user can find the
 * current overall temperature of the room as well as the currently set target temperature as directed
 * by the heating schedule for the room. The user can set a new target temperature and add or remove
 * thermostats by scanning their NFC tags or hitting the X button next to an existing thermostat.
 * <p>
 * Via the Settings menu in the ActionBar, the user can view and modify the schedule for the room.
 * This will bring up the {@link ScheduleActivity} for the room.
 */
public class RoomDetailActivity extends ActionBarActivity {

    private ListView mThermostats;
    private boolean mListeningForTags = true;
    private ImageView mTemperatureBar;
    private VerticalSeekBar mSeekBar;
    private int mRoomID, mTargetProgress, mActualProgress;
    private Button mConfirmButton, mCancelButton;
    private final SmartheatingDbHelper mDbHelper = new SmartheatingDbHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Add title from the room the user has selected via Intent.
        setTitle(getIntent().getStringExtra("name"));
        mRoomID = getIntent().getIntExtra("room_id", -1);

        // Get View elements.
        TextView infoTextView = (TextView) findViewById(R.id.roomDetailInstructions);
        TextView temperatureView = (TextView) findViewById(R.id.temperatureText);
        mTemperatureBar = (ImageView) findViewById(R.id.temperatureBar);
        final TextView targetTempView = (TextView) findViewById(R.id.targetValue);
        mSeekBar = (VerticalSeekBar) findViewById(R.id.seekBar1);
        mThermostats = (ListView) findViewById(R.id.thermostatList);
        mConfirmButton = (Button) findViewById(R.id.confirmButton);
        mCancelButton = (Button) findViewById(R.id.cancelButton);
        final ImageView targetTemperatureBar = (ImageView) findViewById(R.id.temperatureTargetBar);
        final GradientDrawable targetRectangleDrawable = (GradientDrawable) targetTemperatureBar.getDrawable();

        // Modify View elements.
        infoTextView.setText(String.format(getResources().getString(R.string.room_detail_text), getTitle()));
        targetRectangleDrawable.setAlpha(100);

        mThermostats.setAdapter(new ThermostatAdapter(this, mRoomID));
        mThermostats.setOverScrollMode(ListView.OVER_SCROLL_NEVER);

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This only works if the application has a working internet connection, otherwise a
                // popup is shown.
                if (!Utility.isCurrentlyOnline()) {
                    Utility.showDisconnectedPopup();
                    return;
                }
                mTargetProgress = mSeekBar.getProgress();
                mCancelButton.setEnabled(false);
                mConfirmButton.setEnabled(false);
                mCancelButton.setBackground(ContextCompat.getDrawable(RoomDetailActivity.this, R.drawable.cross_disabled));
                mConfirmButton.setBackground(ContextCompat.getDrawable(RoomDetailActivity.this, R.drawable.tick_disabled));
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                double targetTemp = Double.valueOf(targetTempView.getText().subSequence(0, targetTempView.length() - 1).toString());
                mDbHelper.setCurrentTargetTemperature(db, targetTemp, mRoomID);
                ScheduleEntry newEntry = mDbHelper.getCurrentScheduleEntry(db, mRoomID);
                db.close();
                Request r = new Request(RoomDetailActivity.this);
                Log.d("DEBUG", "New Entry: Day:" + newEntry.getDay() + " Start: " + newEntry.getStartTime() + " End: " + newEntry.getEndTime() + " Temp: " + newEntry.getTemperature());
                for(String RFID : ((ThermostatAdapter) mThermostats.getAdapter()).getRFIDs()) {
                    r.addScheduleEntry(newEntry, mRoomID, RFID);
                }
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSeekBar.setProgress(mTargetProgress);
            }
        });

        mSeekBar.setMax(Utility.TEMPERATURE_STEPS);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress != mTargetProgress) {
                    mCancelButton.setEnabled(true);
                    mConfirmButton.setEnabled(true);
                    mCancelButton.setBackground(ContextCompat.getDrawable(RoomDetailActivity.this, R.drawable.cross_enabled));
                    mConfirmButton.setBackground(ContextCompat.getDrawable(RoomDetailActivity.this, R.drawable.tick_enabled));
                } else {
                    mCancelButton.setEnabled(false);
                    mConfirmButton.setEnabled(false);
                    mCancelButton.setBackground(ContextCompat.getDrawable(RoomDetailActivity.this, R.drawable.cross_disabled));
                    mConfirmButton.setBackground(ContextCompat.getDrawable(RoomDetailActivity.this, R.drawable.tick_disabled));
                }
                // User has changed the desired value.
                int seekBarMax = seekBar.getMax();
                float tempValueRaw = ((float) progress / seekBarMax) * (Utility.HIGHEST_TEMPERATURE - Utility.LOWEST_TEMPERATURE);
                float tempValue = Math.round(tempValueRaw * 2) / 2f + Utility.LOWEST_TEMPERATURE;
                // Set text and height of target temperature.
                int thumbY = (seekBar.getThumb().getBounds().centerX() + seekBar.getThumb().getBounds().width() / 2);

                targetTemperatureBar.getLayoutParams().height = thumbY;
                targetTemperatureBar.setTop(seekBar.getHeight() - thumbY);
                targetRectangleDrawable.setColor(Utility.getColorForTemperature(tempValue));
                targetTempView.setText(String.valueOf(tempValue) + "°");
                targetTempView.setY(seekBar.getHeight() - thumbY - targetTempView.getHeight() / 2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Fetch values for dynamic View elements.
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        double targetTempValRaw = mDbHelper.getCurrentTargetTemperature(db, mRoomID);
        double tempValRaw = mDbHelper.getRoomTemperature(db, mRoomID);
        db.close();

        // Round to nearest 0.5
        double tempVal = Math.round(tempValRaw * 2) / 2d;
        double targetTempVal = Math.round(targetTempValRaw * 2) / 2d;

        String temperature = tempVal + "°";
        temperatureView.setText(temperature);

        mActualProgress = (int) ((tempVal - Utility.LOWEST_TEMPERATURE) / (Utility.HIGHEST_TEMPERATURE - Utility.LOWEST_TEMPERATURE) * mSeekBar.getMax());
        mTargetProgress = (int) ((targetTempVal - Utility.LOWEST_TEMPERATURE) / (Utility.HIGHEST_TEMPERATURE - Utility.LOWEST_TEMPERATURE) * mSeekBar.getMax());

        // Set color for circle and rectangle in the thermostat image according to current temperature
        targetRectangleDrawable.setColor(Utility.getColorForTemperature(targetTempVal));
        ((GradientDrawable) mTemperatureBar.getDrawable()).setColor(Utility.getColorForTemperature(tempVal));
        ImageView circleView = (ImageView) findViewById(R.id.circle);
        GradientDrawable circleDrawable = (GradientDrawable) circleView.getDrawable();
        circleDrawable.setColor(Utility.getColorForTemperature(tempVal));
    }

    /**
     * Set the thermometer's temperature bar to the actual temperature and the seek bar to the current
     * target temperature.
     * <p>
     * This needs to be done in this function because in onCreate, the layout parameters are not yet
     * set and any changes to them will not be seen on the screen.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // Reset the progress to what the room's temperature actually is, so we can adjust the thermometer's
        // temperature bar according to the thumb.
        mSeekBar.setProgress(mActualProgress);
        int thumbY = (mSeekBar.getThumb().getBounds().centerX() + mSeekBar.getThumb().getBounds().width() / 2);
        mTemperatureBar.setTop(mSeekBar.getHeight() - thumbY);
        mTemperatureBar.getLayoutParams().height = thumbY;

        // After setting the thermometer's bar we change the seek bar back to the target temperature.
        mSeekBar.setProgress(mTargetProgress);
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

    /**
     * Handle the NFC-tag scan intents. To prevent multiple scans we disable scanning while a new
     * thermostat is being added.
     *
     * @param intent The intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        if (mListeningForTags) {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
                // Prevent multiple scans of the same NFC tag.
                mListeningForTags = false;
                Parcelable[] parcels = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                showNamePopup(Utility.extractRFID(parcels));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_room_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_set_schedule:
                Intent newIntent = new Intent(this, ScheduleActivity.class);
                newIntent.putExtra("roomID", mRoomID);
                newIntent.putExtra("roomName", getIntent().getStringExtra("name"));
                newIntent.putExtra("thermostatRFIDs", ((ThermostatAdapter) mThermostats.getAdapter()).getRFIDs());
                startActivity(newIntent);
                return true;
            case R.id.action_add_dummy_thermostat:
                showNamePopup(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
                return true;
            case R.id.action_manual_tag_entry:
                showManualTagPopup();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Show the popup for entering a name for the new thermostat being added.
     * <p>
     * This method only works if the application has a working internet connection, otherwise a
     * popup is shown.
     *
     * @param RFID The RFID of the new thermostat
     */
    private void showNamePopup(final String RFID) {
        if (!Utility.isCurrentlyOnline()) {
            Utility.showDisconnectedPopup();
            return;
        }
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add new thermostat")
                .setMessage("Please enter a name for the new thermostat.")
                .setView(input)
                .setCancelable(false)

                        // Positive button onClickListener will be overwritten later for input checking.
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Enable NFC scanning again.
                        mListeningForTags = true;
                        dialog.cancel();
                    }
                })

                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newThermostatName = input.getText().toString().trim();
                // If the name is invalid, keep the dialog open.
                if (newThermostatName.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter a name.", Toast.LENGTH_SHORT).show();
                } else {
                    // Enable NFC scanning again.
                    mListeningForTags = true;
                    addThermostat(RFID, newThermostatName);
                    dialog.dismiss();
                }
            }
        });


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
                .setMessage("Please enter RFID number of the tag on the thermostat.")
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
                    showNamePopup(RFID);
                }
            }
        });
    }

    /**
     * Add a thermostat to the local database as well as to the server.
     *
     * @param RFID The RFID of the new thermostat
     * @param name The name of the new thermostat
     */
    private void addThermostat(String RFID, String name) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        mDbHelper.addThermostat(db, RFID, name, mRoomID);
        ArrayList<ScheduleEntry> schedule = mDbHelper.getSchedule(db, mRoomID);
        db.close();

        Request r = new Request(this);
        r.registerThermostat(mRoomID, RFID, name);
        // Add the schedule of the room to the thermostat on the server. The server has a schedule for every thermostat.
        // In the local database there is only one per room, so we don't need to add anything locally.
        for (ScheduleEntry entry : schedule) {
            r.addScheduleEntry(entry, mRoomID, RFID);
        }

        ((ThermostatAdapter) mThermostats.getAdapter()).notifyDataSetChanged();
    }
}