package ch.ethz.smartheating.activities;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import ch.ethz.smartheating.R;
import ch.ethz.smartheating.adapters.RoomAdapter;
import ch.ethz.smartheating.networking.Request;
import ch.ethz.smartheating.utilities.Utility;

/**
 * The HomeActivity is where the user gets an overview of the heating system. He can see all the
 * rooms in the system and their current temperature. A heating icon indicates if the room is being
 * heated up at the moment or not. Rooms can be added or removed and tapping one of the rooms will
 * lead to its {@link RoomDetailActivity}.
 */
public class HomeActivity extends ActionBarActivity {

    /**
     * The SettingsFragment for the user settings.
     */
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);
            PreferenceManager manager = getPreferenceManager();
            manager.setSharedPreferencesName(Utility.PREFERENCES);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    private GridView mRoomGridView;
    private Button mDeleteRoomButton;
    private Button mAddRoomButton;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mSharedPreferences = this.getSharedPreferences(Utility.PREFERENCES, Context.MODE_PRIVATE);

        // Check if the user has selected to create a dummy house with random rooms.
        if (mSharedPreferences.getBoolean("add_dummy_house", false)) {
            Utility.createDummyHouse(this, 10, 3);

            // Once created it won't be done again.
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean("add_dummy_house", false);
            editor.commit();
        }

        // Start the regular updates every 15 minutes.
        Utility.startUpdates(new Request(this));

        // Set up view elements.
        mAddRoomButton = (Button) findViewById(R.id.addRoomButton);
        mAddRoomButton.setBackgroundResource(android.R.drawable.btn_default);
        mAddRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddRoomPopup();
            }
        });

        mDeleteRoomButton = (Button) findViewById(R.id.deleteRoomButton);
        mDeleteRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteRooms();
            }
        });

        mRoomGridView = (GridView) findViewById(R.id.roomGridView);
        mRoomGridView.setVerticalSpacing(40);
        mRoomGridView.setHorizontalSpacing(40);
        mRoomGridView.setNumColumns(3);
        mRoomGridView.setAdapter(new RoomAdapter(this));

        // Initiate buttons below grid view.
        updateButtons();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mRoomGridView.getAdapter().getCount() == 0) {
            menu.getItem(1).setEnabled(false);
        } else {
            menu.getItem(1).setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_add_room:
                showAddRoomPopup();
                return true;
            case R.id.action_delete_room:
                deleteRooms();
                return true;
            case R.id.action_settings:
                // To show the settings fragment we hide all the other view elements and change the
                // activity's title and back button accordingly.
                mRoomGridView.setVisibility(View.GONE);
                mAddRoomButton.setVisibility(View.GONE);
                mDeleteRoomButton.setVisibility(View.GONE);
                setTitle("Settings");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);

                // Show the settings fragment.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.content, new SettingsFragment());
                ft.addToBackStack(null);
                ft.commit();
                return true;
            case android.R.id.home:
                // The back button is only active when the settings fragment was shown, thus we need
                // to show the other view elements again and pop the settings fragment.
                setTitle("Home");
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getFragmentManager().popBackStackImmediate();
                mRoomGridView.setVisibility(View.VISIBLE);
                mAddRoomButton.setVisibility(View.VISIBLE);
                mDeleteRoomButton.setVisibility(View.VISIBLE);

                // Update the temperatures in case the user has changed them and invalidate the views
                // to redraw with correct colors.
                Utility.updateTemps(mSharedPreferences);
                mRoomGridView.invalidateViews();
                return true;
            case R.id.action_sync:
                // Update the database manually.
                new Request(this).updateAll();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((RoomAdapter) mRoomGridView.getAdapter()).notifyDataSetChanged();
                    }
                }, 5000); // Delay, so that temperatures are updated before refreshing the views.
                return true;
            case R.id.action_reset:
                // Reset the application and database.
                Utility.reset(this);
                Intent homeIntent = new Intent(this, WelcomeActivity.class);
                startActivity(homeIntent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * On every onResume invalidate the grid view's views to allow updates to the heating
     * status to be displayed correctly.
     * <p/>
     * Set up the foreground dispatch to prevent NFC tag scans belonging to the system to open
     * the application a second time.
     * <p/>
     * Initiate the constantly running check for a working internet connection. This has to be done
     * in every Activity anew because the check, once failing, needs to change the currently shown
     * action bar.
     */
    @Override
    public void onResume() {
        super.onResume();
        mRoomGridView.invalidateViews();
        Utility.setupForegroundDispatch(this);
        Utility.startConnectivityCheck(this);
    }

    /**
     * Disable the foreground dispatch for this activity, the next one will handle NFC tag scans itself.
     * <p/>
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

    /**
     * Show the popup for adding a new room where the user can select a name for the new room to be
     * added.
     * <p/>
     * This method only works if the application has a working internet connection, otherwise a
     * popup is shown.
     */
    private void showAddRoomPopup() {
        // Check for internet connection.
        if (!Utility.isCurrentlyOnline()) {
            Utility.showDisconnectedPopup();
            return;
        }

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add new room")
                .setMessage("Please enter a name for the new room.")
                .setView(input)
                .setCancelable(false)

                        // Set up the buttons. Positive will be overwritten afterwards for input checks.
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })

                .show();

        // Overwrite the OK button to implement a check of the user input.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newRoomName = input.getText().toString().trim();
                // Check for valid input.
                if (newRoomName.isEmpty()) {
                    // Name can't be empty, keep the dialog open and notify the user.
                    Toast.makeText(getApplicationContext(), "Please enter a name.", Toast.LENGTH_SHORT).show();
                } else {
                    addNewRoom(newRoomName);
                    dialog.dismiss();
                }
            }
        });
    }

    /**
     * Add a new room to the grid view.
     *
     * @param name the new room's name.
     */
    private void addNewRoom(String name) {
        RoomAdapter roomAdapter = ((RoomAdapter) mRoomGridView.getAdapter());
        roomAdapter.addRoom(name);

        // Update buttons as room deletion might just have become possible.
        updateButtons();
    }

    /**
     * Depending on the current state this method does 3 different things:
     * <p/>
     * - Switch the grid view from deletion mode to regular mode if no rooms were selected for deletion.
     * <p/>
     * - Switch the grid view from regular mode to deletion mode.
     * <p/>
     * - Delete the rooms which were selected by the user.
     * <p/>
     * Can be called via the Delete button below the grid view during deletion mode or the delete
     * option in the settings of the action bar.
     * <p/>
     * This method only works if the application has a working internet connection, otherwise a
     * popup is shown.
     */
    private void deleteRooms() {
        // Check for internet connection.
        if (!Utility.isCurrentlyOnline()) {
            Utility.showDisconnectedPopup();
            return;
        }

        String buttonText = mDeleteRoomButton.getText().toString();
        RoomAdapter adapter = (RoomAdapter) mRoomGridView.getAdapter();

        // We decide what to do according to the delete button's text.
        switch (buttonText) {
            case "Delete rooms":
                adapter.setRemoving(true);
                break;
            case "Delete":
                adapter.removeSelectedRooms();
                ((RoomAdapter) mRoomGridView.getAdapter()).notifyDataSetChanged();
                break;
            case "Cancel":
                adapter.setRemoving(false);
                break;
            default:
                break;
        }

        // In any case we need to invalidate the grid view's views in order for animations to be
        // either stopped or started.
        mRoomGridView.invalidateViews();

        // Update buttons since we changed the state of the grid view.
        updateButtons();
    }

    /**
     * Update the buttons "Add Room" and "Delete Room" below the grid view to adjust whether they're
     * enabled and display the correct text during deletion mode.
     */
    public void updateButtons() {
        if (mRoomGridView.getCount() == 0) {
            mDeleteRoomButton.setEnabled(false);
        }

        // Check if the user is currently deleting rooms.
        if (((RoomAdapter) mRoomGridView.getAdapter()).isRemoving()) {
            // If so, disable the add room button.
            mAddRoomButton.setEnabled(false);

            // If The user has selected some rooms for deletion we change the Cancel button to Delete
            // and vice versa.
            if (((RoomAdapter) mRoomGridView.getAdapter()).removeSelectCount() > 0) {
                mDeleteRoomButton.setText("Delete");
                mDeleteRoomButton.setBackgroundColor(Color.RED);
            } else {
                mDeleteRoomButton.setText("Cancel");
                mDeleteRoomButton.setBackgroundColor(Color.WHITE);
            }
        } else {
            // If not, enable the Add Room button and reset the Delete rooms button.
            mAddRoomButton.setEnabled(true);
            mDeleteRoomButton.setBackgroundResource(android.R.drawable.btn_default);
            mDeleteRoomButton.setText("Delete rooms");
        }
    }
}
