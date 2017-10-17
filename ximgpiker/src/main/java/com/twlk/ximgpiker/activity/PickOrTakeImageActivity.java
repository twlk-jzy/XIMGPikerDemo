package com.twlk.ximgpiker.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.twlk.ximgpiker.R;
import com.twlk.ximgpiker.base.BaseActivity;
import com.twlk.ximgpiker.helper.AlbumBitmapCacheHelper;
import com.twlk.ximgpiker.model.ImageDirectoryModel;
import com.twlk.ximgpiker.model.SingleImageModel;
import com.twlk.ximgpiker.util.CommonUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class PickOrTakeImageActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener,
        AbsListView.OnScrollListener, View.OnTouchListener {

    Toolbar toolbar;
    GridView gridView;
    TextView tvDate;
    RelativeLayout rlDate;
    ListView lvDirectories;
    RelativeLayout rlChooseDirectory;
    TextView tvChooseImageDirectory;
    View vLine;
    TextView tvPreview;
    RelativeLayout rlBottom;
    /**
     * 按时间排序的所有图片list
     */
    private ArrayList<SingleImageModel> allImages;
    /**
     * 按目录排序的所有图片list
     */
    private ArrayList<SingleImageDirectories> imageDirectories;
    /**
     * 选中图片的信息
     */
    ArrayList<String> picklist = new ArrayList<String>();

    /**
     * 当前显示的文件夹路径，全部-- -1
     */
    private int currentShowPosition;

    private int firstVisibleItem = 0;
    private int currentState = SCROLL_STATE_IDLE;
    private int currentTouchState = MotionEvent.ACTION_UP;


    private LayoutInflater inflater = null;
    private GridViewAdapter adapter;

    //拍照的文件的文件名
    String tempPath = null;

    /**
     * 选择文件夹的弹出框
     */
    private ListviewAdapter listviewAdapter;

    private ObjectAnimator animation;
    private ObjectAnimator reverseanimation;

    private Animation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);

    /**
     * 每张图片需要显示的高度和宽度
     */
    private int perWidth;

    /**
     * 选择图片的数量总数，默认为9
     */
    private int picNums = 9;
    /**
     * 当前选中的图片数量
     */
    private int currentPicNums = 0;

    /**
     * 最新一张图片的时间
     */
    private long lastPicTime = 0;

    public static final String EXTRA_NUMS = "extra_nums";
    public static final int CODE_FOR_PIC_BIG = 1;
    public static final int CODE_FOR_PIC_BIG_PREVIEW = 2;
    public static final int CODE_FOR_TAKE_PIC = 3;
    public static final int CODE_FOR_WRITE_PERMISSION = 100;
    private static final int REQUEST_CAMERA = 0x00;
    private MenuItem menuItem;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_complete, menu);
        menuItem = menu.getItem(0);
        menuItem.setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_complete) {
            returnDataAndClose();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getLayoutId(@Nullable Bundle savedInstanceState) {
        return R.layout.activity_pick_or_take_image_activity;
    }


    @Override
    protected void init() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        gridView = (GridView) findViewById(R.id.gv_content);
        tvDate = (TextView) findViewById(R.id.tv_date);
        tvChooseImageDirectory = (TextView) findViewById(R.id.tv_choose_image_directory);
        rlDate = (RelativeLayout) findViewById(R.id.rl_date);
        lvDirectories = (ListView) findViewById(R.id.lv_directories);
        rlChooseDirectory = (RelativeLayout) findViewById(R.id.rl_choose_directory);
        vLine = findViewById(R.id.v_line);
        tvPreview = (TextView) findViewById(R.id.tv_preview);
        rlBottom = (RelativeLayout) findViewById(R.id.rl_bottom);

        setTitle(toolbar, "选择图片");

        listviewAdapter = new ListviewAdapter();

        inflater = LayoutInflater.from(this);

        allImages = new ArrayList<>();
        imageDirectories = new ArrayList<>();
        //默认显示全部图片
        currentShowPosition = -1;
        adapter = new GridViewAdapter();
        getAllImages();

        tvChooseImageDirectory.setText(getString(R.string.all_pic));
        tvPreview.setText(getString(R.string.preview_without_num));

        rlChooseDirectory.setOnClickListener(this);
        tvChooseImageDirectory.setOnClickListener(this);
        tvPreview.setOnClickListener(this);
        //计算每张图片应该显示的宽度
        perWidth = (((WindowManager) (this.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getWidth() - CommonUtil.dip2px(this, 4)) / 3;

        picNums = getIntent().getIntExtra(EXTRA_NUMS, 9);

        if (picNums == 1) {
            tvPreview.setVisibility(View.GONE);
            vLine.setVisibility(View.GONE);
            if (menuItem != null) {
                menuItem.setVisible(false);
                menuItem.setEnabled(false);
            }
        } else {
            if (menuItem != null) {
                menuItem.setEnabled(false);
                menuItem.setTitle("完成");
            }
        }
        initAnimation();
        initListener();
    }

    @SuppressLint("ObjectAnimatorBinding")
    private void initAnimation() {
        if (Build.VERSION.SDK_INT < 11) {
            // no animation cause low SDK version
        } else {
            animation = ObjectAnimator.ofInt(lvDirectories, "bottomMargin", -CommonUtil.dip2px(this, 400), 0);
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int value = (Integer) valueAnimator.getAnimatedValue();
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lvDirectories.getLayoutParams();
                    rlChooseDirectory.setAlpha(1 - Math.abs(value / CommonUtil.dip2px(PickOrTakeImageActivity.this, 400)));
                    params.bottomMargin = value;
                    lvDirectories.setLayoutParams(params);
                    lvDirectories.invalidate();
                    rlChooseDirectory.invalidate();
                }
            });
            animation.setDuration(500);

            reverseanimation = ObjectAnimator.ofInt(lvDirectories, "bottomMargin", 0, -CommonUtil.dip2px(this, 400));
            reverseanimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int value = (Integer) valueAnimator.getAnimatedValue();
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lvDirectories.getLayoutParams();
                    params.bottomMargin = value;
                    lvDirectories.setLayoutParams(params);
                    rlChooseDirectory.setAlpha(1 - Math.abs(value / CommonUtil.dip2px(PickOrTakeImageActivity.this, 400)));
                    if (value <= -CommonUtil.dip2px(PickOrTakeImageActivity.this, 300)) {
                        rlChooseDirectory.setVisibility(View.GONE);
                    }
                    lvDirectories.invalidate();
                    rlChooseDirectory.invalidate();
                }
            });
            reverseanimation.setDuration(500);
        }

        alphaAnimation.setDuration(1000);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                rlDate.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

    }

    protected void initListener() {
        gridView.setOnTouchListener(this);
        rlBottom.setOnClickListener(this);
        lvDirectories.setOnItemClickListener(this);
    }

    /**
     * 6.0版本之后需要动态申请权限
     */
    private void getAllImages() {
        int hasWriteContactsPermission = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission == PackageManager.PERMISSION_GRANTED) {
            initPhoto();
        } else {
            ActivityCompat.requestPermissions(PickOrTakeImageActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_FOR_WRITE_PERMISSION);
        }
    }

    /**
     * 初始化图片
     * 从手机中获取所有图片
     */
    private void initPhoto() {
        LoaderManager.LoaderCallbacks<Cursor> mLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
            private final String[] IMAGE_PROJECTION = {
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.SIZE
            };

            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new CursorLoader(mContext, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION, null, null, IMAGE_PROJECTION[2] + " DESC");
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                if (cursor != null) {
                    allImages.clear();
                    imageDirectories.clear();
                    while (cursor.moveToNext()) {
                        SingleImageModel singleImageModel = new SingleImageModel();
                        singleImageModel.path = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                        singleImageModel.date = cursor.getLong(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
                        singleImageModel.id = cursor.getLong(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[3]));
                        allImages.add(singleImageModel);
                        //存入按照目录分配的list
                        String path = singleImageModel.path;
                        String parentPath = new File(path).getParent();
                        putImageToParentDirectories(parentPath, path, singleImageModel.date, singleImageModel.id);
                    }
                    if (gridView.getAdapter() == null) {
                        gridView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    lvDirectories.setAdapter(listviewAdapter);
                    gridView.setOnScrollListener(PickOrTakeImageActivity.this);

                    cursor.close();
                }
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {

            }
        };
        getSupportLoaderManager().restartLoader(0, null, mLoaderCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CODE_FOR_WRITE_PERMISSION) {
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户同意使用write
                initPhoto();
            } else {
                finish();
            }
        } else if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                takePic();
            }
        }
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.tv_choose_image_directory) {
            if (Build.VERSION.SDK_INT < 11) {
                if (rlChooseDirectory.getVisibility() == View.VISIBLE) {
                    rlChooseDirectory.setVisibility(View.GONE);
                } else {
                    rlChooseDirectory.setVisibility(View.VISIBLE);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lvDirectories.getLayoutParams();
                    params.bottomMargin = 0;
                    lvDirectories.setLayoutParams(params);
                    ((ViewGroup) (lvDirectories.getParent())).invalidate();
                }
            } else {
                if (rlChooseDirectory.getVisibility() == View.VISIBLE) {
                    reverseanimation.start();
                } else {
                    rlChooseDirectory.setVisibility(View.VISIBLE);
                    animation.start();
                }
            }

        } else if (i == R.id.tv_preview) {
            if (currentPicNums > 0) {
                Intent intent = new Intent();
                intent.setClass(PickOrTakeImageActivity.this, PickBigImagesActivity.class);
                intent.putExtra(PickBigImagesActivity.EXTRA_DATA, getChoosePicFromList());
                intent.putExtra(PickBigImagesActivity.EXTRA_ALL_PICK_DATA, picklist);
                intent.putExtra(PickBigImagesActivity.EXTRA_CURRENT_PIC, 0);
                intent.putExtra(PickBigImagesActivity.EXTRA_LAST_PIC, picNums - currentPicNums);
                intent.putExtra(PickBigImagesActivity.EXTRA_TOTAL_PIC, picNums);
                startActivityForResult(intent, CODE_FOR_PIC_BIG_PREVIEW);
                AlbumBitmapCacheHelper.getInstance().releaseHalfSizeCache();
            }

        } else if (i == R.id.rl_choose_directory) {
            if (Build.VERSION.SDK_INT < 11) {
                if (rlChooseDirectory.getVisibility() == View.VISIBLE) {
                    rlChooseDirectory.setVisibility(View.GONE);
                } else {
                    rlChooseDirectory.setVisibility(View.VISIBLE);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lvDirectories.getLayoutParams();
                    params.bottomMargin = 0;
                    lvDirectories.setLayoutParams(params);
                    ((ViewGroup) (lvDirectories.getParent())).invalidate();
                }
            } else {
                if (rlChooseDirectory.getVisibility() == View.VISIBLE) {
                    reverseanimation.start();
                } else {
                    rlChooseDirectory.setVisibility(View.VISIBLE);
                    animation.start();
                }
            }

        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        AlbumBitmapCacheHelper.getInstance().removeAllThreads();
        //点击的全部图片
        if (!(currentShowPosition == i - 1)) {
            currentShowPosition = i - 1;
            reloadDataByChooseDirectory();
        }
    }

    /**
     * 重新加载当前页面数据
     */
    private void reloadDataByChooseDirectory() {
        if (currentShowPosition == -1) {
            tvChooseImageDirectory.setText(getString(R.string.all_pic));
        } else {
            tvChooseImageDirectory.setText(new File(imageDirectories.get(currentShowPosition).directoryPath).getName());
        }
        //去除当前正在加载的所有图片，重新开始
        AlbumBitmapCacheHelper.getInstance().removeAllThreads();
        gridView.setAdapter(adapter);
        gridView.smoothScrollToPosition(0);
        View v = lvDirectories.findViewWithTag("picked");
        if (v != null) {
            v.setVisibility(View.GONE);
            v.setTag(null);
        }
        v = (View) lvDirectories.findViewWithTag(currentShowPosition + 1).getParent().getParent();
        if (v != null) {
            v.findViewById(R.id.iv_directory_check).setVisibility(View.VISIBLE);
            v.findViewById(R.id.iv_directory_check).setTag("picked");
        }
        if (Build.VERSION.SDK_INT < 11) {
            rlChooseDirectory.setVisibility(View.GONE);
        } else {
            reverseanimation.start();
        }
    }

    @Override
    public void onBackPressed() {
        if (rlChooseDirectory.getVisibility() == View.VISIBLE) {
            if (Build.VERSION.SDK_INT < 11) {
                rlChooseDirectory.setVisibility(View.GONE);
            } else {
                reverseanimation.start();
            }
        } else {
            //离开此页面之后记住要清空cache内存
            AlbumBitmapCacheHelper.getInstance().clearCache();
            super.onBackPressed();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        currentState = i;
        if (currentState == SCROLL_STATE_IDLE) {
            rlDate.setAnimation(alphaAnimation);
            alphaAnimation.startNow();
        }
        if (currentTouchState == MotionEvent.ACTION_UP && currentState != SCROLL_STATE_FLING) {
            rlDate.setAnimation(alphaAnimation);
            alphaAnimation.startNow();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i1, int i2) {
        firstVisibleItem = i;
        //保证当选择全部文件夹的时候，显示的时间为第一个图片，排除第一个拍照图片
        if (currentShowPosition == -1 && firstVisibleItem > 0)
            firstVisibleItem--;
        if (lastPicTime != getImageDirectoryModelDateFromMapById(firstVisibleItem)) {
            lastPicTime = getImageDirectoryModelDateFromMapById(firstVisibleItem);
        }
        if (currentState == SCROLL_STATE_TOUCH_SCROLL) {
            showTimeLine(lastPicTime);
        }
        if (currentTouchState == MotionEvent.ACTION_UP && currentState == SCROLL_STATE_FLING) {
            showTimeLine(lastPicTime);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentTouchState = MotionEvent.ACTION_DOWN;
                break;
            case MotionEvent.ACTION_MOVE:
                currentTouchState = MotionEvent.ACTION_MOVE;
                break;
            case MotionEvent.ACTION_UP:
                currentTouchState = MotionEvent.ACTION_UP;
                break;
        }
        return false;
    }

    /**
     * gridview适配器
     */
    private class GridViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            int size = 0;
            //如果显示全部图片,则第一项为
            if (currentShowPosition == -1) {
                size = allImages.size() + 1;
            } else {
                size = imageDirectories.get(currentShowPosition).images.getImageCounts();
            }
            return size;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            //第一个要显示拍摄照片图片
            if (currentShowPosition == -1 && i == 0) {
                view = new ImageView(PickOrTakeImageActivity.this);
                view.setBackgroundResource(R.mipmap.take_pic);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkCameraPermiss();
                    }
                });
                view.setLayoutParams(new GridView.LayoutParams(perWidth, perWidth));
                return view;
            }
            //在此处直接进行处理最好，能够省去其他部分的处理，其他部分直接可以使用原来的数据结构
            if (currentShowPosition == -1)
                i--;
            //其他部分的处理
            final String path = getImageDirectoryModelUrlFromMapById(i);
            lastPicTime = getImageDirectoryModelDateFromMapById(i);
            final GridViewHolder holder;
            if (view == null || view.getTag() == null) {
                view = inflater.inflate(R.layout.item_pick_up_image, viewGroup, false);
                holder = new GridViewHolder();
                holder.iv_content = (ImageView) view.findViewById(R.id.iv_content);
                holder.v_gray_masking = view.findViewById(R.id.v_gray_masking);
                holder.cb_check = (CheckBox) view.findViewById(R.id.cb_check);
                if (picNums == 1) {
                    holder.cb_check.setOnCheckedChangeListener(null);
                    holder.cb_check.setChecked(false);
                }
                OnclickListenerWithHolder listener = new OnclickListenerWithHolder(holder);
                OnCheckedChangeListener checkListener = new OnCheckedChangeListener(holder);
                holder.iv_content.setOnClickListener(listener);

                holder.cb_check.setOnCheckedChangeListener(checkListener);

                view.setTag(holder);
                //要在这进行设置，在外面设置会导致第一个项点击效果异常
                view.setLayoutParams(new GridView.LayoutParams(perWidth, perWidth));
            } else {
                holder = (GridViewHolder) view.getTag();
            }
            //一定不要忘记更新position
            holder.position = i;
            holder.cb_check.setOnCheckedChangeListener(null);
            //如果该图片被选中，则讲状态变为选中状态
            if (getImageDirectoryModelStateFromMapById(i)) {
                holder.v_gray_masking.setVisibility(View.VISIBLE);
                holder.cb_check.setChecked(true);
            } else {
                holder.cb_check.setChecked(false);
                holder.v_gray_masking.setVisibility(View.GONE);
            }
            holder.cb_check.setOnCheckedChangeListener(new OnCheckedChangeListener(holder));

            if (holder.iv_content.getTag() != null) {
                String remove = (String) holder.iv_content.getTag();
                AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(remove);
            }
            AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
            holder.iv_content.setTag(path);
            Bitmap bitmap = AlbumBitmapCacheHelper.getInstance().getBitmap(getApplicationContext(), path, perWidth, perWidth, new AlbumBitmapCacheHelper.ILoadImageCallback() {
                @Override
                public void onLoadImageCallBack(Bitmap bitmap, String path1, Object... objects) {
                    if (bitmap != null) {
                        holder.iv_content.setImageBitmap(bitmap);
                    } else {
                        holder.iv_content.setBackgroundResource(R.color.gray);
                    }
                }
            }, i);
            if (bitmap != null) {
                holder.iv_content.setImageBitmap(bitmap);
            } else {
                holder.iv_content.setBackgroundResource(R.color.gray);
            }
            return view;
        }
    }

    private static class GridViewHolder {
        private ImageView iv_content;
        private View v_gray_masking;
        private CheckBox cb_check;
        private int position;
    }

    /**
     * 检测相机权限
     */
    private void checkCameraPermiss() {
        int hasCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        if (hasCamera != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(PickOrTakeImageActivity.this,
                    Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(PickOrTakeImageActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                return;
            }
            ActivityCompat.requestPermissions(PickOrTakeImageActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {
            takePic();
        }
    }

    /**
     * 调用系统相机进行拍照
     */

    private void takePic() {
        String name = "temp" + System.currentTimeMillis();
        if (!new File(CommonUtil.getDataPath()).exists())
            new File(CommonUtil.getDataPath()).mkdirs();
        tempPath = CommonUtil.getDataPath() + name + ".jpg";
        File file = new File(tempPath);
        try {
            if (file.exists())
                file.delete();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        Uri uriForFile = getUri(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
        startActivityForResult(intent, CODE_FOR_TAKE_PIC);
    }

    /**
     * 展示顶部的时间
     */
    private void showTimeLine(long date) {
        alphaAnimation.cancel();
        rlDate.setVisibility(View.VISIBLE);
        tvDate.setText(calculateShowTime(date * 1000));
    }

    /**
     * 计算照片的具体时间
     *
     * @param time
     * @return
     */
    private String calculateShowTime(long time) {

        final Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        int mDayWeek = c.get(Calendar.DAY_OF_WEEK);
        mDayWeek--;
        //习惯性的还是定周一为第一天
        if (mDayWeek == 0)
            mDayWeek = 7;
        int mWeek = c.get(Calendar.WEEK_OF_MONTH);
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMinute = c.get(Calendar.MINUTE);

        if ((System.currentTimeMillis() - time) < (mHour * 60 + mMinute) * 60 * 1000) {
            return "今天";
        } else if ((System.currentTimeMillis() - time) < (mDayWeek) * 24 * 60 * 60 * 1000) {
            return "本周";
        } else if ((System.currentTimeMillis() - time) < ((long) ((mWeek - 1) * 7 + mDayWeek)) * 24 * 60 * 60 * 1000) {
            return "这个月";
        } else {
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM", Locale.getDefault());
            return format.format(time);
        }
    }

    public class ListviewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (imageDirectories.size() == 0)
                return 0;
            else
                return imageDirectories.size() + 1;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return 0;
            }
            return 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null || view.getTag() == null) {
                view = inflater.inflate(R.layout.item_list_view_album_directory, null);
                ViewHolder holder = new ViewHolder(view);
                view.setTag(holder);
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.position = i;
            holder.tvDirectoryName.setTag(i);
            String path = null;
            //全部图片
            if (getItemViewType(i) == 0) {
                holder.tvDirectoryName.setText(getString(R.string.all_pic) + "   ");
                int size = 0;
                for (SingleImageDirectories directories : imageDirectories)
                    size += directories.images.getImageCounts();
                holder.tvDirectoryName.setText(size + "张");
                //获取第0个位置的图片，即第一张图片展示
                path = imageDirectories.get(0).images.getImagePath(0);
                if (currentShowPosition == -1) {
                    holder.ivDirectoryCheck.setTag("picked");
                    holder.ivDirectoryCheck.setVisibility(View.VISIBLE);
                } else {
                    holder.ivDirectoryCheck.setTag(null);
                    holder.ivDirectoryCheck.setVisibility(View.INVISIBLE);
                }
            } else {
                holder.tvDirectoryNums.setText(imageDirectories.get(i - 1).images.getImageCounts() + "张");
                if (currentShowPosition == i - 1) {
                    holder.ivDirectoryCheck.setTag("picked");
                    holder.ivDirectoryCheck.setVisibility(View.VISIBLE);
                } else {
                    holder.ivDirectoryCheck.setTag(null);
                    holder.ivDirectoryCheck.setVisibility(View.INVISIBLE);
                }
                holder.tvDirectoryName.setText(new File(imageDirectories.get(i - 1).directoryPath).getName() + "   ");
                //获取第0个位置的图片，即第一张图片展示
                path = imageDirectories.get(i - 1).images.getImagePath(0);
            }
            if (path == null)
                return null;
            if (holder.ivDirectoryPic.getTag() != null) {
                AlbumBitmapCacheHelper.getInstance().removePathFromShowlist((String) (holder.ivDirectoryPic.getTag()));
            }
            AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
            if (getItemViewType(i) == 0) {
                holder.ivDirectoryPic.setTag(path + "all");
            } else
                holder.ivDirectoryPic.setTag(path);
            Bitmap bitmap = AlbumBitmapCacheHelper.getInstance().getBitmap(getApplicationContext(), path, 225, 225, new AlbumBitmapCacheHelper.ILoadImageCallback() {
                @Override
                public void onLoadImageCallBack(Bitmap bitmap, String path, Object... objects) {
                    if (bitmap == null) return;
                    BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);
                    View v = null;
                    if (objects[0].toString().equals("0")) {
                        v = lvDirectories.findViewWithTag(path + "all");
                    } else {
                        v = lvDirectories.findViewWithTag(path);
                    }
                    if (v != null) v.setBackgroundDrawable(bd);
                }
            }, getItemViewType(i));
            if (bitmap != null) {
                holder.ivDirectoryPic.setImageBitmap(bitmap);
            } else {
                holder.ivDirectoryPic.setBackgroundResource(R.color.gray);
            }
            return view;
        }

        class ViewHolder {
            ImageView ivDirectoryPic;
            TextView tvDirectoryName;
            TextView tvDirectoryNums;
            ImageView ivDirectoryCheck;

            int position;

            ViewHolder(View view) {
                ivDirectoryPic = (ImageView) view.findViewById(R.id.iv_directory_pic);
                tvDirectoryName = (TextView) view.findViewById(R.id.tv_directory_name);
                tvDirectoryNums = (TextView) view.findViewById(R.id.tv_directory_nums);
                ivDirectoryCheck = (ImageView) view.findViewById(R.id.iv_directory_check);
            }
        }
    }


    /**
     * 根据id获取map中相对应的图片路径
     */
    private String getImageDirectoryModelUrlFromMapById(int position) {
        //如果是选择的全部图片
        if (currentShowPosition == -1) {
            return allImages.get(position).path;
        } else {
            return imageDirectories.get(currentShowPosition).images.getImagePath(position);
        }
    }

    /**
     * 根据id获取map中相对应的图片时间
     */
    private long getImageDirectoryModelDateFromMapById(int position) {
        if (allImages.size() == 0) {
            return System.currentTimeMillis();
        }
        //如果是选择的全部图片
        if (currentShowPosition == -1) {
            return allImages.get(position).date;
        } else {
            return imageDirectories.get(currentShowPosition).images.getImages().get(position).date;
        }
    }

    /**
     * 根据id获取map中相对应的图片选中状态
     */
    private boolean getImageDirectoryModelStateFromMapById(int position) {
        //如果是选择的全部图片
        if (currentShowPosition == -1) {
            return allImages.get(position).isPicked;
        } else {
            return imageDirectories.get(currentShowPosition).images.getImagePickOrNot(position);
        }
    }

    /**
     * 转变该位置图片的选中状态
     *
     * @param position
     */
    private void toggleImageDirectoryModelStateFromMapById(int position) {
        //如果是选择的全部图片
        if (currentShowPosition == -1) {
            allImages.get(position).isPicked = !allImages.get(position).isPicked;
            for (SingleImageDirectories directories : imageDirectories) {
                directories.images.toggleSetImage(allImages.get(position).path);
            }
        } else {
            imageDirectories.get(currentShowPosition).images.toggleSetImage(position);
            for (SingleImageModel model : allImages) {
                if (model.path.equalsIgnoreCase(imageDirectories.get(currentShowPosition).images.getImagePath(position)))
                    model.isPicked = !model.isPicked;
            }
        }
    }

    /**
     * 带holder的监听器
     */
    private class OnclickListenerWithHolder implements View.OnClickListener {
        GridViewHolder holder;

        public OnclickListenerWithHolder(GridViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public void onClick(View view) {
            int position = holder.position;
            int i = view.getId();
            if (i == R.id.iv_content) {
                if (picNums > 1) {
                    Intent intent = new Intent();
                    intent.setClass(PickOrTakeImageActivity.this, PickBigImagesActivity.class);
                    intent.putExtra(PickBigImagesActivity.EXTRA_DATA, getAllImagesFromCurrentDirectory());
                    intent.putExtra(PickBigImagesActivity.EXTRA_ALL_PICK_DATA, picklist);
                    intent.putExtra(PickBigImagesActivity.EXTRA_CURRENT_PIC, position);
                    intent.putExtra(PickBigImagesActivity.EXTRA_LAST_PIC, picNums - currentPicNums);
                    intent.putExtra(PickBigImagesActivity.EXTRA_TOTAL_PIC, picNums);
                    startActivityForResult(intent, CODE_FOR_PIC_BIG);
                    AlbumBitmapCacheHelper.getInstance().releaseHalfSizeCache();
                } else {
                    picklist.add(getImageDirectoryModelUrlFromMapById(holder.position));
                    currentPicNums++;
                    returnDataAndClose();
                }
            }
        }
    }

    private class OnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        GridViewHolder holder;

        public OnCheckedChangeListener(GridViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int position = holder.position;
            toggleImageDirectoryModelStateFromMapById(position);
            if (getImageDirectoryModelStateFromMapById(position)) {
                if (currentPicNums == picNums) {
                    holder.cb_check.setChecked(false);
                    toggleImageDirectoryModelStateFromMapById(position);
                    Toast.makeText(PickOrTakeImageActivity.this, String.format(getString(R.string.choose_pic_num_out_of_index), picNums), Toast.LENGTH_SHORT).show();
                    return;
                }
                picklist.add(getImageDirectoryModelUrlFromMapById(holder.position));
                holder.cb_check.setChecked(true);
                holder.v_gray_masking.setVisibility(View.VISIBLE);
                currentPicNums++;
                tvPreview.setText(String.format(getString(R.string.preview_with_num), currentPicNums));
                menuItem.setEnabled(true);
                menuItem.setTitle(String.format(getString(R.string.choose_pic_finish_with_num), currentPicNums, picNums));
            } else {
                picklist.remove(getImageDirectoryModelUrlFromMapById(holder.position));
                holder.cb_check.setChecked(false);
                holder.v_gray_masking.setVisibility(View.GONE);
                currentPicNums--;
                if (currentPicNums == 0) {
                    menuItem.setEnabled(false);
                    menuItem.setTitle(getString(R.string.choose_pic_finish));
                    tvPreview.setText(getString(R.string.preview_without_num));
                } else {
                    menuItem.setEnabled(true);
                    tvPreview.setText(String.format(getString(R.string.preview_with_num), currentPicNums));
                    menuItem.setTitle(String.format(getString(R.string.choose_pic_finish_with_num), currentPicNums, picNums));
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CODE_FOR_PIC_BIG:
            case CODE_FOR_PIC_BIG_PREVIEW:
                AlbumBitmapCacheHelper.getInstance().resizeCache();
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> temp = (ArrayList<String>) data.getSerializableExtra("pick_data");
                    //如果返回的list中含该path，但是picklist不含有该path，选中
                    for (String path : temp) {
                        if (!picklist.contains(path)) {
                            View v = gridView.findViewWithTag(path);
                            if (v != null) {
                                ((ViewGroup) (v.getParent())).findViewById(R.id.v_gray_masking).setVisibility(View.VISIBLE);
                                ((CheckBox) ((ViewGroup) (v.getParent())).findViewById(R.id.cb_check)).setChecked(true);
                            }
                            setPickStateFromHashMap(path, true);
                        }
                    }
                    //如果返回的list中不含该path，但是picklist含有该path,不选中
                    for (String path : picklist) {
                        if (!temp.contains(path)) {
                            View v = gridView.findViewWithTag(path);
                            if (v != null) {
                                ((ViewGroup) (v.getParent())).findViewById(R.id.v_gray_masking).setVisibility(View.GONE);
                                ((CheckBox) ((ViewGroup) (v.getParent())).findViewById(R.id.cb_check)).setChecked(false);
                            }
                            currentPicNums--;
                            setPickStateFromHashMap(path, false);
                        }
                    }
                    picklist = temp;
                    if (currentPicNums <= 0) {
                        tvPreview.setText(getString(R.string.preview_without_num));
                        menuItem.setTitle(getString(R.string.choose_pic_finish));
                    } else {
                        menuItem.setTitle(String.format(getString(R.string.choose_pic_finish_with_num), currentPicNums, picNums));
                        tvPreview.setText(String.format(getString(R.string.preview_with_num), currentPicNums));
                    }
                    boolean isFinish = data.getBooleanExtra("isFinish", false);
                    if (isFinish) {
                        returnDataAndClose();
                    }
                }
                break;
            case CODE_FOR_TAKE_PIC:
                if (resultCode == RESULT_OK) {
                    //扫描最新的图片进相册
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    File file = new File(tempPath);
                    Uri uri = Uri.fromFile(file);
                    intent.setData(uri);
                    this.sendBroadcast(intent);

                    SingleImageModel singleImageModel = new SingleImageModel();
                    singleImageModel.path = file.getAbsolutePath();
                    singleImageModel.date = System.currentTimeMillis();
                    singleImageModel.id = System.currentTimeMillis();
                    allImages.add(0, singleImageModel);

                    refreshData();
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 刷新数据
     */
    private void refreshData() {
        if (gridView.getAdapter() == null) {
            gridView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        lvDirectories.setAdapter(listviewAdapter);
        gridView.setOnScrollListener(PickOrTakeImageActivity.this);
    }

    /**
     * 获取uri  适配7.0
     *
     * @param file
     * @return
     */
    private Uri getUri(File file) {
        Uri uriForFile;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uriForFile = FileProvider.getUriForFile(this, "com.twlk.ximgpiker.fileprovider", file);
        } else {
            uriForFile = Uri.fromFile(file);
        }
        return uriForFile;
    }

    /**
     * 点击完成按钮之后将图片的地址返回到上一个页面
     */
    private void returnDataAndClose() {
        AlbumBitmapCacheHelper.getInstance().clearCache();
        if (currentPicNums == 0) {
            Toast.makeText(this, getString(R.string.not_choose_any_pick), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = new Intent();
        data.putExtra("data", picklist);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    /**
     * 设置该图片的选中状态
     */
    private void setPickStateFromHashMap(String path, boolean isPick) {
        for (SingleImageDirectories directories : imageDirectories) {
            if (isPick)
                directories.images.setImage(path);
            else
                directories.images.unsetImage(path);
        }
        for (SingleImageModel model : allImages) {
            if (model.path.equalsIgnoreCase(path))
                model.isPicked = isPick;
        }
    }

    /**
     * 获取所有的选中图片
     */
    private ArrayList<SingleImageModel> getChoosePicFromList() {
        ArrayList<SingleImageModel> list = new ArrayList<SingleImageModel>();
        for (String path : picklist) {
            SingleImageModel model = new SingleImageModel(path, true, 0, 0);
            list.add(model);
        }
        return list;
    }

    /**
     * 获取当前选中文件夹中给的所有图片
     */
    private ArrayList<SingleImageModel> getAllImagesFromCurrentDirectory() {
        ArrayList<SingleImageModel> list = new ArrayList<SingleImageModel>();
        if (currentShowPosition == -1) {
            for (SingleImageModel model : allImages) {
                list.add(model);
            }
        } else {
            for (SingleImageModel model : imageDirectories.get(currentShowPosition).images.getImages()) {
                list.add(model);
            }
        }
        return list;
    }

    /**
     * 将图片插入到对应parentPath路径的文件夹中
     */
    private void putImageToParentDirectories(String parentPath, String path, long date, long id) {
        ImageDirectoryModel model = getModelFromKey(parentPath);
        if (model == null) {
            model = new ImageDirectoryModel();
            SingleImageDirectories directories = new SingleImageDirectories();
            directories.images = model;
            directories.directoryPath = parentPath;
            imageDirectories.add(directories);
        }
        model.addImage(path, date, id);
    }

    private ImageDirectoryModel getModelFromKey(String path) {
        for (SingleImageDirectories directories : imageDirectories) {
            if (directories.directoryPath.equalsIgnoreCase(path)) {
                return directories.images;
            }
        }
        return null;
    }

    /**
     * 一个文件夹中的图片数据实体
     */
    private class SingleImageDirectories {
        /**
         * 父目录的路径
         */
        public String directoryPath;
        /**
         * 目录下的所有图片实体
         */
        public ImageDirectoryModel images;
    }

}
