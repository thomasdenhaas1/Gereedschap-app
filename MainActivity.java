package nl.kimplusdelta.gereedschap.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import nl.kimplusdelta.gereedschap.MyApplication;
import nl.kimplusdelta.gereedschap.R;
import nl.kimplusdelta.gereedschap.firebase.FirebaseHelper;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    /**
     * EditText where the user can fill in his serial number
     */
    @BindView(R.id.serialNumber) EditText mSerialNumber;

    /**
     * Get the CompanyID from the database to use in the item path
     */
    private String mCompanyId = "";


    /**
     * Start the activity and retrieve the CompanyID from the singed in user
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        // retrieve company id
        mDatabase.child("users").child(getUid()).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mCompanyId = (String) dataSnapshot.child("company").getValue();
                        ((MyApplication)getApplication()).setCompanyID(mCompanyId);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Timber.w(databaseError.toException(), "getUser:onCancelled");
                        finish();
                    }
                });

    }

    /**
     * Camera permission callback called for the QR activity.
     * Launch the QR activity if the permission request was a success.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 0: {
                boolean finalResult = true;
                for(int result : grantResults) {
                    if(result != PackageManager.PERMISSION_GRANTED) {
                        finalResult = false;
                        break;
                    }
                }
                if(finalResult) {
                    showQR();
                } else {
                    Toast.makeText(this, R.string.we_need_the_permissions, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Reset the text of the serial number every time we click on the field
     */
    @OnClick(R.id.serialNumber)
    public void resetSerialOnClick() {
        mSerialNumber.setText("");
    }

    /**
     * Check if the camera permission is enabled, else prompt the user with the permission dialog
     */
    @OnClick(R.id.qrscan)
    public void onQRClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean externalStoragePermission = ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            if (!externalStoragePermission) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
            } else {
                showQR();
            }
        } else {
            showQR();
        }
    }

    /**
     * Start the NCF activity
     */
    @OnClick(R.id.nfcscan)
    public void onNFCClicked() {
        Intent intent = new Intent(MainActivity.this, NfcReaderActivity.class);
        startActivity(intent);
    }

    /**
     * Check the serial filled in by the user is empty and handle the request
     */
    @OnClick(R.id.go)
    public void onGoClicked() {
        final String itemSerial = mSerialNumber.getText().toString();
        if(itemSerial.isEmpty()) {
            ((MyApplication)getApplication()).showToast(R.string.device_not_found);
        } else {
            checkItem(itemSerial);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_logout:
                FirebaseAuth.getInstance().signOut();
                navigateToLogin();
                return true;
            case R.id.action_privacy_policy:
                navigateToPrivacyPolicy();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Call the helper class to check the serial against the database entries
     * @param serial item ID
     */
    private void checkItem(String serial) {
        FirebaseHelper.startItemActivity(this, serial, false);
    }

    /**
     * Start QR Scanner activity
     */
    private void showQR() {
        Intent intent = new Intent(MainActivity.this, QrScannerActivity.class);
        startActivity(intent);
    }

    /**
     * Start Privacy Policy activity
     */
    private void navigateToPrivacyPolicy() {
        Intent intent = new Intent(MainActivity.this, PrivacyPolicyActivity.class);
        startActivity(intent);
    }

    /**
     * Return to the Inlog activity when the user logged out
     */
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    /**
     * Get the userID as a non-null string
     * @return non-null userID
     */
    private String getUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            return user.getUid();
        } else {
            return "";
        }
    }


}
