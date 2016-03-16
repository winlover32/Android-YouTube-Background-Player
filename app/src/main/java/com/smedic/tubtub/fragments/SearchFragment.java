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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.smedic.tubtub.BackgroundAudioService;
import com.smedic.tubtub.R;
import com.smedic.tubtub.VideosAdapter;
import com.smedic.tubtub.YouTubeSearch;
import com.smedic.tubtub.YouTubeVideo;
import com.smedic.tubtub.interfaces.YouTubeVideosReceiver;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.SnappyDb;

import java.util.ArrayList;

/**
 * Class that handles list of the videos searched on YouTube
 * Created by smedic on 7.3.16..
 */
public class SearchFragment extends ListFragment implements YouTubeVideosReceiver {

    private static final String TAG = "SMEDIC search frag";
    private DynamicListView videosFoundListView;
    private Handler handler;
    private ArrayList<YouTubeVideo> searchResultsList;
    private VideosAdapter videoListAdapter;
    private int preLast = 0;
    private YouTubeSearch youTubeSearch;
    private ProgressBar loadingProgressBar;
    private String searchQuery;

    public SearchFragment() {
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
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        loadingProgressBar = (ProgressBar) v.findViewById(R.id.progressBar);

        handler = new Handler();
        searchResultsList = new ArrayList<>();

        return v;
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);

        if (visible && isResumed()) {
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getUserVisibleHint()) {
            //do nothing for now
        }
        //4th parameter is null, because playlists are not needed to this fragment
        youTubeSearch = new YouTubeSearch(getActivity(), this);
        youTubeSearch.setYouTubeVideosReceiver(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        videosFoundListView = (DynamicListView) getListView();

        setupAdapter();
        addListeners();
    }

    /**
     * Setups custom adapter which enables animations when adding elements
     */
    private void setupAdapter() {
        videoListAdapter = new VideosAdapter(getActivity(), searchResultsList);
        SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(videoListAdapter);
        animationAdapter.setAbsListView(videosFoundListView);
        videosFoundListView.setAdapter(animationAdapter);
    }

    /**
     * Search for query on youTube by using YouTube Data API V3
     * @param query
     */
    public void searchQuery(String query) {
        Log.d(TAG, "Search query: " + query);
        if (searchQuery != null) {
            if (!searchQuery.equals(query)) { //check so on new query, it wont call onScroll and trigger last element event
                preLast = 0;
            }
        }
        searchQuery = query;

        //initially set adapter
        if (!searchResultsList.isEmpty()) {
            searchResultsList.clear();
        }

        Log.d(TAG, "SET IT VISIBLE");
        loadingProgressBar.setVisibility(View.VISIBLE);
        youTubeSearch.searchVideos(query);
    }

    /**
     * Adds listener for item list selection and starts BackgroundAudioService
     */
    private void addListeners() {

        videosFoundListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos,
                                    long id) {
                Toast.makeText(getContext(), "Playing: " + searchResultsList.get(pos).getTitle(), Toast.LENGTH_SHORT).show();

                SnappyDb.getInstance().insert(searchResultsList.get(pos));

                Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
                serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
                serviceIntent.putExtra(Config.YOUTUBE_MEDIA_TYPE, Config.YOUTUBE_VIDEO);
                serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, searchResultsList.get(pos));
                getActivity().startService(serviceIntent);
            }
        });
    }

    /**
     * Called when video items are received
     *
     * @param youTubeVideos - videos to be shown in list view
     */
    @Override
    public void onVideosReceived(ArrayList<YouTubeVideo> youTubeVideos) {
        searchResultsList.addAll(youTubeVideos);

        handler.post(new Runnable() {
            public void run() {
                if (videoListAdapter != null) {
                    videoListAdapter.notifyDataSetChanged();
                    loadingProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
}
