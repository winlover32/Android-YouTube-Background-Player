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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.smedic.tubtub.BackgroundAudioService;
import com.smedic.tubtub.R;
import com.smedic.tubtub.adapters.VideosAdapter;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.interfaces.YouTubeVideosReceiver;
import com.smedic.tubtub.model.ItemType;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.NetworkConf;
import com.smedic.tubtub.youtube.YouTubeSearch;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles list of the videos searched on YouTube
 * Created by smedic on 7.3.16..
 */
public class SearchFragment extends BaseFragment implements YouTubeVideosReceiver {

    private static final String TAG = "SMEDIC search frag";
    private ListView videosFoundListView;
    private Handler handler;
    private ArrayList<YouTubeVideo> searchResultsList;
    private ArrayList<YouTubeVideo> scrollResultsList;
    private VideosAdapter videoListAdapter;
    private YouTubeSearch youTubeSearch;
    private ProgressBar loadingProgressBar;
    private NetworkConf networkConf;

    private int onScrollIndex = 0;
    private int mPrevTotalItemCount = 0;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
        searchResultsList = new ArrayList<>();
        scrollResultsList = new ArrayList<>();
        networkConf = new NetworkConf(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        videosFoundListView = (ListView) v.findViewById(R.id.fragment_list_items);
        loadingProgressBar = (ProgressBar) v.findViewById(R.id.fragment_progress_bar);
        videoListAdapter = new VideosAdapter(getActivity(), searchResultsList, false);
        videoListAdapter.setOnItemEventsListener(this);
        videosFoundListView.setAdapter(videoListAdapter);

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);

        addListeners();
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

    /**
     * Search for query on youTube by using YouTube Data API V3
     *
     * @param query
     */
    public void searchQuery(String query) {
        //check network connectivity
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);
        onScrollIndex = 0;
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
                //check network connectivity
                if (!networkConf.isNetworkAvailable()) {
                    networkConf.createNetErrorDialog();
                    return;
                }

                Toast.makeText(getContext(), "Playing: " + searchResultsList.get(pos).getTitle(), Toast.LENGTH_SHORT).show();

                YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).create(searchResultsList.get(pos));

                Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
                serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
                serviceIntent.putExtra(Config.YOUTUBE_TYPE, ItemType.YOUTUBE_MEDIA_TYPE_VIDEO);
                serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, searchResultsList.get(pos));
                getActivity().startService(serviceIntent);
            }
        });

        videosFoundListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                //if specified number of videos is added, do not load more
                if (totalItemCount < Config.NUMBER_OF_VIDEOS_RETURNED) {
                    if (view.getAdapter() != null && ((firstVisibleItem + visibleItemCount) >= totalItemCount) && totalItemCount != mPrevTotalItemCount) {
                        mPrevTotalItemCount = totalItemCount;
                        addMoreData();
                    }
                }
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

        videosFoundListView.smoothScrollToPosition(0);
        searchResultsList.clear();
        scrollResultsList.clear();
        scrollResultsList.addAll(youTubeVideos);

        handler.post(new Runnable() {
            public void run() {
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        });

        addMoreData();
    }

    /**
     * Called when playlist cannot be found
     * NOT USED in this fragment
     *
     * @param playlistId
     * @param errorCode
     */
    @Override
    public void onPlaylistNotFound(String playlistId, int errorCode) {

    }

    /**
     * Adds 10 items at the bottom of the list when list is scrolled to the end (10th element)
     * 50 is max number of videos
     * If number is between, so no full step is available (step is 10), add elements:
     * scrollResultsList.size() % 10
     */
    private void addMoreData() {

        List<YouTubeVideo> subList;
        if (scrollResultsList.size() < (onScrollIndex + 10)) {
            subList = scrollResultsList.subList(onScrollIndex, scrollResultsList.size());
            onScrollIndex += (scrollResultsList.size() % 10);
        } else {
            subList = scrollResultsList.subList(onScrollIndex, onScrollIndex + 10);
            onScrollIndex += 10;
        }

        if (!subList.isEmpty()) {
            searchResultsList.addAll(subList);
            handler.post(new Runnable() {
                public void run() {
                    if (videoListAdapter != null) {
                        videoListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }
}
