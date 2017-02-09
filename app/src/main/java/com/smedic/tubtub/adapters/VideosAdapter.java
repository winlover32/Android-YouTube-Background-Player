/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smedic.tubtub.adapters;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.smedic.tubtub.R;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.interfaces.ItemEventsListener;
import com.smedic.tubtub.model.ItemType;
import com.smedic.tubtub.model.YouTubeVideo;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Custom ArrayAdapter which enables setup of a list view row views
 * Created by smedic on 8.2.16..
 */
public class VideosAdapter extends ArrayAdapter<YouTubeVideo> {

    private Activity context;
    private final List<YouTubeVideo> list;
    private boolean[] itemChecked;
    private boolean isFavoriteList;
    private ItemEventsListener itemEventsListener;

    public VideosAdapter(Activity context, List<YouTubeVideo> list, boolean isFavoriteList) {
        super(context, R.layout.video_item, list);
        this.list = list;
        this.context = context;
        this.itemChecked = new boolean[50];
        this.isFavoriteList = isFavoriteList;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        if (convertView == null) {
            convertView = context.getLayoutInflater().inflate(R.layout.video_item, parent, false);
        }
        ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
        TextView title = (TextView) convertView.findViewById(R.id.video_title);
        TextView duration = (TextView) convertView.findViewById(R.id.video_duration);
        TextView viewCount = (TextView) convertView.findViewById(R.id.views_number);
        CheckBox favoriteCheckBox = (CheckBox) convertView.findViewById(R.id.favoriteButton);
        ImageView shareButton = (ImageView) convertView.findViewById(R.id.shareButton);

        final YouTubeVideo searchResult = list.get(position);

        Picasso.with(context).load(searchResult.getThumbnailURL()).into(thumbnail);
        title.setText(searchResult.getTitle());
        duration.setText(searchResult.getDuration());
        viewCount.setText(searchResult.getViewCount());

        //set checked if exists in database
        if (YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).checkIfExists(searchResult.getId())) {
            itemChecked[position] = true;
        } else {
            itemChecked[position] = false;
        }
        favoriteCheckBox.setChecked(itemChecked[position]);

        favoriteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                itemChecked[position] = isChecked;
            }
        });

        favoriteCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).create(searchResult);
                } else {
                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).delete(searchResult.getId());
                    if (isFavoriteList) {
                        list.remove(position);
                        notifyDataSetChanged();
                    }
                }
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemEventsListener != null) {
                    itemEventsListener.onShareClicked(ItemType.YOUTUBE_MEDIA_TYPE_VIDEO,
                            searchResult.getId());
                }
            }
        });

        return convertView;
    }

    public void setOnItemEventsListener(ItemEventsListener listener) {
        itemEventsListener = listener;
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).hashCode();
    }


    @Override
    public boolean hasStableIds() {
        return true;
    }

}
