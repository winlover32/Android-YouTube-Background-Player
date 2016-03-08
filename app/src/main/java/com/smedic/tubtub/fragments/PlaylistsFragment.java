package com.smedic.tubtub.fragments;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.nhaarman.listviewanimations.util.Swappable;
import com.smedic.tubtub.PlaylistItem;
import com.smedic.tubtub.R;
import com.smedic.tubtub.YouTubeSearch;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import javax.annotation.Nullable;

/**
 * Created by smedic on 7.3.16..
 */
public class PlaylistsFragment extends Fragment {

    private static final String TAG = "SMEDIC PlaylistsFrag";

    private ArrayList<PlaylistItem> playlists;
    private DynamicListView playlistsListView;
    private Handler handler;
    private PlaylistAdapter playlistsAdapter;

    public static final String ACCOUNT_KEY = "accountName";
    private String mChosenAccountName;

    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;

    private YouTubeSearch youTubeSearch;
    private boolean[] itemChecked;
    public PlaylistsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY);
        } else {
            loadAccount();
        }
    }

    private void loadAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        mChosenAccountName = sp.getString(ACCOUNT_KEY, null);
    }

    private void saveAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        sp.edit().putString(ACCOUNT_KEY, mChosenAccountName).commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_playlists, container, false);

        /* Setup the ListView */
        playlistsListView = (DynamicListView) v.findViewById(R.id.recently_played);  //TODO make use of just one layout file, not two

        playlists = new ArrayList<>();
        setupListViewAndAdapter();

        handler = new Handler();
        itemChecked = new boolean[500];

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "***** onActivityCreated ******");

        youTubeSearch = new YouTubeSearch(getActivity());
        mChosenAccountName = "vanste25@gmail.com";
        youTubeSearch.setAuthSelectedAccountName(mChosenAccountName);

        youTubeSearch.searchPlaylists(playlists, playlistsAdapter);
    }

    /*@Override
    public void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);
        Log.d(TAG, "2 setUserVisibleHint");
        if (visible && isResumed()){
            Log.d(TAG, "2 setUserVisibleHint visible and resumed");
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();

            youTubeSearch = new YouTubeSearch(getActivity());
            youTubeSearch.setAuthSelectedAccountName(mChosenAccountName);

            youTubeSearch.buildYouTube0();
        }

    }*/

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        Log.d(TAG, "2 setMenuVisibility 0");
        if (visible) {
            Log.d(TAG, "2 setMenuVisibility 1");
            /*youTubeSearch = new YouTubeSearch(getActivity());
            mChosenAccountName = "vanste25@gmail.com";
            youTubeSearch.setAuthSelectedAccountName(mChosenAccountName);

            youTubeSearch.searchPlaylists(playlists, playlistsAdapter);*/
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "on activity result");
        switch (requestCode) {
            case REQUEST_AUTHORIZATION:
                Log.d(TAG, "on activity result 1");
                if (resultCode != Activity.RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                Log.d(TAG, "on activity result 2");
                if (resultCode == Activity.RESULT_OK && data != null
                        && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mChosenAccountName = accountName;
                        youTubeSearch.setAuthSelectedAccountName(accountName);
                        saveAccount();
                    }
                }
                youTubeSearch.searchPlaylists(playlists, playlistsAdapter);
                break;
        }
    }

    private void chooseAccount() {
        Log.d(TAG, "choose account bre");
        startActivityForResult(youTubeSearch.getCredential().newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
    }


    public void setupListViewAndAdapter(){
        playlistsAdapter = new PlaylistAdapter(getActivity());
        SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(playlistsAdapter);
        animationAdapter.setAbsListView(playlistsListView);
        playlistsListView.setAdapter(animationAdapter);

        /* Enable drag and drop functionality */
        playlistsListView.enableDragAndDrop();
        playlistsListView.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                                   final int position, final long id) {
                        playlistsListView.startDragging(position);
                        return true;
                    }
                }
        );

        playlistsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos,
                                    long id) {
                Toast.makeText(getContext(), "Playing playlist: " + playlists.get(pos), Toast.LENGTH_SHORT).show();

                //Intent serviceIntent = new Intent(getContext(), YouTubeService.class);
                //serviceIntent.putExtra("YT_URL", searchResultsList.get(pos).getId());
                //startService(serviceIntent);
            }
        });

    }

    public class PlaylistAdapter extends ArrayAdapter<PlaylistItem> implements Swappable, UndoAdapter  {
        public PlaylistAdapter(Activity context){
            super(context, R.layout.video_item, playlists);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.playlist_item, parent, false);
            }
            ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
            TextView title = (TextView) convertView.findViewById(R.id.playlist_title);
            TextView videosNumber = (TextView) convertView.findViewById(R.id.videos_number);
            TextView privacy = (TextView) convertView.findViewById(R.id.privacy);

            PlaylistItem searchResult = playlists.get(position);

            Picasso.with(getContext()).load(searchResult.getThumbnailURL()).into(thumbnail);
            title.setText(searchResult.getTitle());
            videosNumber.setText("Number of videos: " + String.valueOf(searchResult.getNumberOfVideos()));
            privacy.setText("Status: " + searchResult.getStatus());

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
            PlaylistItem firstItem = getItem(i);

            playlists.set(i, getItem(i1));
            playlists.set(i1, firstItem);

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
}