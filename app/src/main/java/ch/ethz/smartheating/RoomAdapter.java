package ch.ethz.smartheating;

import android.content.Context;
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
    private String[] texts = {"aaa", "bbb", "ccc", "ddd", "eee", "fff", "eee", "hhh", "iii"};

    public RoomAdapter(Context context) {
        this.context = context;
    }

    public int getCount() {
        return 9;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv;
        if (convertView == null) {
            tv = new TextView(context);
            tv.setLayoutParams(new GridView.LayoutParams(85, 85));
        }
        else {
            tv = (TextView) convertView;
        }

        tv.setText(texts[position]);
        return tv;
    }
}
