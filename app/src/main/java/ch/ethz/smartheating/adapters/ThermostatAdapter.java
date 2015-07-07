package ch.ethz.smartheating.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import ch.ethz.smartheating.R;
import ch.ethz.smartheating.utilities.Utility;
import ch.ethz.smartheating.database.SmartheatingDbHelper;

/**
 * Created by schmisam on 19/05/15.
 */
public class ThermostatAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Double> temps;
    private ArrayList<String> RFIDs;
    private LayoutInflater inflater;
    private SmartheatingDbHelper mDbHelper;
    private int roomID;

    @Override
    public void notifyDataSetChanged() {
        updateTemps();
        super.notifyDataSetChanged();
    }

    public ArrayList<String> getRFIDs () {
        return RFIDs;
    }

    private void updateTemps () {
        temps = new ArrayList<Double>();
        RFIDs = new ArrayList<String>();
        Cursor cursor = mDbHelper.getReadableDatabase().rawQuery("SELECT temperature, rfid FROM thermostats WHERE room_id LIKE " + roomID, null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            temps.add(cursor.getDouble(cursor.getColumnIndex("temperature")));
            RFIDs.add(cursor.getString(cursor.getColumnIndex("rfid")));
            cursor.moveToNext();
        }
        cursor.close();
    }

    public ThermostatAdapter(Context context, int roomID) {
        this.context = context;
        this.roomID = roomID;
        mDbHelper = new SmartheatingDbHelper(context);
        updateTemps();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return temps.size();
    }

    public View getItem(int position) { return null; }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder = new Holder();

        View cellView = inflater.inflate(R.layout.listview_thermostat, null);

        holder.tempValue = (TextView) cellView.findViewById(R.id.thermostatTemp);
        holder.name = (TextView) cellView.findViewById(R.id.thermostatName);

        holder.name.setText("Thermostat " + (position + 1));
        holder.tempValue.setText(String.valueOf(temps.get(position)) + "°");

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Utility.getColorForTemperature(temps.get(position)));
        gd.setAlpha(150);
        cellView.setBackground(gd);

        return cellView;
    }

    public class Holder
    {
        TextView tempValue;
        TextView name;
    }
}
