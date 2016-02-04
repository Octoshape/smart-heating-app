package ch.ethz.smartheating.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ch.ethz.smartheating.activities.RoomDetailActivity;
import ch.ethz.smartheating.R;
import ch.ethz.smartheating.model.Thermostat;
import ch.ethz.smartheating.utilities.Utility;
import ch.ethz.smartheating.database.SmartheatingDbHelper;

/**
 * The {@link BaseAdapter} holding the thermostats for the {@link RoomDetailActivity}.
 */
public class ThermostatAdapter extends BaseAdapter {

    private ArrayList<Thermostat> thermostats;
    private final LayoutInflater inflater;
    private final SmartheatingDbHelper mDbHelper;
    private final int mRoomID;
    private final Context mContext;

    @Override
    public void notifyDataSetChanged() {
        updateValues();
        super.notifyDataSetChanged();
    }

    public ArrayList<String> getRFIDs() {
        ArrayList<String> RFIDs = new ArrayList<>();
        for (Thermostat t : thermostats) {
            RFIDs.add(t.getRFID());
        }
        return RFIDs;
    }

    /**
     * Update all the values of the thermostats from the database.
     */
    private void updateValues() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        thermostats = mDbHelper.getThermostats(db, mRoomID);
        db.close();
    }

    public ThermostatAdapter(Context context, int roomID) {
        this.mRoomID = roomID;
        this.mContext = context;
        mDbHelper = new SmartheatingDbHelper(context);
        updateValues();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return thermostats.size();
    }

    public View getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        View cellView = inflater.inflate(R.layout.listview_thermostat, null);

        TextView tempValue = (TextView) cellView.findViewById(R.id.thermostatTemp);
        final TextView name = (TextView) cellView.findViewById(R.id.thermostatName);
        ImageView deleteButton = (ImageView) cellView.findViewById(R.id.deleteThermostatButton);

        name.setText(thermostats.get(position).getName());
        tempValue.setText(String.valueOf(thermostats.get(position).getTemperature()) + "°");
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeletePopup(position, thermostats.get(position).getName());
            }
        });

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Utility.getColorForTemperature(thermostats.get(position).getTemperature()));
        gd.setAlpha(150);
        cellView.setBackground(gd);
        cellView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do nothing.
            }
        });

        return cellView;
    }

    /**
     * Show an {@link AlertDialog} on screen for deleting a thermostat.
     * <p>
     * This method only works if the application has a working internet connection, otherwise a
     * popup is shown.
     *
     * @param position The index of the thermostat in the list.
     * @param name     The name of the thermostat to be deleted.
     */
    private void showDeletePopup(final int position, String name) {
        if (!Utility.isCurrentlyOnline()) {
            Utility.showDisconnectedPopup();
            return;
        }
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle("Remove Thermostat")
                .setMessage("Are you sure you want to delete the Thermostat " + name + "?")
                .setCancelable(true)
                .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String RFID = thermostats.get(position).getRFID();
                        SQLiteDatabase db = mDbHelper.getWritableDatabase();
                        mDbHelper.deleteThermostat(db, RFID, mRoomID, mContext);
                        notifyDataSetChanged();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }
}
