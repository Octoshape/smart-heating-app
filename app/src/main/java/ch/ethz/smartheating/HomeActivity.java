package ch.ethz.smartheating;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NfcAdapter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import ch.ethz.smartheating.db.smartHeatingDbHelper;
import ch.ethz.smartheating.db.smartHeatingContract.Rooms;

public class HomeActivity extends ActionBarActivity {

    private GridView mRoomGridView;
    private String mNewRoomName = "";
    private final smartHeatingDbHelper mDbHelper = new smartHeatingDbHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Utility.createDummyHouse(this, 10, 5);

        mRoomGridView = (GridView) findViewById(R.id.roomGridView);

        mRoomGridView.setVerticalSpacing(40);
        mRoomGridView.setHorizontalSpacing(40);
        mRoomGridView.setNumColumns(3);
        mRoomGridView.setAdapter(new RoomAdapter(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_room) {

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected
            input.setInputType(InputType.TYPE_CLASS_TEXT);

            final AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Add new room")
            .setMessage("Please enter a name for the new room.")
            .setView(input)
            .setCancelable(false)

            // Set up the buttons
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Gets overridden after .show();
                }
            })
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
                    mNewRoomName = input.getText().toString().trim();
                    if (mNewRoomName.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Please enter a name.", Toast.LENGTH_SHORT).show();
                    } else {
                        addNewRoom (mNewRoomName);
                        dialog.dismiss();
                    }
                }
            });
        }

        return super.onOptionsItemSelected(item);
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
        // Do nothing!
    }

    private void addNewRoom (String name) {
        ContentValues values = new ContentValues();
        values.put(Rooms.COLUMN_NAME_NAME, name);
        values.put(Rooms.COLUMN_NAME_TEMPERATURE, 0);
        values.put(Rooms.COLUMN_NAME_SERVER_ID, -1);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int room_id = (int)db.insert(Rooms.TABLE_NAME, null, values);

        RoomAdapter roomAdapter = ((RoomAdapter)mRoomGridView.getAdapter());
        roomAdapter.notifyDataSetChanged();

        Request mRequest = new Request(this);
        mRequest.registerRoom(name, room_id);

        Intent roomDetailIntent = new Intent(this, RoomDetailActivity.class);
        roomDetailIntent.putExtra("name", name);
        startActivity(roomDetailIntent);
    }
}
