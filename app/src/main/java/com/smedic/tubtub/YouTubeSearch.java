package com.smedic.tubtub;

import android.app.Activity;
import android.os.Handler;
import android.support.v4.app.Fragment;
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
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.smedic.tubtub.fragments.PlaylistsFragment;
import com.smedic.tubtub.interfaces.YouTubeVideosReceiver;
import com.smedic.tubtub.utils.Auth;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.SnappyDb;
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
    private String appName;

    private Handler handler;
    private Activity activity;

    private YouTube youtube;

    private Fragment playlistFragment;

    private String mChosenAccountName;

    private YouTubeVideosReceiver youTubeVideosReceiver;

    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;
    private GoogleAccountCredential credential;

    public YouTubeSearch(Activity activity, Fragment playlistFragment, YouTubeVideosReceiver youTubeVideosReceiver) {
        this.activity = activity;
        this.playlistFragment = playlistFragment;
        handler = new Handler();
        credential = GoogleAccountCredential.usingOAuth2(activity.getApplicationContext(), Arrays.asList(Auth.SCOPES));
        credential.setBackOff(new ExponentialBackOff());
        appName = activity.getResources().getString(R.string.app_name);
        this.youTubeVideosReceiver = youTubeVideosReceiver;
    }

    public void buildYouTube0() { //TODO RECONSIDER LOGIC ABOUT THIS - important
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {

            }
        }).setApplicationName(appName).build();
    }

    public void setAuthSelectedAccountName(String authSelectedAccountName) {
        this.mChosenAccountName = authSelectedAccountName;
        credential.setSelectedAccountName(mChosenAccountName);
    }

    public GoogleAccountCredential getCredential() {
        return credential;
    }

    public void searchPlaylists(final ArrayList<YouTubePlaylist> playlistsList,
                                final PlaylistsFragment.PlaylistAdapter playlistAdapter) {
        new Thread() {
            public void run() {
                youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                        .setApplicationName(appName).build();
                try {
                    ChannelListResponse channelListResponse = youtube.channels().list("snippet").setMine(true).execute();

                    Log.d(TAG, "Playlist acquiring...");

                    List<Channel> channelList = channelListResponse.getItems();
                    if (channelList.isEmpty()) {
                        Log.d(TAG, "Can't find user channel");
                    }
                    Channel channel = channelList.get(0);

                    Log.d(TAG, "Name: " + channel.getSnippet().getTitle() + " , getId: " + channel.getId());

                    YouTube.Playlists.List searchList = youtube.playlists().list("id,snippet,contentDetails,status").setKey(Config.YOUTUBE_API_KEY);

                    searchList.setChannelId(channel.getId());
                    searchList.setFields("items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status)");
                    searchList.setMaxResults((long) 50);

                    PlaylistListResponse playListResponse = searchList.execute();
                    List<Playlist> playlists = playListResponse.getItems();

                    if (playlists != null) {

                        Iterator<Playlist> iteratorPlaylistResults = playlists.iterator();

                        if (!iteratorPlaylistResults.hasNext()) {
                            Log.d(TAG, " There aren't any results for your query.");
                        }

                        //remove existing playlists
                        playlistsList.clear();

                        while (iteratorPlaylistResults.hasNext()) {
                            Playlist playlist = iteratorPlaylistResults.next();

                            YouTubePlaylist playlistItem = new YouTubePlaylist(playlist.getSnippet().getTitle(),
                                    playlist.getSnippet().getThumbnails().getDefault().getUrl(),
                                    playlist.getId(),
                                    playlist.getContentDetails().getItemCount(),
                                    playlist.getStatus().getPrivacyStatus());
                            playlistsList.add(playlistItem);
                            SnappyDb.getInstance().insertPlaylist(playlistItem); //save to Snappy DB
                        }
                        handler.post(new Runnable() {
                            public void run() {
                                if (playlistAdapter != null) {
                                    playlistAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                } catch (UserRecoverableAuthIOException e) {
                    playlistFragment.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public void acquirePlaylistVideos(final String playlistId) {

        // Define a list to store items in the list of uploaded videos.
        new Thread(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "Chosen name: " + mChosenAccountName);
                credential.setSelectedAccountName(mChosenAccountName);

                youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                        .setApplicationName(appName).build();

                ArrayList<PlaylistItem> playlistItemList = new ArrayList<PlaylistItem>();
                ArrayList<YouTubeVideo> playlistItems = new ArrayList<>();

                try {
                    // Retrieve the playlist of the channel's uploaded videos.
                    YouTube.PlaylistItems.List playlistItemRequest = youtube.playlistItems().list("id,contentDetails,snippet");
                    playlistItemRequest.setPlaylistId(playlistId);

                    // Only retrieve data used in this application, thereby making
                    // the application more efficient. See:
                    // https://developers.google.com/youtube/v3/getting-started#partial
                    playlistItemRequest.setFields("items(contentDetails/videoId,snippet/title," +
                            "snippet/thumbnails/default/url),nextPageToken");
                    String nextToken = "";

                    //videos to get duration
                    YouTube.Videos.List videosList = youtube.videos().list("id,contentDetails");
                    videosList.setKey(Config.YOUTUBE_API_KEY);
                    videosList.setFields("items(contentDetails/duration)");

                    // Call the API one or more times to retrieve all items in the
                    // list. As long as the API response returns a nextPageToken,
                    // there are still more items to retrieve.
                    //do {
                    playlistItemRequest.setPageToken(nextToken);
                    PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();
                    playlistItemList.addAll(playlistItemResult.getItems());

                    //save all ids from searchList list in order to find video list
                    StringBuilder contentDetails = new StringBuilder();

                    int ii = 0;
                    for (PlaylistItem result : playlistItemList) {
                        contentDetails.append(result.getContentDetails().getVideoId());
                        if (ii < 49)
                            contentDetails.append(",");
                        ii++;
                    }

                    //find video list
                    videosList.setId(contentDetails.toString());
                    VideoListResponse resp = videosList.execute();
                    List<Video> videoResults = resp.getItems();

                    Iterator<PlaylistItem> pit = playlistItemList.iterator();
                    Iterator<Video> vit = videoResults.iterator();
                    while (pit.hasNext()) {
                        PlaylistItem playlistItem = pit.next();
                        Video videoItem = vit.next();

                        YouTubeVideo youTubeVideo = new YouTubeVideo();

                        youTubeVideo.setId(playlistItem.getContentDetails().getVideoId());
                        youTubeVideo.setTitle(playlistItem.getSnippet().getTitle());
                        youTubeVideo.setThumbnailURL(playlistItem.getSnippet().getThumbnails().getDefault().getUrl());

                        //video info
                        if (videoItem != null) {
                            String isoTime = videoItem.getContentDetails().getDuration();
                            String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                            youTubeVideo.setDuration(time);
                        } else {
                            youTubeVideo.setDuration("NA");
                        }

                        playlistItems.add(youTubeVideo);
                    }

                    nextToken = playlistItemResult.getNextPageToken();
                    //} while (nextToken != null);

                    youTubeVideosReceiver.receive(playlistItems);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    //method for searching only 50 videos
    public void searchVideos(final String keywords, final ArrayList<YouTubeVideo> searchResultsList,
                             final VideosAdapter videoListAdapter) {

        new Thread() {
            public void run() {
                try {

                    YouTube.Search.List searchList;
                    YouTube.Videos.List videosList;

                    searchList = youtube.search().list("id,snippet");
                    searchList.setKey(Config.YOUTUBE_API_KEY);
                    searchList.setType("video");
                    searchList.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                    searchList.setFields("items(id/videoId,snippet/title,snippet/thumbnails/default/url)");

                    videosList = youtube.videos().list("id,contentDetails");
                    videosList.setKey(Config.YOUTUBE_API_KEY);
                    videosList.setFields("items(contentDetails/duration)");

                    //search
                    searchList.setQ(keywords);
                    SearchListResponse searchListResponse = searchList.execute();
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
                    ArrayList<YouTubeVideo> items = new ArrayList<>();
                    for (int i = 0; i < searchResults.size(); i++) {
                        YouTubeVideo item = new YouTubeVideo();
                        //searchList list info
                        item.setTitle(searchResults.get(i).getSnippet().getTitle());
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

                    searchResultsList.addAll(items);

                    handler.post(new Runnable() {
                        public void run() {
                            if (videoListAdapter != null) {
                                videoListAdapter.notifyDataSetChanged();
                            }
                        }
                    });

                } catch (IOException e) {
                    Log.e(TAG, "Could not initialize: " + e);
                    e.printStackTrace();
                    return;
                }
            }
        }.start();
    }
}