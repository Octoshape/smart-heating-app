package ch.ethz.smartheating;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;


public class HomeActivity extends ActionBarActivity {

    private Button mAddRoomButton;
    private GridView mRoomGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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
            Intent addRoomIntent = new Intent(getApplicationContext(), AddRoomActivity.class);
            startActivity(addRoomIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
