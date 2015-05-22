package ch.ethz.smartheating;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class RoomDetailActivity extends ActionBarActivity {

    private TextView testText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        testText = (TextView) findViewById(R.id.testText);

        testText.setText("You have selected: " + getIntent().getStringExtra("name"));
    }
}