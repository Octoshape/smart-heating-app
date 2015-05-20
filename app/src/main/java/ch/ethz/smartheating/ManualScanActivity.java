package ch.ethz.smartheating;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class ManualScanActivity extends ActionBarActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_scan);

        mTextView = (TextView) findViewById(R.id.textView);
        mTextView.setText(R.string.manual_scan_text);
    }
}
