package nl.kimplusdelta.gereedschap.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.kimplusdelta.gereedschap.MyApplication;
import nl.kimplusdelta.gereedschap.R;
import nl.kimplusdelta.gereedschap.adapter.FullScreenImageAdapter;

/**
 * Download the images and show them in a form of a gallery.
 * This class is an presenting view. When a image is removed the activity will
 * return the image data to its parent so the parent can remove the image accordingly.
 */
public class ImagePreview extends AppCompatActivity {

    /**
     * KEY: List of all the image URLs
     */
    public static final String KEY_IMAGES = "KEY_IMAGES";

    /**
     * KEY: index of the photo that is selected
     */
    public static final String KEY_POSITION = "KEY_POSITION";

    /**
     * Dynamic menu item for removing photos if the user has the correct access
     */
    private static final int MENU_REMOVE = Menu.FIRST;

    /**
     * ViewPager to create a gallery like view
     */
    @BindView(R.id.viewpager) ViewPager mViewPager;

    /**
     * List of all the image URLs
     */
    private List<String> mImages;

    /**
     * index of the photo that is selected, which photo should be presented first
     */
    private int mPosition;

    /**
     * Update the title bar to display the shown images index
     */
    private final ViewPager.OnPageChangeListener viewPagerOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
        @Override
        public void onPageScrollStateChanged(int state) {}
        @Override
        public void onPageSelected(int position) {
            if (mViewPager != null) {
                mViewPager.setCurrentItem(position);
                setActionBarTitle(position);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mImages = extras.getStringArrayList(KEY_IMAGES);
                mPosition = extras.getInt(KEY_POSITION);
            }
        }

        setUpViewPager();
    }

    private void setUpViewPager() {
        mViewPager.setAdapter(new FullScreenImageAdapter(getApplicationContext(), mImages));
        mViewPager.addOnPageChangeListener(viewPagerOnPageChangeListener);
        mViewPager.setCurrentItem(mPosition);

        setActionBarTitle(mPosition);
    }

    /**
     * Create a dynamic menu according to the users access-controls.
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(((MyApplication)getApplication()).canUserEdit) {
            menu.clear();
            menu.add(0, MENU_REMOVE, Menu.NONE, "Remove").setIcon(R.drawable.ic_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * return the index of the item to the previous activity so it could get removed
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case MENU_REMOVE:
                Bundle conData = new Bundle();
                conData.putInt("id", mViewPager.getCurrentItem());
                Intent intent = new Intent();
                intent.putExtras(conData);
                setResult(RESULT_OK, intent);
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Format the actionbar title. e.g. 1/10
     * @param position of the view that is currently shown
     */
    private void setActionBarTitle(int position) {
        if (mViewPager != null && mImages.size() > 1) {
            int totalPages = mViewPager.getAdapter().getCount();

            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null){
                actionBar.setTitle(String.format("%d/%d", (position + 1), totalPages));
            }
        }
    }
}
