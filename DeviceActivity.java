package nl.kimplusdelta.gereedschap.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import nl.kimplusdelta.gereedschap.MyApplication;
import nl.kimplusdelta.gereedschap.R;
import nl.kimplusdelta.gereedschap.adapter.DeviceAdapter;
import nl.kimplusdelta.gereedschap.firebase.InstrumentItem;
import nl.kimplusdelta.gereedschap.utils.ConnectionUtils;

/**
 * Activity that shows a short list of data
 */
public class DeviceActivity extends AppCompatActivity {

    /**
     * Information for passing along the intent
     */
    public static final String EXTRA_ITEM = "item";
    public static final String EXTRA_BUNDLE = "bundle";
    public static final String EXTRA_ID = "ID";

    /**
     * Dynamic menu item because only a set of users are allowed to edit items
     */
    private static final int MENU_EDIT = Menu.FIRST;

    @BindView(R.id.listView) ListView mListView;

    /**
     * Device item retrieved from our database
     */
    private InstrumentItem mItem;

    /**
     * Parent node / device ID
     */
    private String mID;

    /**
     * assign the variables from the intent and start the adapter
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mID = getIntent().getExtras().getString(EXTRA_ID);
        Bundle bundle = getIntent().getBundleExtra(EXTRA_BUNDLE);
        mItem = bundle.getParcelable(EXTRA_ITEM);
        if(mItem != null) {
            this.setTitle(mID);
            mListView.setAdapter(new DeviceAdapter(this, mItem.toShortDisplayList()));
        } else {
            finish();
        }
    }

    /**
     * Add an Edit button to the navbar if the user is allowed to edit items
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(((MyApplication)getApplication()).canUserEdit) {
            menu.clear();
            menu.add(0, MENU_EDIT, Menu.NONE, "Edit").setIcon(R.drawable.ic_mode_edit).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
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
                    showMoreInfo(true);
                } else {
                    ConnectionUtils.showNoConnectionDialog(this);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.infoButton)
    public void onMoreInfoClicked() {
        showMoreInfo(false);
    }

    /**
     * Show a full list
     * @param edit if we start in edit mode or not
     */
    private void showMoreInfo(boolean edit) {
        Intent intent = new Intent(DeviceActivity.this, DeviceDetailsActivity.class);
        intent.putExtra(DeviceDetailsActivity.EXTRA_ID, mID);
        intent.putExtra(DeviceDetailsActivity.EXTRA_EDIT, edit);

        Bundle bundle = new Bundle();
        bundle.putParcelable(DeviceDetailsActivity.EXTRA_ITEM, mItem);
        intent.putExtra(DeviceDetailsActivity.EXTRA_BUNDLE, bundle);

        startActivity(intent);

        finish();
    }
}
