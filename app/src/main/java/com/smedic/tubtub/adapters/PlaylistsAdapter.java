package com.smedic.tubtub.adapters;

/**
 * Created by smedic on 6.2.17..
 */

import android.content.Context;
import android.view.LayoutInflater;
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

import static com.smedic.tubtub.R.id.privacy;

/**
 * Custom array adapter class
 */
public class PlaylistsAdapter extends ArrayAdapter<YouTubePlaylist> {

    private Context context;
    private List<YouTubePlaylist> playlists;
    private ItemEventsListener itemEventsListener;
    private ViewHolder holder;

    public PlaylistsAdapter(Context context, List<YouTubePlaylist> playlists) {
        super(context, R.layout.video_item, playlists);
        this.context = context;
        this.playlists = playlists;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (holder == null) {
            holder = new ViewHolder();
        }

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.playlist_item, parent, false);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
            holder.title = (TextView) convertView.findViewById(R.id.playlist_title);
            holder.videosNumber = (TextView) convertView.findViewById(R.id.videos_number);
            holder.privacy = (TextView) convertView.findViewById(privacy);
            holder.shareButton = (ImageView) convertView.findViewById(R.id.share_button);
        }

        final YouTubePlaylist searchResult = playlists.get(position);

        Picasso.with(context).load(searchResult.getThumbnailURL()).into(holder.thumbnail);
        holder.title.setText(searchResult.getTitle());
        String videosNumberText = context.getString(R.string.number_of_videos) + String.valueOf(searchResult.getNumberOfVideos());
        holder.videosNumber.setText(videosNumberText);
        String status = context.getString(R.string.status) + searchResult.getStatus();
        holder.privacy.setText(status);

        holder.shareButton.setOnClickListener(new View.OnClickListener() {
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

    private static class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView videosNumber;
        TextView privacy;
        ImageView shareButton;
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