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
public class RoomAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<String> names; //{"Kitchen", "Bedroom1", "Bedroom2", "Bathroom", "Living Room", "Kid's Room", "Entry hall", "Laundry room", "Office"};
    private ArrayList<Double> temps; //{12d, 15.5, 18.5, 17d, 19.5, 20.5, 22d, 24d, 24.5 };
    private ArrayList<View> views;
    private LayoutInflater inflater;
    private smartHeatingDbHelper mDbHelper;

    @Override
    public void notifyDataSetChanged() {
        names = getNames();
        temps = getTemps();
        super.notifyDataSetChanged();
    }

    public ArrayList<String> getNames () {
        Cursor cursor = mDbHelper.getReadableDatabase().rawQuery("SELECT name FROM rooms", null);
        cursor.moveToFirst();
        ArrayList<String> names = new ArrayList<String>();
        while(!cursor.isAfterLast()) {
            names.add(cursor.getString(cursor.getColumnIndex("name")));
            cursor.moveToNext();
        }
        cursor.close();
        return names;
    }

    private ArrayList<Double> getTemps () {
        Cursor cursor = mDbHelper.getReadableDatabase().rawQuery("SELECT temperature FROM rooms", null);
        cursor.moveToFirst();
        ArrayList<Double> temps = new ArrayList<Double>();
        while(!cursor.isAfterLast()) {
            temps.add(cursor.getDouble(cursor.getColumnIndex("temperature")));
            cursor.moveToNext();
        }
        cursor.close();
        return temps;
    }

    public RoomAdapter(Context context) {
        this.context = context;
        mDbHelper = new smartHeatingDbHelper(context);
        names = getNames();
        temps = getTemps();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return names.size();
    }

    public View getItem(int position) { return null; }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder = new Holder();

        View cellView = inflater.inflate(R.layout.gridview_room, null);

        holder.tempValue = (TextView) cellView.findViewById(R.id.tempValue);
        holder.name = (TextView) cellView.findViewById(R.id.roomName);

        holder.name.setText(names.get(position));
        holder.tempValue.setText(String.valueOf(temps.get(position)) + "°");

        holder.flame = (ImageView) cellView.findViewById(R.id.flameImage);

        holder.flame.setImageResource(R.drawable.flame_animation);

        AnimationDrawable flameAnimation = (AnimationDrawable) holder.flame.getDrawable();

        flameAnimation.start();

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Utility.getColorForTemperature(temps.get(position))); // Changes this drawable to use a single color instead of a gradient
        gd.setAlpha(150);
        gd.setCornerRadius(10);
        gd.setStroke(3, 0xFF000000);
        cellView.setBackground(gd);

        cellView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent roomDetailIntent = new Intent (context, RoomDetailActivity.class);
                roomDetailIntent.putExtra("name", names.get(position));
                context.startActivity(roomDetailIntent);
            }
        });

        return cellView;
    }

    public class Holder
    {
        TextView tempValue;
        TextView name;
        ImageView flame;
    }
}
