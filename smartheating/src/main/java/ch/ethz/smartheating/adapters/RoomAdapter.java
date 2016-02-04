package ch.ethz.smartheating.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

import ch.ethz.smartheating.R;
import ch.ethz.smartheating.activities.HomeActivity;
import ch.ethz.smartheating.activities.RoomDetailActivity;
import ch.ethz.smartheating.model.Room;
import ch.ethz.smartheating.networking.Request;
import ch.ethz.smartheating.utilities.Utility;
import ch.ethz.smartheating.database.SmartheatingDbHelper;

/**
 * The {@link BaseAdapter} holding the rooms for the {@link HomeActivity}.
 */
public class RoomAdapter extends BaseAdapter {

    private final Context mContext;
    private ArrayList<Room> rooms;
    private final LayoutInflater inflater;
    private final SmartheatingDbHelper mDbHelper;
    private boolean mIsRemoving;
    private final ArrayList<Integer> removeIDs;

    // Animation
    private final Random mRandom = new Random();
    private final Handler mHandler = new Handler();

    @Override
    public void notifyDataSetChanged() {
        updateRooms();
        super.notifyDataSetChanged();
    }

    /**
     * Set the delete mode of the adapter.
     *
     * @param value True iff the adapter should be set to delete mode.
     */
    public void setRemoving(boolean value) {
        mIsRemoving = value;
    }

    /**
     * @return Whether or not the adapter is in delete mode.
     */
    public boolean isRemoving() {
        return mIsRemoving;
    }

    /**
     * @return The amount of selected rooms to be deleted.
     */
    public int removeSelectCount() {
        return removeIDs.size();
    }

    /**
     * Update the values for the rooms.
     */
    private void updateRooms() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        mDbHelper.updateAllRoomTemps(db);
        rooms = mDbHelper.getRooms(db);
        db.close();
    }

    public RoomAdapter(Context context) {
        this.mContext = context;
        removeIDs = new ArrayList<>();
        mDbHelper = new SmartheatingDbHelper(context);
        updateRooms();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Remove the selected rooms when in delete mode.
     */
    public void removeSelectedRooms() {
        if (!mIsRemoving) {
            return;
        }
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        for (int nextID : removeIDs) {
            mDbHelper.deleteRoom(db, nextID, mContext);
        }
        db.close();

        removeIDs.clear();
        mIsRemoving = false;
    }

    /**
     * Add a new room to the local database.
     *
     * @param name The name of the new room.
     */
    public void addRoom(String name) {
        // Add the room to the local database.
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int roomID = mDbHelper.addRoom(db, name);
        db.close();

        // Add the room to the server.
        new Request(mContext).registerRoom(name, roomID);

        // Add the default schedule to the room.
        mDbHelper.resetSchedule(db, roomID);

        // Add the room to the adapter.
        rooms.add(new Room(name, roomID, 0d));
        super.notifyDataSetChanged();

        // After adding a new room to the system, we immediately go to it's RoomDetailActivity.
        Intent roomDetailIntent = new Intent(mContext, RoomDetailActivity.class);
        roomDetailIntent.putExtra("name", name);
        roomDetailIntent.putExtra("room_id", roomID);
        mContext.startActivity(roomDetailIntent);
    }

    public int getCount() {
        return rooms.size();
    }

    public View getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final View cellView = inflater.inflate(R.layout.gridview_room, null);

        TextView tempValue = (TextView) cellView.findViewById(R.id.tempValue);
        TextView name = (TextView) cellView.findViewById(R.id.roomName);
        ImageView flame = (ImageView) cellView.findViewById(R.id.flameImage);

        name.setText(rooms.get(position).getName());
        tempValue.setText(String.valueOf(rooms.get(position).getTemperature()) + "°");

        // Show the flame icon if the room's target temperature is higher than its current temperature.
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        if (mDbHelper.getCurrentTargetTemperature(db, rooms.get(position).getID()) > rooms.get(position).getTemperature()) {
            flame.setImageResource(R.drawable.flame_animation);
            AnimationDrawable flameAnimation = (AnimationDrawable) flame.getDrawable();
            flameAnimation.start();
        } else {
            flame.setImageResource(R.drawable.glossy_flame_invisible);
        }
        db.close();

        final GradientDrawable gd = new GradientDrawable();
        gd.setColor(Utility.getColorForTemperature(rooms.get(position).getTemperature()));
        gd.setAlpha(150);
        gd.setCornerRadius(10);
        gd.setStroke(3, 0xFF000000);
        cellView.setBackground(gd);

        cellView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the adapter is in delete mode, adjust the color of the room that's tapped,
                // otherwise, simply open the corresponding RoomDetailView.
                if (mIsRemoving) {
                    // If the room has already been selected for deletion, we remove it from the list,
                    // otherwise we add it to the list, adjusting the color and animation accordingly.
                    if (removeIDs.contains(rooms.get(position).getID())) {
                        Animation wiggleAnimation = AnimationUtils.loadAnimation(mContext, R.anim.wiggle_animation);
                        cellView.startAnimation(wiggleAnimation);
                        gd.setColor(Utility.getColorForTemperature(rooms.get(position).getTemperature()));
                        removeIDs.remove(rooms.get(position).getID());
                    } else {
                        cellView.clearAnimation();
                        gd.setColor(Color.GRAY);
                        removeIDs.add(rooms.get(position).getID());
                    }

                    cellView.setBackground(gd);

                    // Update the buttons from the HomeActivity.
                    ((HomeActivity) mContext).updateButtons();
                } else {
                    Intent roomDetailIntent = new Intent(mContext, RoomDetailActivity.class);
                    roomDetailIntent.putExtra("name", rooms.get(position).getName());
                    roomDetailIntent.putExtra("room_id", rooms.get(position).getID());
                    mContext.startActivity(roomDetailIntent);
                }
            }
        });

        // If the adapter is in delete mode, start the wiggle animations for all the rooms but with
        // different delays, so they don't wiggle in sync.
        if (mIsRemoving) {
            final Animation wiggleAnimation = AnimationUtils.loadAnimation(mContext, R.anim.wiggle_animation);
            wiggleAnimation.setStartOffset(mRandom.nextInt(200));
            cellView.setAnimation(wiggleAnimation);
            final Runnable runnable = new Runnable() {
                public void run() {
                    Animation anim = cellView.getAnimation();
                    anim.setStartOffset(0);
                    cellView.setAnimation(anim);
                }
            };
            mHandler.postDelayed(runnable, mRandom.nextInt(200));
        }

        return cellView;
    }
}
