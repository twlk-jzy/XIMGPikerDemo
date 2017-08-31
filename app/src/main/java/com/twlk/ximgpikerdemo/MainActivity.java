package com.twlk.ximgpikerdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.GridView;

import com.twlk.ximgpiker.activity.PickOrTakeImageActivity;
import com.twlk.ximgpiker.base.BaseActivity;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {

    Toolbar toolbar;
    GridView gvImg;
    private MyGridIMGAdapter adapter;
    @Override
    protected int getLayoutId(@Nullable Bundle savedInstanceState) {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {
        toolbar = findViewById(R.id.toolbar);
        gvImg = findViewById(R.id.gv_img);
        setTitle(toolbar, "选择图片");
        adapter = new MyGridIMGAdapter(mContext);
        gvImg.setAdapter(adapter);


        adapter.setAddIMGListener(new MyGridIMGAdapter.AddIMGListener() {
            @Override
            public void addClick() {
                Intent intent = new Intent(mContext, PickOrTakeImageActivity.class);
                startActivityForResult(intent, 0x0013);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0x0013 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                ArrayList<String> imgs = data.getStringArrayListExtra("data");
                if (imgs != null) {
                    adapter.addIMGS(imgs);
                    adapter.notifyDataSetChanged();
                }
            }
        }

    }
}
