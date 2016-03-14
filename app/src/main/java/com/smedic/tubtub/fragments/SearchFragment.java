package com.smedic.tubtub.fragments;

import android.content.Intent;
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
        youTubeSearch = new YouTubeSearch(getActivity(), this, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        loadingProgressBar = new ProgressBar(getContext());
        videosFoundListView = (DynamicListView) getListView();

        setupAdapter();
        addListeners();
    }

    private void setupAdapter() {
        videoListAdapter = new VideosAdapter(getActivity(), searchResultsList);
        SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(videoListAdapter);
        animationAdapter.setAbsListView(videosFoundListView);
        videosFoundListView.setAdapter(animationAdapter);
    }

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
        youTubeSearch.searchVideos(query, searchResultsList, videoListAdapter);
    }

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


        videosFoundListView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                return true;
            }
        });

        /*videosFoundListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                //MenuItem item = mode.getMenu().getItem(R.id.menu_item_play);
                final int checkedCount = videosFoundListView.getCheckedItemCount();
                switch (checkedCount) {
                    case 0:
                        //item.setVisible(false);
                        mode.setSubtitle("1 item selected");
                        break;
                    default:
                        //item.setVisible(true);
                        mode.setSubtitle(checkedCount + " items selected");
                        break;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_main, menu);
                MenuItem item = menu.findItem(R.id.menu_item_play);
                item.setVisible(false);

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Log.d(TAG, "onActionItemClicked");
                switch (item.getItemId()) {
                    case R.id.menu_item_play:
                        Toast.makeText(getContext(), "Playing " + videosFoundListView.getCheckedItemCount() +
                                " items", Toast.LENGTH_SHORT).show();
                        ArrayList<YouTubeVideo> youTubeVideos = new ArrayList<>();

                        SparseBooleanArray checked = videosFoundListView.getCheckedItemPositions();
                        for (int i = 0; i < videosFoundListView.getAdapter().getCount(); i++) {
                            if (checked.get(i)) {
                                SnappyDb.getInstance().insert(searchResultsList.get(i));
                                youTubeVideos.add(searchResultsList.get(i));
                            }
                        }
                        Log.d(TAG, "Video list length: " + youTubeVideos.size());

                        Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
                        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
                        serviceIntent.putExtra("YT_MEDIA_TYPE", Config.YOUTUBE_PLAYLIST);
                        serviceIntent.putExtra("YT_PLAYLIST", youTubeVideos);
                        getActivity().startService(serviceIntent);

                        mode.finish();
                        break;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });*/

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
                            //videosFoundListView.removeFooterView(loadingProgressBar);
                            Toast.makeText(getContext(), "No more videos", Toast.LENGTH_SHORT).show();
                        }

                        if (lastItem == totalItemCount) {
                            if (preLast != lastItem && lastItem < 500) { //avoid multiple calls for last item and loading more than 500 videos - google restriction
                                //if (videosFoundListView.getFooterViewsCount() == 0) {
                                //    videosFoundListView.addFooterView(loadingProgressBar);
                                //}
                                Log.d(TAG, "Last. Search the same query");
                                //searchQuery(searchQuery);
                                preLast = lastItem;
                            }
                        }
                }
            }
        });
    }

    @Override
    public void receive(ArrayList<YouTubeVideo> youTubeVideos) {

    }
}
