package com.smedic.tubtub;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.smedic.tubtub.fragments.PlaylistsFragment;
import com.smedic.tubtub.utils.Auth;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by smedic on 18.2.16..
 */
public class YouTubeSearch {

    private static final String TAG = "SMEDIC SEARCH CLASS";
    private static final long NUMBER_OF_VIDEOS_RETURNED = 50; //due to YouTube API rules

    private String prevQuery;
    private Handler handler;
    private Activity activity;

    private String nextPageToken = null;
    private String prevPageToken = null;

    private YouTube.Search.List searchList;
    private YouTube.Videos.List videosList;

    private YouTube youtube;

    private String mChosenAccountName;

    //test
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private static final int REQUEST_GMS_ERROR_DIALOG = 1;
    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;
    private static final int RESULT_PICK_IMAGE_CROP = 4;
    private static final int RESULT_VIDEO_CAP = 5;
    private static final int REQUEST_DIRECT_TAG = 6;
    private GoogleAccountCredential credential;

    public YouTubeSearch(Activity activity) {
        this.activity = activity;

        handler = new Handler();

        ////test
        if(activity == null) {
            Log.d(TAG, "Activity nul ?!?!? ");
        }
        credential = GoogleAccountCredential.usingOAuth2(activity.getApplicationContext(), Arrays.asList(Auth.SCOPES));
        credential.setBackOff(new ExponentialBackOff());
    }

    public void buildYouTube0() {
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {

            }
        }).setApplicationName("YouTubeAudioApp").build();
    }

    public void setAuthSelectedAccountName(String authSelectedAccountName) {
        this.mChosenAccountName = authSelectedAccountName;
        credential.setSelectedAccountName(mChosenAccountName);
    }

    public void searchPlaylists(final ArrayList<PlaylistItem> playlistsList,
                                final PlaylistsFragment.PlaylistAdapter playlistAdapter) {

        new Thread() {
            public void run() {

                youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                        .setApplicationName("YouTubeAudioApp").build();

                try {
                    ChannelListResponse channelListResponse = youtube.channels().list("snippet").setMine(true).execute();

                    List<Channel> channelList = channelListResponse.getItems();
                    if (channelList.isEmpty()) {
                        Log.d(TAG, "Can't find a channel with username: vanste25");
                    }

                    Channel channel = channelList.get(0);

                    Log.d(TAG, "Name: " + channel.getSnippet().getTitle() + " , getId: " + channel.getId());

                    YouTube.Playlists.List searchList = youtube.playlists().list("id,snippet,contentDetails,status").setKey(Config.YOUTUBE_API_KEY);

                    searchList.setChannelId(channel.getId());
                    searchList.setFields("items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status)");
                    searchList.setMaxResults((long) 10);

                    PlaylistListResponse playListResponse = searchList.execute();
                    List<Playlist> playlists = playListResponse.getItems();

                    if (playlists != null) {
                        Iterator<Playlist> iteratorPlaylistResults = playlists.iterator();
                        if (!iteratorPlaylistResults.hasNext()) {
                            Log.d(TAG, " There aren't any results for your query.");
                        }
                        while (iteratorPlaylistResults.hasNext()) {
                            Playlist playlist = iteratorPlaylistResults.next();

                            PlaylistItem playlistItem = new PlaylistItem(playlist.getSnippet().getTitle(),
                                    playlist.getSnippet().getThumbnails().getDefault().getUrl(),
                                    playlist.getId(),
                                    playlist.getContentDetails().getItemCount(),
                                    playlist.getStatus().getPrivacyStatus());
                            Log.d(TAG, "Playlist: " + playlistItem.toString());
                            playlistsList.add(playlistItem);

                        }
                        Log.d(TAG, "playlistsList size >>> : " + playlistsList);
                        handler.post(new Runnable() {
                            public void run() {
                                if (playlistAdapter != null) {
                                    playlistAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                } catch (UserRecoverableAuthIOException e) {
                    Log.d(TAG, "BUILD startActivityForResult");
                    activity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public void searchOnYoutube(final String keywords, final ArrayList<VideoItem> searchResultsList,
                                final VideosAdapter videoListAdapter) {
        new Thread() {
            public void run() {
                try {

                    if (!keywords.equals(prevQuery)) { //if new query comes, clear the list and reset other parameters
                        searchResultsList.clear();
                        prevPageToken = null;
                        nextPageToken = null;

                        if (searchList != null) {
                            if (!searchList.isEmpty()) {
                                searchList.clear();
                            }
                        }
                    }

                    if (prevPageToken == null && nextPageToken == null) {

                        searchList = youtube.search().list("id,snippet");
                        searchList.setKey(Config.YOUTUBE_API_KEY);
                        searchList.setType("video");
                        searchList.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                        searchList.setFields("items(id/videoId,snippet/title,snippet/thumbnails/default/url),nextPageToken,prevPageToken");

                        videosList = youtube.videos().list("id,contentDetails");
                        videosList.setKey(Config.YOUTUBE_API_KEY);
                        videosList.setFields("items(contentDetails/duration)");
                    } else {
                        searchList.setPageToken(nextPageToken);
                    }

                } catch (IOException e) {
                    Log.d(TAG, "Could not initialize: " + e);
                    return;
                }

                searchResultsList.addAll(search(keywords));
                handler.post(new Runnable() {
                    public void run() {
                        if (videoListAdapter != null) {
                            videoListAdapter.notifyDataSetChanged();
                        }
                    }
                });
                prevQuery = keywords;

            }
        }.start();
    }


    public GoogleAccountCredential getCredential() {
        return credential;
    }


    private ArrayList<VideoItem> search(String keywords) {
        searchList.setQ(keywords);
        try {
            SearchListResponse searchListResponse = searchList.execute();

            nextPageToken = searchListResponse.getNextPageToken();
            prevPageToken = searchListResponse.getPrevPageToken();

            List<SearchResult> searchResults = searchListResponse.getItems();

            //save all ids from searchList list in order to find video list
            StringBuilder contentDetails = new StringBuilder();

            int ii = 0;
            for (SearchResult result : searchResults) {
                contentDetails.append(result.getId().getVideoId());
                if (ii < 49)
                    contentDetails.append(",");
                ii++;
            }

            //find video list
            videosList.setId(contentDetails.toString());
            VideoListResponse resp = videosList.execute();
            List<Video> videoResults = resp.getItems();
            //make items for displaying in listView
            ArrayList<VideoItem> items = new ArrayList<>();
            for (int i = 0; i < searchResults.size(); i++) {
                VideoItem item = new VideoItem();
                //searchList list info
                item.setTitle(searchResults.get(i).getSnippet().getTitle());
                //item.setDescription(searchResults.get(i).getSnippet().getDescription());
                item.setThumbnailURL(searchResults.get(i).getSnippet().getThumbnails().getDefault().getUrl());
                item.setId(searchResults.get(i).getId().getVideoId());
                //video info
                if (videoResults.get(i) != null) {
                    String isoTime = videoResults.get(i).getContentDetails().getDuration();
                    String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                    item.setDuration(time);
                } else {
                    item.setDuration("NA");
                }

                //add to the list
                items.add(item);
            }
            return items;

        } catch (IOException e) {
            Log.d(TAG, "Could not search: " + e);
            return null;
        }
    }

    public void searchExtendedInfo(String videoId) {
        try {
            videosList = youtube.videos().list("id,contentDetails");
            videosList.setKey(Config.YOUTUBE_API_KEY);
            videosList.setFields("items(contentDetails/duration/contentRating)");
        } catch (IOException e) {
            Log.d(TAG, "Could not search: " + e);
            return;
        }
    }


    public void searchOnYoutube2(final String keywords, final ArrayList<String> list) {
        searchList.setQ(keywords);
        new Thread() {
            public void run() {
                try {
                    searchList = youtube.search().list("id,snippet");
                    searchList.setKey(Config.YOUTUBE_API_KEY);
                    searchList.setType("video");
                    searchList.setMaxResults(5l);
                    searchList.setFields("items(id/videoId,snippet/title)");

                    SearchListResponse searchListResponse = searchList.execute();
                    List<SearchResult> searchResults = searchListResponse.getItems();

                    //make items for displaying in listView
                    Log.d(TAG, " SIZE : " + searchResults.size());
                    for (int i = 0; i < searchResults.size(); i++) {
                        Log.d(TAG, "ADD TO LIST: " + searchResults.get(i).getSnippet().getTitle());
                        //list.add(searchResults.get(i).getSnippet().getTitle());
                        list.add("peraaaaaaaaa");
                    }
                    Log.d(TAG, " SIZE2 : " + list.size());
                } catch (IOException e) {
                    Log.d(TAG, "Could not initialize: " + e);
                    return;
                }
            }
        }.start();
    }
}