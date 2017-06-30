package nl.kimplusdelta.gereedschap.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import nl.kimplusdelta.gereedschap.BuildConfig;
import nl.kimplusdelta.gereedschap.MyApplication;
import nl.kimplusdelta.gereedschap.R;
import nl.kimplusdelta.gereedschap.adapter.ImageAdapter;
import nl.kimplusdelta.gereedschap.firebase.InstrumentItem;
import nl.kimplusdelta.gereedschap.utils.CameraUtils;
import nl.kimplusdelta.gereedschap.utils.ImageResizeHelper;

/**
 * Simple grid which shows either the DOCS or the PICS from Firebase.
 * The activity can download and present existing data, add new data, remove data
 * and once clicked will call the correct full screen preview activity.
 */
public class GridActivity extends AppCompatActivity {

    /**
     * Intent data
     */
    public static final String KEY_DATA = "data";
    public static final String KEY_TYPE = "type";
    public static final String KEY_ID = "ID";

    /**
     * static fields to check if which data we should present.
     */
    public static final String TYPE_PHOTO = "pictures";
    public static final String TYPE_DOCS = "attachments";

    /**
     * Dynamic menu for removing data
     */
    private static final int MENU_REMOVE = Menu.FIRST;

    /**
     * Flags for catching the results from intents
     */
    private static final int RESULT_NEW_IMG = 0;
    private static final int RESULT_EXISTING_IMG = 1;
    private static final int RESULT_REMOVE_IMG = 2;

    /**
     * Permission flag since we need the camera permission to take pictures
     */
    private static final int TAKE_PHOTO_REQUEST = 0;

    /**
     * GridView which present the DOCS or PICS
     */
    @BindView(R.id.gridView) GridView mGridView;

    /**
     * Add new pictures. The add button only accepts PICS so it will be hidden when
     * we display DOCS
     */
    @BindView(R.id.addNewButton) Button mAddButton;

    /**
     * Add existing DOCS or PICS
     */
    @BindView(R.id.addExistingButton) Button mExistingButton;

    /**
     * Adapter for handling the GridView
     */
    private ImageAdapter mAdapter;

    /**
     * Data type: DOCS or PICS
     */
    private String mType;

    /**
     * ItemID Ref for adding/removing items
     */
    private String mItemID;

    /**
     * Storage Field for downloading/uploading data
     */
    private StorageReference mStorageRef;

    /**
     * List with all the grid data
     */
    private List<String> mData;

    /**
     * Class field used for adding a new picture
     */
    private Uri mMediaUri;

    /**
     * Index used for context menu
     */
    private int mSelectedIndex;

    /**
     * Load the view. Set the variables and flag the activity as DOCS or PICS.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        mStorageRef = storage.getReferenceFromUrl("gs://gereedschap-app.appspot.com/");

        mItemID = getIntent().getExtras().getString(KEY_ID);
        mType = getIntent().getExtras().getString(KEY_TYPE);
        mAdapter = new ImageAdapter(this, mType);
        mGridView.setAdapter(mAdapter);

        // not allowed to alter
        if(!((MyApplication)getApplication()).canUserEdit) {
            mAddButton.setVisibility(View.GONE);
            mExistingButton.setVisibility(View.GONE);
        } else {
            registerForContextMenu(mGridView);
        }

        String[] data = getIntent().getExtras().getStringArray(KEY_DATA);
        if(data != null) {
            mData = new ArrayList<>(Arrays.asList(data));
        }

        if(mType != null) {
            if (mType.equals(TYPE_PHOTO)) {
                setTitle(InstrumentItem.S_PICTURES);
                handlePhotos();
            } else {
                setTitle(InstrumentItem.S_DOCUMENTS);
                handleDocs();

                mExistingButton.setVisibility(View.GONE);
            }
        }

        mGridView.setOnItemClickListener((adapterView, view, i, l) -> {
            if(mType.equals(TYPE_PHOTO)) {
                Intent intent = new Intent(GridActivity.this, ImagePreview.class);
                intent.putExtra(ImagePreview.KEY_POSITION, i);
                intent.putExtra(ImagePreview.KEY_IMAGES, mAdapter.getItems());
                startActivityForResult(intent, RESULT_REMOVE_IMG);
            } else {
                Intent intent = new Intent(GridActivity.this, DocumentPreview.class);
                intent.putExtra(DocumentPreview.KEY_DOC, mAdapter.getItem(i));
                startActivity(intent);
            }
        });
    }

    /**
     * Context menu (Long Press Grid Item) for removing data
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, MENU_REMOVE, 0, R.string.remove_item);

        // Get the list item position
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        mSelectedIndex = info.position;
    }

    /**
     * Context menu callback
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getItemId() == MENU_REMOVE) {
            removeFile();
            return true;
        }
        return false;
    }

    /**
     * Remove the file from our GridView, Firebase Database and Firebase Storage
     */
    private void removeFile() {
        String selectedData = mData.get(mSelectedIndex);
        mData.remove(selectedData);

        mAdapter.removeItem(mSelectedIndex);
        mAdapter.notifyDataSetChanged();

        saveEdits();

        // remove from storage
        final String companyID = ((MyApplication) getApplication()).companyID;
        final String path = companyID + "/" + selectedData;
        mStorageRef.child(path).delete();
    }

    /**
     * We check if we should add a new file or remove a file.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == RESULT_NEW_IMG) {
                Uri uri = mMediaUri;
                if(mType.equals(TYPE_PHOTO)) {
                    File file;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri outputPath = CameraUtils.getOutputMediaFileUri(this, getString(R.string.app_name));
                        file = new ImageResizeHelper(mMediaUri).resizeFromContent(this, outputPath.getPath());
                    } else {
                        file = new ImageResizeHelper(mMediaUri).resize();
                    }

                    if(file != null) {
                        uri = Uri.fromFile(file);
                    }
                }
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(uri);
                this.sendBroadcast(mediaScanIntent);
                addItem(uri);
            } else if(requestCode == RESULT_EXISTING_IMG) {
                Uri uri = data.getData();
                if(mType.equals(TYPE_PHOTO)) {
                    mMediaUri = CameraUtils.getOutputMediaFileUri(this, getString(R.string.app_name));
                    File file = new ImageResizeHelper(data.getData()).resizeFromGallery(this, mMediaUri.getPath());
                    if(file != null) {
                        uri = Uri.fromFile(file);
                    }
                }
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(uri);
                this.sendBroadcast(mediaScanIntent);
                addItem(uri);
            } else if(requestCode == RESULT_REMOVE_IMG) {
                int id = data.getIntExtra("id" , -1);
                if(id != -1) {
                    mSelectedIndex = id;
                    removeFile();
                }
            }
        }
    }

    /**
     * Callback when in need for permissions. We use the Camera permission for taking pictures
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case TAKE_PHOTO_REQUEST: {
                boolean finalResult = true;
                for(int result : grantResults) {
                    if(result != PackageManager.PERMISSION_GRANTED) {
                        finalResult = false;
                        break;
                    }
                }
                if(finalResult) {
                    launchCameraIntent();
                } else {
                    Toast.makeText(this, getString(R.string.we_need_the_permissions), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Add an item by Uri. Generate an unique ID for each item added to Firebase.
     * Custom Metadata will help us keep the name once the filename is hashed.
     * Upload the item to Firebase Storage.
     * @param uri of the local item
     */
    private void addItem(Uri uri) {
        if(uri == null) {
            return;
        }

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(getContentResolver().getType(uri))
                .setCustomMetadata("filename", getFileNameFromUri(uri))
                .build();

        final String companyID = ((MyApplication) getApplication()).companyID;
        final String filename = generateGUID();
        final String path = companyID + "/" + filename;

        mStorageRef.child(path).putFile(uri, metadata).addOnSuccessListener(taskSnapshot -> {
            mData.add(filename);

            if(mType.equals(TYPE_DOCS)) {
                mAdapter.addItem(filename);
                mAdapter.notifyDataSetChanged();
            } else {
                mAdapter.addItem(null);
                mAdapter.notifyDataSetChanged();

                downloadPhoto(mAdapter.getCount() - 1, companyID);
            }

            saveEdits();
        });
    }

    /**
     * Update the changes of the current item in the Firebase database
     */
    private void saveEdits() {
        FirebaseDatabase.getInstance().getReference("items")
                .child(((MyApplication)getApplication()).companyID)
                .child(mItemID)
                .child(mType)
                .setValue(mData);
    }

    @OnClick(R.id.addNewButton)
    public void onAddItemClicked() {
        if(mType.equals(TYPE_PHOTO)) {
            onAddNewItem();
        } else {
            onAddExistingItem();
        }
    }

    @OnClick(R.id.addExistingButton)
    public void onAddExistingClicked() {
        onAddExistingItem();
    }

    /**
     * Check for the correct permissions before launching the camera intent
     */
    private void onAddNewItem() {
        if(mType.equals(TYPE_PHOTO)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, TAKE_PHOTO_REQUEST);
            } else {
                launchCameraIntent();
            }
        }
    }

    /**
     * Generate an unique Uri and let the OS handle the camera intent
     */
    private void launchCameraIntent() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File imagePath = new File(this.getFilesDir(), "images");
            if (!imagePath.exists()) imagePath.mkdirs();
            File newFile = new File(imagePath, "image.jpg");
            mMediaUri = FileProvider.getUriForFile(this, getProviderUrl(), newFile);
        } else {
            mMediaUri = CameraUtils.getOutputMediaFileUri(this, getString(R.string.app_name));
        }

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
            takePhotoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            List<ResolveInfo> resInfoList = getPackageManager()
                    .queryIntentActivities(takePhotoIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, mMediaUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        }

        if (mMediaUri != null) {
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
            startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQUEST);
        }
    }

    private void onAddExistingItem() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if(mType.equals(TYPE_PHOTO)) {
            intent.setType("image/*");
        } else {
            intent.setType("application/pdf");
        }

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.add_existing_item_chooser_title)), RESULT_EXISTING_IMG);
    }

    /**
     * Simple display the items. no need for downloading any documents in this view
     */
    private void handleDocs() {
        if(mData != null) {
            // add placeholder items
            for (String item : mData) {
                mAdapter.addItem(item);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Simple display placeholders while we download the photos from Firebase
     */
    private void handlePhotos() {
        if(mData != null) {
            // add placeholder items
            for (String ignored : mData) {
                mAdapter.addItem(null);
            }
            mAdapter.notifyDataSetChanged();

            String companyID = ((MyApplication) getApplication()).companyID;
            for (int i = 0; i < mData.size(); i++) {
                downloadPhoto(i, companyID);
            }
        }
    }

    /**
     * Download the image from firebase and replace the placeholder item in the adapter with the picture
     * @param index of the placeholder in the adapter
     * @param companyID users company for download reference
     */
    private void downloadPhoto(final int index, String companyID) {
        StorageReference pathReference = mStorageRef.child(companyID + "/" + mData.get(index));
        pathReference.getDownloadUrl().addOnSuccessListener(uri -> mAdapter.replaceItem(index, uri.toString()));
    }

    /**
     * @return Random generated file name
     */
    private String generateGUID() {
        return randomGUIDString() + randomGUIDString() + '-' + randomGUIDString() + '-' + randomGUIDString() + '-' +
                randomGUIDString() + '-' + randomGUIDString() + randomGUIDString() + randomGUIDString();
    }

    private String randomGUIDString() {
        return formatDoubles(Math.floor((1 + Math.random()) * 0x10000))
                .substring(1);
    }

    /**
     * Helper to read the actual file name from an Uri using the contentResolver
     * @param contentUri file Uri
     * @return actual Filename
     */
    private String getFileNameFromUri(Uri contentUri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(contentUri, null, null, null, null);
            if(cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private String formatDoubles(double d) {
        if(d == (long) d)
            return String.format("%d",(long)d);
        else
            return String.format("%s",d);
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

    private String getProviderUrl() {
        return BuildConfig.APPLICATION_ID + ".provider";
    }
}
