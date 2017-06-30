package nl.kimplusdelta.gereedschap.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;
import nl.kimplusdelta.gereedschap.firebase.FirebaseHelper;

public class QrScannerActivity extends AppCompatActivity implements ZBarScannerView.ResultHandler {

    /**
     * QR / Barcode scanner
     */
    private ZBarScannerView mScannerView;

    /**
     * A list to hold all the checked IDs so we don't check the same ID multiple times
     */
    private List<String> mCheckedIDs;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mCheckedIDs = new ArrayList<>();

        mScannerView = new ZBarScannerView(this);
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
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

    /**
     * Callback when a QR/barcode is found
     * Check the serial against our database
     *
     * @param rawResult which holds the barcode data
     */
    @Override
    public void handleResult(Result rawResult) {
        final String serial = rawResult.getContents();

        // prevent checking the same tag many times
        if (!mCheckedIDs.contains(serial)) {
            mCheckedIDs.add(serial);

            if(!serial.isEmpty()) {
                FirebaseHelper.startItemActivity(this, serial, true);
            }
        }

        mScannerView.resumeCameraPreview(QrScannerActivity.this);
    }
}
