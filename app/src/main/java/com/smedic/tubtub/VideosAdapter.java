package com.smedic.tubtub;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by smedic on 8.2.16..
 */
public class VideosAdapter extends ArrayAdapter<VideoItem> {

    private final static String TAG = "SMEDIC VideosAdapter";
    private boolean[] itemChecked;

    private Activity context;
    private final List<VideoItem> list;

    public VideosAdapter(Activity context, List<VideoItem> list) {
        super(context, R.layout.video_item, list);
        this.list = list;
        this.context = context;
        itemChecked = new boolean[500];
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = context.getLayoutInflater().inflate(R.layout.video_item, parent, false);
        }
        ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
        TextView title = (TextView) convertView.findViewById(R.id.video_title);
        TextView duration = (TextView) convertView.findViewById(R.id.video_duration);

        VideoItem searchResult = list.get(position);

        Picasso.with(context).load(searchResult.getThumbnailURL()).into(thumbnail);
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
}
