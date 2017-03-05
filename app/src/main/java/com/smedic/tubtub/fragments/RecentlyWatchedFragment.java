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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.R;
import com.smedic.tubtub.adapters.VideosAdapter;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.interfaces.OnFavoritesSelected;
import com.smedic.tubtub.interfaces.OnItemSelected;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.NetworkConf;

import java.util.ArrayList;

/**
 * Class that handles list of the recently watched YouTube
 * Created by smedic on 7.3.16..
 */
public class RecentlyWatchedFragment extends BaseFragment implements
        AdapterView.OnItemClickListener {

    private static final String TAG = "SMEDIC RecentlyWatched";
    private ArrayList<YouTubeVideo> recentlyPlayedVideos;

    private ListView recentlyPlayedListView;
    private VideosAdapter videoListAdapter;
    private OnItemSelected itemSelected;
    private OnFavoritesSelected onFavoritesSelected;
    private NetworkConf conf;
    private Context context;

    public RecentlyWatchedFragment() {
        // Required empty public constructor
    }

    public static RecentlyWatchedFragment newInstance() {
        return new RecentlyWatchedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recentlyPlayedVideos = new ArrayList<>();
        conf = new NetworkConf(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        recentlyPlayedListView = (ListView) v.findViewById(R.id.fragment_list_items);
        recentlyPlayedListView.setOnItemClickListener(this);
        videoListAdapter = new VideosAdapter(getActivity(), recentlyPlayedVideos);
        videoListAdapter.setOnItemEventsListener(this);
        recentlyPlayedListView.setAdapter(videoListAdapter);

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        recentlyPlayedVideos.clear();
        recentlyPlayedVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).readAll());
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            this.context = context;
            itemSelected = (MainActivity) context;
            onFavoritesSelected = (MainActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
        itemSelected = null;
        onFavoritesSelected = null;
    }

    /**
     * Clears recently played list items
     */
    public void clearRecentlyPlayedList() {
        recentlyPlayedVideos.clear();
        videoListAdapter.notifyDataSetChanged();
    }

    /**
     * Adds listener for list item choosing
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).create(recentlyPlayedVideos.get(pos));
        itemSelected.onVideoSelected(recentlyPlayedVideos.get(pos));
    }

    @Override
    public void onFavoriteClicked(YouTubeVideo video, boolean isChecked) {
        super.onFavoriteClicked(video, isChecked);
        onFavoritesSelected.onFavoritesSelected(video, isChecked); // pass event to MainActivity
    }
}
