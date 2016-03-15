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
package com.smedic.tubtub.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.SimpleSwipeUndoAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.nhaarman.listviewanimations.util.Swappable;
import com.smedic.tubtub.BackgroundAudioService;
import com.smedic.tubtub.R;
import com.smedic.tubtub.YouTubeVideo;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.SnappyDb;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Class that handles list of the recently watched YouTube
 * Created by smedic on 7.3.16..
 */
public class RecentlyWatchedFragment extends Fragment {

    private static final String TAG = "SMEDIC RecentlyWatched";
    private ArrayList<YouTubeVideo> recentlyPlayedVideos;

    private DynamicListView recentlyPlayedListView;
    private VideoListAdapter videoListAdapter;

    public RecentlyWatchedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_recently_watched, container, false);

        /* Setup the ListView */
        recentlyPlayedListView = (DynamicListView) v.findViewById(R.id.recently_played);

        recentlyPlayedVideos = new ArrayList<>();
        setupListViewAndAdapter();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getUserVisibleHint()) {
            //do nothing for now
        }
        recentlyPlayedVideos.clear();
        recentlyPlayedVideos.addAll(SnappyDb.getInstance().getAllVideoItems());
        videoListAdapter.notifyDataSetChanged();
    }


    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);

        if (visible && isResumed()) {
            //Log.d(TAG, "RecentlyWatchedFragment visible and resumed");
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    /**
     * Setups list view and adapter for storing recently watched YouTube videos
     */
    private void setupListViewAndAdapter() {

        /* Setup the adapter */
        videoListAdapter = new VideoListAdapter(getActivity());
        SimpleSwipeUndoAdapter simpleSwipeUndoAdapter = new SimpleSwipeUndoAdapter(videoListAdapter, getContext(), new MyOnDismissCallback());
        SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(simpleSwipeUndoAdapter);
        animationAdapter.setAbsListView(recentlyPlayedListView);
        recentlyPlayedListView.setAdapter(animationAdapter);

        /* Enable drag and drop functionality */
        recentlyPlayedListView.enableDragAndDrop();
        //recentlyPlayedListView.setDraggableManager(new TouchViewDraggableManager(R.id.row_item));
        recentlyPlayedListView.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                                   final int position, final long id) {
                        recentlyPlayedListView.startDragging(position);
                        return true;
                    }
                }
        );

        /* Enable swipe to dismiss with Undo */
        recentlyPlayedListView.enableSimpleSwipeUndo();

        addListeners();
    }

    /**
     * Adds listener for list item choosing
     */
    void addListeners() {
        recentlyPlayedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, final int pos,
                                    long id) {
                Toast.makeText(getContext(), "Playing: " + recentlyPlayedVideos.get(pos).getTitle(), Toast.LENGTH_SHORT).show();

                Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
                serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
                serviceIntent.putExtra(Config.YOUTUBE_MEDIA_TYPE, Config.YOUTUBE_VIDEO);
                serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, recentlyPlayedVideos.get(pos));
                getActivity().startService(serviceIntent);
            }
        });
    }

    /**
     * Custom array adapter class which enables drag and drop and delete/undo of list items
     */
    private class VideoListAdapter extends ArrayAdapter<YouTubeVideo> implements Swappable, UndoAdapter {

        public VideoListAdapter(Activity context) {
            super(context, R.layout.video_item, recentlyPlayedVideos);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.video_item, parent, false);
            }
            ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
            TextView title = (TextView) convertView.findViewById(R.id.video_title);
            TextView duration = (TextView) convertView.findViewById(R.id.video_duration);

            YouTubeVideo searchResult = recentlyPlayedVideos.get(position);

            Picasso.with(getContext()).load(searchResult.getThumbnailURL()).into(thumbnail);
            title.setText(searchResult.getTitle());
            duration.setText(searchResult.getDuration());

            return convertView;
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).hashCode();
        }


        @Override
        public boolean hasStableIds() {
            return true;
        }


        @Override
        public void swapItems(int i, int i1) {
            YouTubeVideo firstItem = getItem(i);

            recentlyPlayedVideos.set(i, getItem(i1));
            recentlyPlayedVideos.set(i1, firstItem);

            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getUndoView(int i, @Nullable View convertView, @NonNull ViewGroup viewGroup) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.undo_row, viewGroup, false);
            }
            return view;
        }

        @NonNull
        @Override
        public View getUndoClickView(@NonNull View view) {
            return view.findViewById(R.id.undo_row_undobutton);
        }
    }

    /**
     * Callback which handles onDismiss event of a list item
     */
    private class MyOnDismissCallback implements OnDismissCallback {

        @Nullable
        private Toast mToast;

        @Override
        public void onDismiss(@NonNull final ViewGroup listView, @NonNull final int[] reverseSortedPositions) {
            for (int position : reverseSortedPositions) {
                SnappyDb.getInstance().removeVideo(recentlyPlayedVideos.get(position).getId());
                recentlyPlayedVideos.remove(position);
                videoListAdapter.notifyDataSetChanged();
            }

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(
                    getActivity(),
                    getString(R.string.removed_positions, Arrays.toString(reverseSortedPositions)),
                    Toast.LENGTH_LONG
            );
            mToast.show();
        }
    }

}
