package com.twlk.ximgpiker.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.twlk.ximgpiker.R;
import com.twlk.ximgpiker.base.BaseActivity;
import com.twlk.ximgpiker.helper.AlbumBitmapCacheHelper;
import com.twlk.ximgpiker.model.SingleImageModel;
import com.twlk.ximgpiker.view.ZoomImageView;

import java.util.ArrayList;

import butterknife.BindView;


public class PickBigImagesActivity extends BaseActivity implements ViewPager.OnPageChangeListener{

    Toolbar toolbar;
    ViewPager vpContent;
    CheckBox cb_choose_state;
    private MyViewPagerAdapter adapter;

    private ArrayList<SingleImageModel> allimages;
    ArrayList<String> picklist;
    /**
     * 当前选中的图片
     */
    private int currentPic;

    private int last_pics;
    private int total_pics;

    private boolean isFinish = false;

    /**
     * 选择的照片文件夹
     */
    public final static String EXTRA_DATA = "extra_data";
    /**
     * 所有被选中的图片
     */
    public final static String EXTRA_ALL_PICK_DATA = "extra_pick_data";
    /**
     * 当前被选中的照片
     */
    public final static String EXTRA_CURRENT_PIC = "extra_current_pic";
    /**
     * 剩余的可选择照片
     */
    public final static String EXTRA_LAST_PIC = "extra_last_pic";
    /**
     * 总的照片
     */
    public final static String EXTRA_TOTAL_PIC = "extra_total_pic";
    private MenuItem menuItem;
    private MenuInflater mi;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mi = getMenuInflater();
        mi.inflate(R.menu.menu_complete, menu);
        menuItem = menu.getItem(0);
        if (menuItem != null) {
            int selectCount = total_pics - last_pics;
            menuItem.setTitle(String.format(getString(R.string.choose_pic_finish_with_num), selectCount > 0 ? selectCount : 0, total_pics));
            if (total_pics - last_pics > 0) {
                menuItem.setEnabled(true);
            } else {
                menuItem.setEnabled(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_complete) {
            isFinish = true;
            finish();
        } else if (item.getItemId() == android.R.id.home) {
            isFinish = false;
            finish();
        }
        return true;
    }

    @Override
    protected int getLayoutId(@Nullable Bundle savedInstanceState) {
        return R.layout.activity_pick_big_images;
    }

    @Override
    protected void init() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        vpContent = (ViewPager) findViewById(R.id.vp_content);
        cb_choose_state = (CheckBox) findViewById(R.id.cb_choose_state);

        if (last_pics < total_pics) {
            int selectCount = total_pics - last_pics;
            menuItem.setTitle(String.format(getString(R.string.choose_pic_finish_with_num), selectCount > 0 ? selectCount : 0, total_pics));
        }
        allimages = (ArrayList<SingleImageModel>) getIntent().getSerializableExtra(EXTRA_DATA);
        picklist = (ArrayList<String>) getIntent().getSerializableExtra(EXTRA_ALL_PICK_DATA);
        if (picklist == null)
            picklist = new ArrayList<String>();

        currentPic = getIntent().getIntExtra(EXTRA_CURRENT_PIC, 0);

        last_pics = getIntent().getIntExtra(EXTRA_LAST_PIC, 0);
        total_pics = getIntent().getIntExtra(EXTRA_TOTAL_PIC, 9);

        setTitle(toolbar, (currentPic + 1) + "/" + getImagesCount());
        //如果该图片被选中
        if (getChooseStateFromList(currentPic)) {
            cb_choose_state.setChecked(true);
        } else {
            cb_choose_state.setChecked(false);
        }

        adapter = new MyViewPagerAdapter();
        vpContent.setAdapter(adapter);

        vpContent.setCurrentItem(currentPic);

        initListener();
    }

    protected void initListener() {
        vpContent.addOnPageChangeListener(this);
        cb_choose_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setImgIsCheck();
            }
        });
    }

    /**
     * 设置图片是否选中
     */
    private void setImgIsCheck() {
        toggleChooseState(currentPic);
        //如果被选中
        if (getChooseStateFromList(currentPic)) {
            if (last_pics <= 0) {
                cb_choose_state.setChecked(false);
                toggleChooseState(currentPic);
                Toast.makeText(PickBigImagesActivity.this, String.format(getString(R.string.choose_pic_num_out_of_index), total_pics), Toast.LENGTH_SHORT).show();
                return;
            }
            picklist.add(getPathFromList(currentPic));
            last_pics--;
            if (total_pics - last_pics > 0) {
                menuItem.setEnabled(true);
            }
            cb_choose_state.setChecked(true);
            int selectCount = total_pics - last_pics;
            menuItem.setTitle(String.format(getString(R.string.choose_pic_finish_with_num), selectCount > 0 ? selectCount : 0, total_pics));
        } else {
            picklist.remove(getPathFromList(currentPic));
            last_pics++;
            cb_choose_state.setChecked(false);
            if (last_pics == total_pics) {
                menuItem.setEnabled(false);
                menuItem.setTitle(getString(R.string.choose_pic_finish));
            } else {
                if (total_pics - last_pics > 0) {
                    menuItem.setEnabled(true);
                } else {
                    menuItem.setEnabled(false);
                }
                int selectCount = total_pics - last_pics;
                menuItem.setTitle(String.format(getString(R.string.choose_pic_finish_with_num), selectCount > 0 ? selectCount : 0, total_pics));
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        //如果该图片被选中
        if (getChooseStateFromList(position)) {
            cb_choose_state.setChecked(true);
        } else {
            cb_choose_state.setChecked(false);
        }
        currentPic = position;
        setTitle(toolbar, (currentPic + 1) + "/" + getImagesCount());
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private class MyViewPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return getImagesCount();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(PickBigImagesActivity.this).inflate(R.layout.widget_zoom_iamge, null);
            final ZoomImageView zoomImageView = (ZoomImageView) view.findViewById(R.id.zoom_image_view);
            AlbumBitmapCacheHelper.getInstance().addPathToShowlist(getPathFromList(position));
            zoomImageView.setTag(getPathFromList(position));
            Bitmap bitmap = AlbumBitmapCacheHelper.getInstance().getBitmap(getApplicationContext(), getPathFromList(position), 0, 0, new AlbumBitmapCacheHelper.ILoadImageCallback() {
                @Override
                public void onLoadImageCallBack(Bitmap bitmap, String path, Object... objects) {
                    ZoomImageView view = ((ZoomImageView) vpContent.findViewWithTag(path));
                    if (view != null && bitmap != null)
                        ((ZoomImageView) vpContent.findViewWithTag(path)).setSourceImageBitmap(bitmap, PickBigImagesActivity.this);
                }
            }, position);
            if (bitmap != null) {
                zoomImageView.setSourceImageBitmap(bitmap, PickBigImagesActivity.this);
            }
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
            AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(getPathFromList(position));
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    /**
     * 通过位置获取该位置图片的path
     */
    private String getPathFromList(int position) {
        return allimages.get(position).path;
    }

    /**
     * 通过位置获取该位置图片的选中状态
     */
    private boolean getChooseStateFromList(int position) {
        return allimages.get(position).isPicked;
    }

    /**
     * 反转图片的选中状态
     */
    private void toggleChooseState(int position) {
        allimages.get(position).isPicked = !allimages.get(position).isPicked;
    }

    /**
     * 获得所有的图片数量
     */
    private int getImagesCount() {
        return allimages.size();
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra("pick_data", picklist);
        data.putExtra("isFinish", isFinish);
        setResult(RESULT_OK, data);
        super.finish();
    }
}
