package ch.ethz.smartheating;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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

        mAddRoomButton = (Button) findViewById(R.id.addRoomButton);

        mAddRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addRoomIntent = new Intent(getApplicationContext(), AddRoomActivity.class);
                startActivity(addRoomIntent);
            }
        });

        mRoomGridView = (GridView) findViewById(R.id.roomGridView);

        mRoomGridView.setAdapter(new RoomAdapter(this));

        mRoomGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent roomDetailIntent = new Intent (getApplicationContext(), RoomDetailActivity.class);
                roomDetailIntent.putExtra("name", ((TextView) view).getText());
                startActivity(roomDetailIntent);
            }
        });
    }
}
