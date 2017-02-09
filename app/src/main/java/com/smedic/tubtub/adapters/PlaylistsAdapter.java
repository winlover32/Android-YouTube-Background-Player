package com.smedic.tubtub.adapters;

/**
 * Created by smedic on 6.2.17..
 */

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.smedic.tubtub.R;
import com.smedic.tubtub.interfaces.ItemEventsListener;
import com.smedic.tubtub.model.ItemType;
import com.smedic.tubtub.model.YouTubePlaylist;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Custom array adapter class
 */
public class PlaylistsAdapter extends ArrayAdapter<YouTubePlaylist> {

    private Activity context;
    private List<YouTubePlaylist> playlists;
    private ItemEventsListener itemEventsListener;

    public PlaylistsAdapter(Activity context, List<YouTubePlaylist> playlists) {
        super(context, R.layout.video_item, playlists);
        this.context = context;
        this.playlists = playlists;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = context.getLayoutInflater().inflate(R.layout.playlist_item, parent, false);
        }
        ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
        TextView title = (TextView) convertView.findViewById(R.id.playlist_title);
        TextView videosNumber = (TextView) convertView.findViewById(R.id.videos_number);
        TextView privacy = (TextView) convertView.findViewById(R.id.privacy);
        ImageView shareButton = (ImageView) convertView.findViewById(R.id.share_button);

        final YouTubePlaylist searchResult = playlists.get(position);

        Picasso.with(getContext()).load(searchResult.getThumbnailURL()).into(thumbnail);
        title.setText(searchResult.getTitle());
        String videosNumberText = context.getString(R.string.number_of_videos) + String.valueOf(searchResult.getNumberOfVideos());
        videosNumber.setText(videosNumberText);
        String status = context.getString(R.string.status) + searchResult.getStatus();
        privacy.setText(status);

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemEventsListener != null) {
                    itemEventsListener.onShareClicked(ItemType.YOUTUBE_MEDIA_TYPE_PLAYLIST,
                            searchResult.getId());
                }
            }
        });

        return convertView;
    }

    public void setOnItemEventsListener(ItemEventsListener listener) {
        itemEventsListener = listener;
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).hashCode();
    }


    @Override
    public boolean hasStableIds() {
        return true;
    }
}