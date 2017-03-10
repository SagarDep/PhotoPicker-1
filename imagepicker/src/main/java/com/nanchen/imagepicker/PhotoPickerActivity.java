package com.nanchen.imagepicker;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nanchen.imagepicker.adapter.FolderAdapter;
import com.nanchen.imagepicker.adapter.PhotoAdapter;
import com.nanchen.imagepicker.adapter.PhotoAdapter.PhotoClickCallBack;
import com.nanchen.imagepicker.bean.Photo;
import com.nanchen.imagepicker.bean.PhotoFolder;
import com.nanchen.imagepicker.util.OtherUtils;
import com.nanchen.imagepicker.util.PhotoUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class PhotoPickerActivity extends AppCompatActivity implements OnClickListener,PhotoClickCallBack {
    /**
     * 是否显示相机
     */
    public final static String EXTRA_SHOW_CAMERA = "is_show_camera";
    public final static int REQUEST_CAMERA = 1;
    /**
     * 照片选择模式
     */
    public final static String EXTRA_SELECT_MODE = "select_mode";
    /**
     * 最大选择数量
     */
    public final static String EXTRA_MAX_MUN = "max_num";
    /**
     * 单选
     */
    public final static int MODE_SINGLE = 0;
    /**
     * 多选
     */
    public final static int MODE_MULTI = 1;
    /**
     * 默认最大选择数量
     */
    public final static int DEFAULT_NUM = 9;

    private final static String ALL_PHOTO = "所有图片";
    public final static String KEY_RESULT = "picker_result";
    /**
     * 是否显示相机，默认不显示
     */
    private boolean mIsShowCamera = false;
    /**
     * 照片选择模式，默认是单选模式
     */
    private int mSelectMode = 0;
    /**
     * 最大选择数量，仅多选模式有用
     */
    private int mMaxNum;
    private ImageView mTitleBack;
    private TextView mTextDes;
    private Button mTitleOk;
    private RecyclerView mRecyclerView;
    private Button mBtnDir;
    private Button mBtnPreview;

    private ProgressDialog mProgressDialog;
    private Map<String, PhotoFolder> mFolderMap;
    private GetPhotosAsyncTask mAsyncTask;
    private List<Photo> mPhotoLists;
    private ArrayList<String> mSelectList;
    private PhotoAdapter mPhotoAdapter;
    private List<PhotoFolder> folders;


    /** 文件夹列表是否处于显示状态 */
    boolean mIsFolderViewShow = false;
    /** 文件夹列表是否被初始化，确保只被初始化一次 */
    boolean mIsFolderViewInit = false;

    /** 拍照时存储拍照结果的临时文件 */
    private File mTmpFile;
    private GridView mGridView;
    private ListView mFolderListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);
        initIntentParams();
        initData();
        initView();
        if (!OtherUtils.isExternalStorageAvailable()) {
            Toast.makeText(this, "没有内存卡！", Toast.LENGTH_SHORT).show();
            return;
        }
        mAsyncTask = new GetPhotosAsyncTask(this);
        mAsyncTask.execute();
    }

    private void initData() {
        mPhotoLists = new ArrayList<>();
        mSelectList = new ArrayList<>();
        folders = new ArrayList<>();
    }

    private void initView() {
        // titleBar设置
        mTitleBack = (ImageView) findViewById(R.id.title_iv_back);
        mTextDes = (TextView) findViewById(R.id.title_tv_des);
        mTitleOk = (Button) findViewById(R.id.title_btn_ok);
        mTitleBack.setOnClickListener(this);
        mTitleOk.setOnClickListener(this);


        mRecyclerView = (RecyclerView) findViewById(R.id.photo_picker_rv);
        mGridView = (GridView) findViewById(R.id.photo_picker_gv);

        // footer设置
        mBtnDir = (Button) findViewById(R.id.photo_picker_btn_dir);
        mBtnPreview = (Button) findViewById(R.id.photo_picker_btn_preview);
        mBtnDir.setOnClickListener(this);
        mBtnPreview.setOnClickListener(this);
    }

    /**
     * 初始化选项参数
     */
    private void initIntentParams() {
        // 是否显示相机，默认为不显示
        mIsShowCamera = getIntent().getBooleanExtra(EXTRA_SHOW_CAMERA, false);
        mSelectMode = getIntent().getIntExtra(EXTRA_SELECT_MODE, MODE_SINGLE);
        mMaxNum = getIntent().getIntExtra(EXTRA_MAX_MUN, DEFAULT_NUM);
    }

    @Override
    public void onClick(View v) {
        // library的id不是静态的，不能使用switch case
        int id = v.getId();
        if (id == R.id.title_iv_back) { // 返回
            finish();
        } else if (id == R.id.title_btn_ok) { // 点击确定

        } else if (id == R.id.photo_picker_btn_dir) { // 查看其它目录
            toggleFolderList(folders);
        } else if (id == R.id.photo_picker_btn_preview) { // 预览

        }
    }

    /**
     * 显示或者隐藏文件夹列表
     * @param folders
     */
    private void toggleFolderList(final List<PhotoFolder> folders) {
        //初始化文件夹列表
        if(!mIsFolderViewInit) {
            ViewStub folderStub = (ViewStub) findViewById(R.id.floder_stub);
            folderStub.inflate();
            View dimLayout = findViewById(R.id.dim_layout);
            mFolderListView = (ListView) findViewById(R.id.listview_floder);
            final FolderAdapter adapter = new FolderAdapter(this, folders);
            mFolderListView.setAdapter(adapter);
            mFolderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    for (PhotoFolder folder : folders) {
                        folder.setIsSelected(false);
                    }
                    PhotoFolder folder = folders.get(position);
                    folder.setIsSelected(true);
                    adapter.notifyDataSetChanged();

                    mPhotoLists.clear();
                    mPhotoLists.addAll(folder.getPhotoList());
                    if (ALL_PHOTO.equals(folder.getName())) {
                        mPhotoAdapter.setIsShowCamera(mIsShowCamera);
                    } else {
                        mPhotoAdapter.setIsShowCamera(false);
                    }
                    //这里重新设置adapter而不是直接notifyDataSetChanged，是让GridView返回顶部
                    mGridView.setAdapter(mPhotoAdapter);
                    toggle();
                }
            });
            dimLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (mIsFolderViewShow) {
                        toggle();
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            initAnimation(dimLayout);
            mIsFolderViewInit = true;
        }
        toggle();
    }

    /**
     * 弹出或者收起文件夹列表
     */
    private void toggle() {
        if(mIsFolderViewShow) {
            outAnimatorSet.start();
            mIsFolderViewShow = false;
        } else {
            inAnimatorSet.start();
            mIsFolderViewShow = true;
        }
    }


    /**
     * 初始化文件夹列表的显示隐藏动画
     */
    AnimatorSet inAnimatorSet = new AnimatorSet();
    AnimatorSet outAnimatorSet = new AnimatorSet();
    private void initAnimation(View dimLayout) {
        ObjectAnimator alphaInAnimator, alphaOutAnimator, transInAnimator, transOutAnimator;
        //获取actionBar的高
        TypedValue tv = new TypedValue();
        int actionBarHeight = 0;
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        /**
         * 这里的高度是，屏幕高度减去上、下tab栏，并且上面留有一个tab栏的高度
         * 所以这里减去3个actionBarHeight的高度
         */
        int height = OtherUtils.getHeightInPx(this) - 3*actionBarHeight;
        alphaInAnimator = ObjectAnimator.ofFloat(dimLayout, "alpha", 0f, 0.7f);
        alphaOutAnimator = ObjectAnimator.ofFloat(dimLayout, "alpha", 0.7f, 0f);
        transInAnimator = ObjectAnimator.ofFloat(mFolderListView, "translationY", height , 0);
        transOutAnimator = ObjectAnimator.ofFloat(mFolderListView, "translationY", 0, height);

        LinearInterpolator linearInterpolator = new LinearInterpolator();

        inAnimatorSet.play(transInAnimator).with(alphaInAnimator);
        inAnimatorSet.setDuration(300);
        inAnimatorSet.setInterpolator(linearInterpolator);
        outAnimatorSet.play(transOutAnimator).with(alphaOutAnimator);
        outAnimatorSet.setDuration(300);
        outAnimatorSet.setInterpolator(linearInterpolator);
    }

    private void getPhotosSuccess() {
        mProgressDialog.dismiss();
        mPhotoLists.addAll(mFolderMap.get(ALL_PHOTO).getPhotoList());


        mPhotoAdapter = new PhotoAdapter(this.getApplicationContext(), mPhotoLists);
        mPhotoAdapter.setIsShowCamera(mIsShowCamera);
        mPhotoAdapter.setSelectMode(mSelectMode);
        mPhotoAdapter.setMaxNum(mMaxNum);
        mPhotoAdapter.setPhotoClickCallBack(this);
        mGridView.setAdapter(mPhotoAdapter);
        Set<String> keys = mFolderMap.keySet();
        for (String key : keys) {
            if (ALL_PHOTO.equals(key)) {
                PhotoFolder folder = mFolderMap.get(key);
                folder.setIsSelected(true);
                folders.add(0, folder);
            } else {
                folders.add(mFolderMap.get(key));
            }
        }
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mPhotoAdapter.isShowCamera() && position == 0) {
                    showCamera();
                    return;
                }
                selectPhoto(mPhotoAdapter.getItem(position));
            }
        });
    }

    @Override
    public void onPhotoClick() {
        List<String> list = mPhotoAdapter.getmSelectedPhotos();
        if(list != null && list.size()>0) {
            mTitleOk.setEnabled(true);
            mTitleOk.setText(OtherUtils.formatResourceString(getApplicationContext(),
                    R.string.commit_num, list.size(), mMaxNum));
        } else {
            mTitleOk.setEnabled(false);
            mTitleOk.setText(R.string.commit);
        }
    }

    /**
     * 点击选择某张照片
     * @param photo
     */
    private void selectPhoto(Photo photo) {
        if(photo == null) {
            return;
        }
        String path = photo.getPath();
        if(mSelectMode == MODE_SINGLE) {
            mSelectList.add(path);
            returnData();
        }
    }

    /**
     * 返回选择图片的路径
     */
    private void returnData() {
        // 返回已选择的图片数据
        Intent data = new Intent();
        data.putStringArrayListExtra(KEY_RESULT, mSelectList);
        setResult(RESULT_OK, data);
        finish();
    }


    /**
     * 选择相机
     */
    private void showCamera() {
        // 跳转到系统照相机
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(cameraIntent.resolveActivity(getPackageManager()) != null){
            // 设置系统相机拍照后的输出路径
            // 创建临时文件
            mTmpFile = OtherUtils.createFile(getApplicationContext());
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTmpFile));
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }else{
            Toast.makeText(getApplicationContext(),
                    "相机不存在", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 相机拍照完成后，返回图片路径
        if(requestCode == REQUEST_CAMERA){
            if(resultCode == Activity.RESULT_OK) {
                if (mTmpFile != null) {
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + mTmpFile.getAbsolutePath())));
                    mSelectList.add(mTmpFile.getAbsolutePath());
                    returnData();
                }
            }else{
                if(mTmpFile != null && mTmpFile.exists()){
                    mTmpFile.delete();
                }
            }
        }
    }

    private static class GetPhotosAsyncTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<PhotoPickerActivity> weakActivity;
        private PhotoPickerActivity mActivity;

        public GetPhotosAsyncTask(PhotoPickerActivity activity) {
            this.weakActivity = new WeakReference<>(activity);
            if (weakActivity.get() != null) {
                this.mActivity = weakActivity.get();
            } else {
                this.mActivity = activity;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mActivity.mProgressDialog = ProgressDialog.show(mActivity, null, "加载中...");
        }

        @Override
        protected Void doInBackground(Void... params) {
            mActivity.mFolderMap = PhotoUtils.getPhotos(mActivity.getApplicationContext());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mActivity.getPhotosSuccess();
        }
    }


}
