package com.smedic.tubtub.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.SimpleSwipeUndoAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.nhaarman.listviewanimations.util.Swappable;
import com.smedic.tubtub.R;
import com.smedic.tubtub.VideoItem;
import com.smedic.tubtub.utils.SnappyDb;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Created by smedic on 7.3.16..
 */
public class RecentlyWatchedFragment extends Fragment {

    private static final String TAG = "SMEDIC RecentlyWatched";
    private ArrayList<VideoItem> recentlyPlayedVideos;

    private DynamicListView recentlyPlayedListView;
    private Handler handler;
    private boolean[] itemChecked;
    private VideoListAdapter videoListAdapter;
    private String query;

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
        recentlyPlayedListView = (DynamicListView) v.findViewById(R.id.recently_played);  //TODO make use of just one layout file, not two

        recentlyPlayedVideos = new ArrayList<>();
        setupListViewAndAdapter();

        handler = new Handler();
        itemChecked = new boolean[500];

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getUserVisibleHint()){
            Log.d(TAG, "not getUserVisibleHint");
            //return;
        }

        recentlyPlayedVideos.clear();
        recentlyPlayedVideos.addAll(SnappyDb.getInstance().getAllVideoItems());
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);
        Log.d(TAG, "setUserVisibleHint");
        if (visible && isResumed()){
            Log.d(TAG, "setUserVisibleHint visible and resumed");
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    private void setupListViewAndAdapter() {

        /* Setup the adapter */
        videoListAdapter = new VideoListAdapter(getActivity());
        SimpleSwipeUndoAdapter simpleSwipeUndoAdapter = new SimpleSwipeUndoAdapter(videoListAdapter, getContext(), new MyOnDismissCallback(videoListAdapter));
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

    void addListeners() {
        recentlyPlayedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, final int pos,
                                    long id) {
                Toast.makeText(getContext(), "Playing: " + recentlyPlayedVideos.get(pos), Toast.LENGTH_SHORT).show();

                /*new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //Intent serviceIntent = new Intent(getContext(), YouTubeService.class);
                        //serviceIntent.putExtra("YT_URL", recentlyPlayedVideos.get(pos).getId());
                        //startService(serviceIntent);
                    }
                }).start();*/
            }
        });
    }

    private class VideoListAdapter extends ArrayAdapter<VideoItem> implements Swappable, UndoAdapter {

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

            VideoItem searchResult = recentlyPlayedVideos.get(position);

            Picasso.with(getContext()).load(searchResult.getThumbnailURL()).into(thumbnail);
            title.setText(searchResult.getTitle());
            duration.setText(searchResult.getDuration());

            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                    Log.d(TAG, "OnChecked changed to " + isChecked);
                    itemChecked[position] = isChecked;
                }
            });

            checkBox.setChecked(itemChecked[position]);

            if (itemChecked[position]) {
                convertView.setSelected(true);
            }
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
            VideoItem firstItem = getItem(i);

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

    private class MyOnDismissCallback implements OnDismissCallback {

        private final ArrayAdapter<VideoItem> mAdapter;

        @Nullable
        private Toast mToast;

        MyOnDismissCallback(final ArrayAdapter<VideoItem> adapter) {
            mAdapter = adapter;
        }

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
