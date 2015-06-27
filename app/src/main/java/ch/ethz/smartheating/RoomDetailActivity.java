package ch.ethz.smartheating;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import ch.ethz.smartheating.adapters.ThermostatAdapter;
import ch.ethz.smartheating.database.SmartheatingContract;
import ch.ethz.smartheating.database.SmartheatingDbHelper;


public class RoomDetailActivity extends ActionBarActivity {

    private TextView mInfoText;
    private ListView mThermostats;
    private VerticalSeekBar mSeekBar;
    private TextView mTarget;
    private int roomID;
    private int serverID;
    private final SmartheatingDbHelper mDbHelper = new SmartheatingDbHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);
        setTitle(getIntent().getStringExtra("name"));

        mInfoText = (TextView) findViewById(R.id.roomDetailInstructions);
        mInfoText.setText(String.format(getResources().getString(R.string.room_detail_text), getTitle()));

        mTarget = (TextView) findViewById(R.id.targetValue);
        mTarget.setText("10.0°");

        mSeekBar = (VerticalSeekBar) findViewById(R.id.seekBar1);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 2f;
                mTarget.setText(String.valueOf(value + 10) + "°");
                float yPos = mSeekBar.getHeight();
                int maxHeight = findViewById(R.id.seekBarLayout).getHeight() - 90;
                int minHeight = 10;
                mTarget.setY(maxHeight - (value * 2) / mSeekBar.getMax() * (maxHeight - minHeight));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Cursor c = mDbHelper.getReadableDatabase().rawQuery("SELECT " + SmartheatingContract.Rooms._ID +
                                                            " FROM " + SmartheatingContract.Rooms.TABLE_NAME +
                                                            " WHERE " + SmartheatingContract.Rooms.COLUMN_NAME_NAME +
                                                            " LIKE '" + getTitle() + "'", null);

        c.moveToFirst();
        while (!c.isAfterLast()) {
            roomID = c.getInt(c.getColumnIndex(SmartheatingContract.Rooms._ID));
            c.moveToNext();
        }

        mThermostats = (ListView) findViewById(R.id.thermostatList);
        mThermostats.setAdapter(new ThermostatAdapter(this, roomID));
    }

    @Override
    public void onResume() {
        super.onResume();
        Utility.setupForegroundDispatch(this);
    }

    @Override
    public void onPause() {
        Utility.stopForegroundDispatch(this);
        super.onPause();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] parcels = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            addThermostat(Utility.extractRFID(parcels));
        }

    }

    private void addThermostat (String RFID) {
        ContentValues values = new ContentValues();
        values.put(SmartheatingContract.Thermostats.COLUMN_NAME_RFID, RFID);
        values.put(SmartheatingContract.Thermostats.COLUMN_NAME_ROOM_ID, roomID);
        values.put(SmartheatingContract.Thermostats.COLUMN_NAME_TEMPERATURE, -1);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int room_id = (int)db.insert(SmartheatingContract.Thermostats.TABLE_NAME, null, values);
    }
}