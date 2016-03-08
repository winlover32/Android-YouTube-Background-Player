package com.smedic.tubtub.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.smedic.tubtub.R;
import com.smedic.tubtub.VideoItem;
import com.smedic.tubtub.VideosAdapter;
import com.smedic.tubtub.YouTubeSearch;
import com.smedic.tubtub.utils.SnappyDb;

import java.util.ArrayList;

/**
 * Created by smedic on 7.3.16..
 */
public class SearchFragment extends ListFragment {

    private static final String TAG = "SMEDIC SEARCH FRAGMENT" ;
    private DynamicListView videosFoundListView;
    private Handler handler;
    private ArrayList<VideoItem> searchResultsList;
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

        handler = new Handler();
        searchResultsList = new ArrayList<>();

        return v;
    }

    @Override
    public void onResume(){
        super.onResume();
        youTubeSearch = new YouTubeSearch(getActivity());
        youTubeSearch.buildYouTube0();
        Log.d(TAG, "onResume finished");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        loadingProgressBar = new ProgressBar(getContext());
        videosFoundListView = (DynamicListView) getListView();

        //setupAdapter();

        addListeners();
    }

    private void setupAdapter() {
        videoListAdapter = new VideosAdapter(getActivity(), searchResultsList);
        //SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(videoListAdapter);
        //animationAdapter.setAbsListView(videosFoundListView);
        videosFoundListView.setAdapter(videoListAdapter);
    }

    public void searchQuery(String query){
        Log.d(TAG, "search query");
        if (searchQuery != null) {
            if (!searchQuery.equals(query)) { //check so on new query, it wont call onScroll and trigger last element event
                preLast = 0;
            }
        }
        searchQuery = query;

        //initially set adapter
        if (videoListAdapter == null) {
            handler.post(new Runnable() {
                public void run() {
                    setupAdapter();
                }
            });
        } else {
            youTubeSearch.searchOnYoutube(query, searchResultsList, videoListAdapter);
        }
    }

    private void addListeners() {

        videosFoundListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos,
                                    long id) {
                SnappyDb.getInstance().insertVideo(searchResultsList.get(pos));

                Toast.makeText(getContext(), "Playing: " + searchResultsList.get(pos), Toast.LENGTH_SHORT).show();

                //Intent serviceIntent = new Intent(getContext(), YouTubeService.class);
                //serviceIntent.putExtra("YT_URL", searchResultsList.get(pos).getId());
                //startService(serviceIntent);
            }
        });

        videosFoundListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //Log.d(TAG, "onScrollStateChanged");
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                switch (view.getId()) {
                    case android.R.id.list:

                        // Sample calculation to determine if the last item is fully visible.
                        final int lastItem = firstVisibleItem + visibleItemCount;
                        if (lastItem > 500) {
                            videosFoundListView.removeFooterView(loadingProgressBar);
                            Toast.makeText(getContext(), "No more videos", Toast.LENGTH_SHORT).show();
                        }

                        if (lastItem == totalItemCount) {
                            if (preLast != lastItem && lastItem < 500) { //avoid multiple calls for last item and loading more than 500 videos - google restriction
                                if (videosFoundListView.getFooterViewsCount() == 0) {
                                    videosFoundListView.addFooterView(loadingProgressBar);
                                }
                                Log.d(TAG, "Last. Search the same query");
                                searchQuery(searchQuery);
                                preLast = lastItem;
                            }
                        }
                }
            }
        });
    }

}
