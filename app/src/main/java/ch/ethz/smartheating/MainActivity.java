package ch.ethz.smartheating;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
        mManualScanButton = (Button)findViewById(R.id.manualScanButton);
        mWelcomeTextView = (TextView)findViewById(R.id.welcomeTextView);

        mWelcomeTextView.setText(R.string.welcome_text);

        mManualScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent manualScanIntent = new Intent(getApplicationContext(), ManualScanActivity.class);
                startActivity(manualScanIntent);
            }
        });

        Intent homeIntent = new Intent(this, HomeActivity.class);
        startActivity(homeIntent);

        /** Removed for DEBUG purposes.

        if (mNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;

        }
         **/
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        NdefMessage[] msgs = null;

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Log.d(logTag, "Action ndef discovered.");
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    Log.d(logTag, "Message parsed.");
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
                for (NdefMessage m : msgs) {
                    registerPi (new String(msgs[0].getRecords()[0].getPayload()).trim().substring(10));
                }
            }
        }
    }

    private void registerPi(String UUID) {
        // TODO: get MMEI from phone, register user with the UUID from the Pi.

        Request mRequest = new Request();

        Log.d(logTag, "----- Register residence -----");
        Log.d(logTag, "----- UUID: " + UUID + " -----");
        mRequest.registerResidence(UUID);

        Log.d(logTag, "----- Register User -----");
        TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        mRequest.registerUser(manager.getDeviceId(), UUID);

        // TODO: Advanced: Check if UUID is correct on server, prompt user to reenter if it was done manually.
    }

    private boolean alreadyRegistered () {
        return this.getPreferences(Context.MODE_PRIVATE).getBoolean("registered", false);
    }
}


