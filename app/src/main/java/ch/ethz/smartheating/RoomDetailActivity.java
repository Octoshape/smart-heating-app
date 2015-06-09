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
import android.widget.TextView;

import ch.ethz.smartheating.db.smartHeatingContract;
import ch.ethz.smartheating.db.smartHeatingDbHelper;


public class RoomDetailActivity extends ActionBarActivity {

    private TextView mInfoText;
    private ListView mThermostats;
    private int roomID;
    private int serverID;
    private final smartHeatingDbHelper mDbHelper = new smartHeatingDbHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);
        setTitle(getIntent().getStringExtra("name"));

        mInfoText = (TextView) findViewById(R.id.roomDetailInstructions);
        mInfoText.setText(String.format(getResources().getString(R.string.room_detail_text), getTitle()));

        Cursor c = mDbHelper.getReadableDatabase().rawQuery("SELECT " + smartHeatingContract.Rooms._ID +
                                                            " FROM " + smartHeatingContract.Rooms.TABLE_NAME +
                                                            " WHERE " + smartHeatingContract.Rooms.COLUMN_NAME_NAME +
                                                            " LIKE '" + getTitle() + "'", null);

        c.moveToFirst();
        while (!c.isAfterLast()) {
            roomID = c.getInt(c.getColumnIndex(smartHeatingContract.Rooms._ID));
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
        values.put(smartHeatingContract.Thermostats.COLUMN_NAME_RFID, RFID);
        values.put(smartHeatingContract.Thermostats.COLUMN_NAME_ROOM_ID, roomID);
        values.put(smartHeatingContract.Thermostats.COLUMN_NAME_TEMPERATURE, -1);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int room_id = (int)db.insert(smartHeatingContract.Thermostats.TABLE_NAME, null, values);

        // TODO Implement me.
    }
}