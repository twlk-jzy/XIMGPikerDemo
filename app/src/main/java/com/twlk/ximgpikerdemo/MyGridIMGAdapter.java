package com.twlk.ximgpikerdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MyGridIMGAdapter extends BaseAdapter {
    private Context mContext;

    private List<String> imgs = new ArrayList<>();

    private LayoutInflater inflater;
    private boolean isDel = false;

    public MyGridIMGAdapter(Context mContext) {
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return imgs.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        return imgs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        convertView = inflater.inflate(R.layout.item_img_grid, parent, false);
        ViewHolder holder = new ViewHolder(convertView);
        ImageView ivImg = holder.ivImg;

        if (position < imgs.size()) {
            Glide.with(mContext).load(imgs.get(position)).into(ivImg);
        } else {
            ivImg.setImageResource(R.mipmap.add);
            ivImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (addIMGListener != null) {
                        addIMGListener.addClick();
                    }
                }
            });
        }

        return convertView;
    }

    public void removeIMG(String imgPath){
        if(imgs.contains(imgPath)){
            imgs.remove(imgPath);
        }
        notifyDataSetChanged();
    }


    public void addIMGS(ArrayList<String> imgList) {
        imgs.addAll(0, imgList);
    }

    public void setIsDel(boolean isDel) {
        this.isDel = isDel;
    }

    private AddIMGListener addIMGListener = null;

    public void setAddIMGListener(AddIMGListener addIMGListener) {
        this.addIMGListener = addIMGListener;
    }

    private DeleteIMGListener deleteIMGListener = null;

    public void setDelIMGListener(DeleteIMGListener deleteIMGListener) {
        this.deleteIMGListener = deleteIMGListener;
    }

    public interface AddIMGListener {
        void addClick();
    }

    public interface DeleteIMGListener {
        void delClick(String imgPath);
    }

    static class ViewHolder {
        ImageView ivImg;

        ViewHolder(View view) {
            ivImg = view.findViewById(R.id.iv_img);
        }
    }
}
