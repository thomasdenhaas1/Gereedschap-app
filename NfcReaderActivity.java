package nl.kimplusdelta.gereedschap.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.kimplusdelta.gereedschap.MyApplication;
import nl.kimplusdelta.gereedschap.R;
import nl.kimplusdelta.gereedschap.firebase.FirebaseHelper;

/**
 * Start an NFC adapter to listen to NFC dispatches
 */
public class NfcReaderActivity extends AppCompatActivity {

    /**
     * NfcAdapter with variables to catch nfc dispatches
     */
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mIntentFilters;
    private String[][] mNFCTechLists;

    /**
     * A list to hold all the checked IDs so we don't check the same ID multiple times
     */
    private List<String> mCheckedIDs;

    /**
     * Setup this activity with an NfcAdapter
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_reader);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mCheckedIDs = new ArrayList<>();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, R.string.no_nfc_available, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, R.string.nfc_disabled, Toast.LENGTH_LONG).show();
        }

        // create an intent with tag data and deliver to this activity
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // set an intent filter for all MIME data
        IntentFilter ndefIntent = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefIntent.addDataType("*/*");
            mIntentFilters = new IntentFilter[] { ndefIntent };
        } catch (Exception e) {
            Log.e("TagDispatch", e.toString());
        }

        mNFCTechLists = new String[][] { new String[] { NfcF.class.getName() } };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilters, mNFCTechLists);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    /**
     * Callback when a NFC tag is found
     * Parse the data and check the serial against our database
     *
     * @param intent which holds the NFC tag data
     */
    @Override
    public void onNewIntent(Intent intent) {
        ArrayList<String> returnList = new ArrayList<>();

        // parse through all NDEF messages and their records and pick text type only
        Parcelable[] data = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (data != null) {
            try {
                for (Parcelable aData : data) {
                    NdefRecord[] recs = ((NdefMessage) aData).getRecords();
                    for (NdefRecord rec : recs) {
                        if (rec.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                                Arrays.equals(rec.getType(), NdefRecord.RTD_TEXT)) {

                            byte[] payload = rec.getPayload();
                            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
                            int langCodeLen = payload[0] & 63;

                            returnList.add(new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1, textEncoding));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("TagDispatch", e.toString());
            }
        }

        if(returnList.size() > 0) {
            final String serial = returnList.get(0);

            // prevent checking the same tag many times
            if(!mCheckedIDs.contains(serial)) {
                mCheckedIDs.add(serial);

                FirebaseHelper.startItemActivity(this, serial, true);
            } else {
                ((MyApplication)getApplication()).showToast(R.string.device_not_found);
            }
        }
    }
}
