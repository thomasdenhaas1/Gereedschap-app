package nl.kimplusdelta.gereedschap.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import nl.kimplusdelta.gereedschap.MyApplication;
import nl.kimplusdelta.gereedschap.R;
import nl.kimplusdelta.gereedschap.utils.ConnectionUtils;
import timber.log.Timber;

/**
 * The first Activity that is launched when the app starts
 * We try to log the user in from cache or else prompt the user with data fields to manually log in
 */
public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.username) EditText mUserNameEditText;
    @BindView(R.id.password) EditText mPasswordEditText;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    /**
     * Init the view
     * Load the username from shared prefs
     * Request focus to the correct view
     * Initialize firebase
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        // use old username
        SharedPreferences sharedPref = getSharedPreferences("Login", MODE_PRIVATE);
        String username = sharedPref.getString("username", "");
        mUserNameEditText.setText(username);

        if(!username.equals("")) {
            mPasswordEditText.requestFocus();
        }

        initFirebaseAuth();
    }

    /**
     * Initialize the Firebase Auth
     * Check if we can auto log in the user from cache
     */
    private void initFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                Timber.d("onAuthStateChanged:signed_in:%s", user.getUid());
                loginSuccessful();
            } else {
                Timber.d("onAuthStateChanged:signed_out");
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    /**
     * Log in button is clicked
     * Get the username & password from the fields and check for correctness
     */
    @OnClick(R.id.login)
    public void onEmailLogin() {
        clearErrors();

        // Store values at the time of the login attempt.
        String username = mUserNameEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            mPasswordEditText.setError(getString(R.string.error_field_required));
            focusView = mPasswordEditText;
            cancel = true;
        }
        // Check for a valid username address.
        if (TextUtils.isEmpty(username)) {
            mUserNameEditText.setError(getString(R.string.error_field_required));
            focusView = mUserNameEditText;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            login(username.toLowerCase(Locale.getDefault()), password);
        }
    }

    /**
     * Check for an internet connection and start the login method
     * @param username
     * @param password
     */
    private void login(String username, String password) {
        if(!ConnectionUtils.isConnected(this)) {
            ConnectionUtils.showNoConnectionDialog(this);
            return;
        }

        if(username.isEmpty() || password.isEmpty()) {
            loginUnSuccessful();
        } else {
            passwordAuth(username, password);
        }
    }

    /**
     * Start the sign in with the username & password
     * @param username
     * @param password
     */
    private void passwordAuth(String username, String password) {
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        loginUnSuccessful();
                    }
                });
    }

    /**
     * Store the username to shared pref for when the user returns to this page
     * Start the main activity
     */
    protected void loginSuccessful() {
        // Save username
        SharedPreferences sharedPref = getSharedPreferences("Login", MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString("username", mUserNameEditText.getText().toString());
        prefEditor.apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Toast the user to indicate that the log in was incorrect
     */
    protected void loginUnSuccessful() {
        ((MyApplication)getApplication()).showToast(R.string.user_pass_invalid);
    }

    private void clearErrors() {
        mUserNameEditText.setError(null);
        mPasswordEditText.setError(null);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Timber.e("ConnectionFailed %s", connectionResult.getErrorMessage());
    }
}
