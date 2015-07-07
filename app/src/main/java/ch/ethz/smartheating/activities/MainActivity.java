package ch.ethz.smartheating.activities;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import ch.ethz.smartheating.R;
import ch.ethz.smartheating.utilities.Request;
import ch.ethz.smartheating.utilities.Utility;

public class MainActivity extends ActionBarActivity {

    private TextView mWelcomeTextView;
    private ImageView mScanImageView;
    private Button mManualScanButton;
    private NfcAdapter mNfcAdapter;
    private static final String logTag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (alreadyRegistered()) {
            Intent homeIntent = new Intent(this, HomeActivity.class);
            startActivity(homeIntent);
        }

        setContentView(R.layout.activity_main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Utility.adapter = mNfcAdapter;

        mWelcomeTextView = (TextView) findViewById(R.id.welcomeTextView);
        mWelcomeTextView.setText(R.string.welcome_text);

        if (mNfcAdapter == null) {
            // TODO: For emulator use, remove later.
            Utility.RESIDENCE_RFID = "120398746";
            new Request(this).registerResidence();
            new Request(this).registerUser("125554");
            Intent homeIntent = new Intent(this, HomeActivity.class);
            startActivity(homeIntent);
            //

            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onResume() {
        Log.d(logTag, "onResume called");
        super.onResume();
        Utility.setupForegroundDispatch(this);
    }

    @Override
    public void onPause() {
        Log.d(logTag, "onPause called");
        Utility.stopForegroundDispatch(this);
        super.onPause();
    }

    @Override
    public void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] parcels = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            registerPi(Utility.extractRFID(parcels));
        }

        Intent homeIntent = new Intent(this, HomeActivity.class);
        startActivity(homeIntent);
    }

    private void registerPi(String RFID) {
        Utility.RESIDENCE_RFID = RFID;

        Request mRequest = new Request(this);

        Log.d(logTag, "----- Register residence -----");
        Log.d(logTag, "----- RFID: " + RFID + " -----");
        mRequest.registerResidence();


        Log.d(logTag, "----- Register User -----");
        TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        mRequest.registerUser(manager.getDeviceId()); // TODO There's another way..
    }

    private boolean alreadyRegistered() {
        return this.getPreferences(Context.MODE_PRIVATE).getBoolean("registered", false);
    }
}


