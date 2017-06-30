package nl.kimplusdelta.gereedschap.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.kimplusdelta.gereedschap.MyApplication;
import nl.kimplusdelta.gereedschap.R;
import nl.kimplusdelta.gereedschap.adapter.DeviceAdapter;
import nl.kimplusdelta.gereedschap.adapter.DeviceBaseAdapter;
import nl.kimplusdelta.gereedschap.adapter.EditDeviceAdapter;
import nl.kimplusdelta.gereedschap.firebase.InstrumentItem;
import nl.kimplusdelta.gereedschap.model.InstrumentDisplayItem;
import nl.kimplusdelta.gereedschap.utils.ConnectionUtils;

/**
 * Activity that shows a full list of data
 */
public class DeviceDetailsActivity extends AppCompatActivity {

    /**
     * Information for passing along the intent
     */
    public static final String EXTRA_ITEM = "item";
    public static final String EXTRA_BUNDLE = "bundle";
    public static final String EXTRA_ID = "ID";
    public static final String EXTRA_EDIT = "edit";

    @BindView(R.id.listView) ListView mListView;

    /**
     * Dynamic menu item because only a set of users are allowed to edit items
     */
    private static final int MENU_EDIT = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_CLOSE = Menu.FIRST + 2;

    /**
     * List adapter
     */
    private DeviceBaseAdapter mAdapter;

    /**
     * Device item retrieved from our database
     */
    private InstrumentItem mItem;

    /**
     * Parent node / device ID
     */
    private String mItemID;

    /**
     * Boolean to switch between edit mode(EditText) or view mode(TextViews)
     */
    private boolean mEditMode;

    /**
     * assign the variables from the intent
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_details);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mItemID = getIntent().getExtras().getString(EXTRA_ID);
        Bundle bundle = getIntent().getBundleExtra(EXTRA_BUNDLE);
        mItem = bundle.getParcelable(EXTRA_ITEM);
        if (mItem != null) {
            this.setTitle(mItemID);

            mEditMode = getIntent().getExtras().getBoolean(EXTRA_EDIT);
            buildList();
        } else {
            finish();
        }
    }

    /**
     * Pick the correct adapter based on mEditMode
     */
    private void buildList() {
        if(mEditMode) {
            mAdapter = new EditDeviceAdapter(this, mItem.toFullDisplayList(this));
        } else {
            mAdapter = new DeviceAdapter(this, mItem.toFullDisplayList(this));
        }

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener((adapterView, view, i, l) -> {
            InstrumentDisplayItem displayItem = mAdapter.getItem(i);
            if(displayItem != null) {
                if (displayItem.key.equals(InstrumentItem.S_PICTURES)) {
                    showGridActivity(GridActivity.TYPE_PHOTO);
                } else if (displayItem.key.equals(InstrumentItem.S_DOCUMENTS)) {
                    showGridActivity(GridActivity.TYPE_DOCS);
                }
            }
        });
    }

    /**
     * Start Picture or Attachment grid activity
     * @param gridType
     */
    private void showGridActivity(String gridType) {
        if(!ConnectionUtils.isConnected(this)) {
            ConnectionUtils.showNoConnectionDialog(this);
            return;
        }

        Intent intent = new Intent(DeviceDetailsActivity.this, GridActivity.class);
        intent.putExtra(GridActivity.KEY_ID, mItemID);
        intent.putExtra(GridActivity.KEY_TYPE, gridType);

        if(gridType.equals(GridActivity.TYPE_PHOTO)) {
            List<String> pictures = mItem.getPictures();
            intent.putExtra(GridActivity.KEY_DATA, pictures.toArray(new String[pictures.size()]));
        } else if(gridType.equals(GridActivity.TYPE_DOCS)) {
            List<String> attachments = mItem.getAttachments();
            intent.putExtra(GridActivity.KEY_DATA, attachments.toArray(new String[attachments.size()]));
        }

        startActivity(intent);
    }

    /**
     * Update the Database with only the changed values
     */
    private void saveEdits() {
        Map<String, Object> updateMap = new HashMap<>();
        for(int i = 0; i < mAdapter.getCount(); i++) {
            InstrumentDisplayItem displayItem = mAdapter.getItem(i);
            if(displayItem != null) {
                if (displayItem.value != null) {
                    String dbKey = InstrumentItem.localeToDBKey(displayItem.key);
                    String dbValue = displayItem.value;

                    updateMap.put(dbKey, dbValue);
                    mItem.updateValue(dbKey, dbValue);
                }
            }
        }

        FirebaseDatabase.getInstance().getReference("items")
                .child(((MyApplication)getApplication()).companyID)
                .child(mItemID)
                .updateChildren(updateMap);
    }

    /**
     * Dynamic menu based on mEditMode and user access-control
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(((MyApplication)getApplication()).canUserEdit) {
            menu.clear();
            if (!mEditMode) {
                menu.add(0, MENU_EDIT, Menu.NONE, "Edit").setIcon(R.drawable.ic_mode_edit).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else {
                menu.add(0, MENU_CLOSE, Menu.NONE, "Close").setIcon(R.drawable.ic_close).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                menu.add(0, MENU_SAVE, Menu.NONE, "Save").setIcon(R.drawable.ic_save).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case MENU_EDIT:
                if(ConnectionUtils.isConnected(this)) {
                    mEditMode = true;
                    invalidateOptionsMenu();
                    buildList();
                } else {
                    ConnectionUtils.showNoConnectionDialog(this);
                }
                break;
            case MENU_SAVE:
                if(!ConnectionUtils.isConnected(this)) {
                    ConnectionUtils.showNoConnectionDialog(this);
                    break;
                }

                saveEdits();
            case MENU_CLOSE:
                mEditMode = false;
                invalidateOptionsMenu();
                buildList();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}