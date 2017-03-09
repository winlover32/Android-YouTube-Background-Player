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
import android.content.Context;
import android.view.LayoutInflater;
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
import com.smedic.tubtub.utils.Config;
import com.squareup.picasso.Picasso;

import java.util.List;

import static com.smedic.tubtub.R.id.shareButton;

/**
 * Custom ArrayAdapter which enables setup of a list view row views
 * Created by smedic on 8.2.16..
 */
public class VideosAdapter extends ArrayAdapter<YouTubeVideo> {

    private Activity context;
    private final List<YouTubeVideo> list;
    private boolean[] itemChecked;
    private ItemEventsListener itemEventsListener;

    public VideosAdapter(Activity context, List<YouTubeVideo> list) {
        super(context, R.layout.video_item, list);
        this.list = list;
        this.context = context;
        this.itemChecked = new boolean[(int) Config.NUMBER_OF_VIDEOS_RETURNED];
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.video_item, parent, false);

            holder.thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
            holder.title = (TextView) convertView.findViewById(R.id.video_title);
            holder.duration = (TextView) convertView.findViewById(R.id.video_duration);
            holder.viewCount = (TextView) convertView.findViewById(R.id.views_number);
            holder.favoriteCheckBox = (CheckBox) convertView.findViewById(R.id.favoriteButton);
            holder.shareButton = (ImageView) convertView.findViewById(shareButton);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final YouTubeVideo video = list.get(position);

        if (YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).checkIfExists(video.getId())) {
            itemChecked[position] = true;
        } else {
            itemChecked[position] = false;
        }

        Picasso.with(context).load(video.getThumbnailURL()).into(holder.thumbnail);
        holder.title.setText(video.getTitle());
        holder.duration.setText(video.getDuration());
        holder.viewCount.setText(video.getViewCount());
        holder.favoriteCheckBox.setOnCheckedChangeListener(null);
        holder.favoriteCheckBox.setChecked(itemChecked[position]);

        holder.favoriteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                itemChecked[position] = isChecked;
                if (itemEventsListener != null) {
                    itemEventsListener.onFavoriteClicked(video, isChecked);
                }
            }
        });

        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemEventsListener != null) {
                    itemEventsListener.onShareClicked(ItemType.YOUTUBE_MEDIA_TYPE_VIDEO,
                            video.getId());
                }
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView duration;
        TextView viewCount;
        CheckBox favoriteCheckBox;
        ImageView shareButton;
    }

    public void setOnItemEventsListener(ItemEventsListener listener) {
        itemEventsListener = listener;
    }
}
