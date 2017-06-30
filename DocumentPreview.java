package nl.kimplusdelta.gereedschap.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

import nl.kimplusdelta.gereedschap.MyApplication;
import nl.kimplusdelta.gereedschap.R;

/**
 * Download the document from the backend and start an intent to handle the file
 */
public class DocumentPreview extends AppCompatActivity {

    public static final String KEY_DOC = "doc";
    private boolean mCloseOnResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_preview);

        String item = (String) getIntent().getExtras().get(KEY_DOC);
        if(item != null) {
            setTitle(item);

            try {
                final File localFile = File.createTempFile("gereedschap-app", null, this.getExternalCacheDir());
                final String companyID = ((MyApplication) getApplication()).companyID;

                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://gereedschap-app.appspot.com/");
                StorageReference pathReference = storageRef.child(companyID + "/" + item);
                pathReference.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                    mCloseOnResume = true;

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(localFile), "application/pdf");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(Intent.createChooser(intent, "open PDF"));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mCloseOnResume) {
            finish();
        }
    }
}
