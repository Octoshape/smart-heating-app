package ch.ethz.smartheating;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

/**
 * Created by schmisam on 19/05/15.
 */
public class RoomAdapter extends BaseAdapter {

    private Context context;
    private String[] texts = {"Kitchen", "Bedroom1", "Bedroom2", "Bathroom", "Living Room", "Kid's Room", "Entry hall", "Laundry room", "Office"};
    private Double[] temps = {12d, 15.5, 18.5, 17d, 19.5, 20.5, 22d, 24d, 24.5 };
    private LayoutInflater inflater;

    public RoomAdapter(Context context) {
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return 9;
    }

    public Object getItem(int position) { return null; }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder = new Holder();

        View cellView = inflater.inflate(R.layout.gridview_room, null);

        holder.tempValue = (TextView) cellView.findViewById(R.id.tempValue);
        holder.name = (TextView) cellView.findViewById(R.id.roomName);
        //holder.etaValue = (TextView) cellView.findViewById(R.id.etaValue);
        holder.status = (TextView) cellView.findViewById(R.id.statusLabel);

        holder.name.setText(texts[position]);
        holder.tempValue.setText(String.valueOf(temps[position]));

        Drawable flame = context.getResources().getDrawable(R.drawable.glossy_flame);

        int width = flame.getIntrinsicWidth();
        int height = flame.getIntrinsicHeight();

        width = flame.getIntrinsicWidth() / 30;
        height = flame.getIntrinsicWidth() / 30;

        flame.setBounds(0, 0, flame.getIntrinsicWidth() / width, flame.getIntrinsicHeight() / height);

        holder.status.setCompoundDrawables(null, null, flame, null);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Utility.getColorForTemperature(temps[position])); // Changes this drawable to use a single color instead of a gradient
        gd.setAlpha(150);
        gd.setCornerRadius(10);
        gd.setStroke(2, 0xFF000000);
        cellView.setBackground(gd);

        cellView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent roomDetailIntent = new Intent (context, RoomDetailActivity.class);
                roomDetailIntent.putExtra("name", texts[position]);
                context.startActivity(roomDetailIntent);
            }
        });

        return cellView;
    }

    public class Holder
    {
        TextView tempValue;
        TextView status;
        TextView name;
        TextView etaValue;
    }
}
