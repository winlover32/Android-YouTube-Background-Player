package com.smedic.tubtub.youtube;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by smedic on 13.2.17..
 */

public class YouTubeVideosLoader extends AsyncTaskLoader<List<YouTubeVideo>> {

    private static final String TAG = "SMEDIC";

    private YouTube youtube = YouTubeSingleton.getYouTube();
    private String keywords;

    public YouTubeVideosLoader(Context context, String keywords) {
        super(context);
        this.keywords = keywords;
    }

    @Override
    public List<YouTubeVideo> loadInBackground() {

        ArrayList<YouTubeVideo> items = new ArrayList<>();
        try {
            YouTube.Search.List searchList = youtube.search().list("id");
            YouTube.Videos.List videosList = youtube.videos().list("id,contentDetails,statistics,snippet");

            searchList.setKey(Config.YOUTUBE_API_KEY);
            searchList.setType("video"); //TODO ADD PLAYLISTS SEARCH
            searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
            searchList.setFields("items(id/kind,id/videoId)");

            videosList.setKey(Config.YOUTUBE_API_KEY);
            videosList.setFields("items(id,contentDetails/duration,statistics/viewCount,snippet/title,snippet/thumbnails/default/url)");

            //search
            searchList.setQ(keywords);
            SearchListResponse searchListResponse = searchList.execute();
            List<SearchResult> searchResults = searchListResponse.getItems();

            //find video list
            videosList.setId(Utils.concatenateIDs(searchResults));  //save all ids from searchList list in order to find video list
            VideoListResponse resp = videosList.execute();
            List<Video> videoResults = resp.getItems();

            for (Video video : videoResults) {

                YouTubeVideo item = new YouTubeVideo();
                item.setTitle(video.getSnippet().getTitle());
                item.setThumbnailURL(video.getSnippet().getThumbnails().getDefault().getUrl());
                item.setId(video.getId());

                if (video.getStatistics() != null) {
                    BigInteger viewsNumber = video.getStatistics().getViewCount();
                    String viewsFormatted = NumberFormat.getIntegerInstance().format(viewsNumber) + " views";
                    item.setViewCount(viewsFormatted);
                }
                if (video.getContentDetails() != null) {
                    String isoTime = video.getContentDetails().getDuration();
                    String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                    item.setDuration(time);
                }
                items.add(item);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "loadInBackground: return " + items.size());
        return items;
    }

    @Override
    public void deliverResult(List<YouTubeVideo> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            return;
        }
        super.deliverResult(data);
    }
}
