package ch.ethz.smartheating;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import ch.ethz.smartheating.db.smartHeatingDbHelper;

/**
 * Created by schmisam on 19/05/15.
 */
public class ThermostatAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Double> temps;
    private LayoutInflater inflater;
    private smartHeatingDbHelper mDbHelper;
    private int roomID;

    @Override
    public void notifyDataSetChanged() {
        temps = getTemps();
        super.notifyDataSetChanged();
    }

    private ArrayList<Double> getTemps () {
        Cursor cursor = mDbHelper.getReadableDatabase().rawQuery("SELECT temperature FROM thermostats WHERE room_id LIKE " + roomID, null);
        cursor.moveToFirst();
        ArrayList<Double> temps = new ArrayList<Double>();
        while(!cursor.isAfterLast()) {
            temps.add(cursor.getDouble(cursor.getColumnIndex("temperature")));
            cursor.moveToNext();
        }
        cursor.close();
        return temps;
    }

    public ThermostatAdapter(Context context, int roomID) {
        this.context = context;
        this.roomID = roomID;
        mDbHelper = new smartHeatingDbHelper(context);
        temps = getTemps();
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
