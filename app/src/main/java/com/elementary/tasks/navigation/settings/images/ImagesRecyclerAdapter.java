package com.elementary.tasks.navigation.settings.images;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.elementary.tasks.R;
import com.elementary.tasks.core.utils.Prefs;
import com.elementary.tasks.core.utils.ThemeUtil;
import com.elementary.tasks.databinding.PhotoListItemBinding;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2016 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class ImagesRecyclerAdapter extends RecyclerView.Adapter<ImagesRecyclerAdapter.PhotoViewHolder> {

    private Context mContext;
    private List<ImageItem> mDataList;
    private int prevSelected = -1;
    private SelectListener mListener;

    ImagesRecyclerAdapter(Context context, List<ImageItem> dataItemList, SelectListener listener) {
        this.mContext = context;
        this.mDataList = new ArrayList<>(dataItemList);
        this.mListener = listener;
    }

    void deselectLast() {
        if (prevSelected != -1) {
            mDataList.get(prevSelected).setSelected(false);
            notifyItemChanged(prevSelected);
            prevSelected = -1;
        }
    }

    void setPrevSelected(int prevSelected) {
        this.prevSelected = prevSelected;
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        return new PhotoViewHolder(DataBindingUtil.inflate(inflater, R.layout.photo_list_item, parent, false).getRoot());
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        ImageItem item = mDataList.get(position);
        holder.binding.setItem(item);
    }

    @Override
    public int getItemCount() {
        return mDataList != null ? mDataList.size() : 0;
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        PhotoListItemBinding binding;
        PhotoViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
            binding.container.setOnClickListener(view -> performClick(getAdapterPosition()));
            binding.container.setOnLongClickListener(view -> {
                if (mListener != null) {
                    mListener.onItemLongClicked(getAdapterPosition(), view);
                }
                return true;
            });
        }
    }

    void addItems(List<ImageItem> list) {
        mDataList.addAll(list);
        notifyItemInserted(getItemCount() - list.size());
    }

    private void performClick(int position) {
        if (position == prevSelected) {
            mDataList.get(prevSelected).setSelected(false);
            notifyItemChanged(prevSelected);
            prevSelected = -1;
            Prefs.getInstance(mContext).setImageId(-1);
            Prefs.getInstance(mContext).setImagePath("");
            if (mListener != null) mListener.onImageSelected(false);
        } else {
            if (prevSelected != -1) {
                if (prevSelected >= getItemCount() && mListener != null) {
                    mListener.deselectOverItem(prevSelected);
                } else {
                    mDataList.get(prevSelected).setSelected(false);
                    notifyItemChanged(prevSelected);
                }
            }
            prevSelected = position;
            mDataList.get(position).setSelected(true);
            ImageItem item = mDataList.get(position);
            Prefs.getInstance(mContext).setImageId(position);
            Prefs.getInstance(mContext).setImagePath(RetrofitBuilder.getImageLink(item.getId()));
            notifyItemChanged(position);
            if (mListener != null) mListener.onImageSelected(true);
        }
    }

    @BindingAdapter("loadPhoto")
    public static void loadPhoto(ImageView imageView, long id) {
        boolean isDark = ThemeUtil.getInstance(imageView.getContext()).isDark();
        String url = RetrofitBuilder.getImageLink(id, 800, 480);
        Picasso.with(imageView.getContext())
                .load(url)
                .error(isDark ? R.drawable.ic_broken_image_white_24dp : R.drawable.ic_broken_image_black_24dp)
                .into(imageView);
    }
}