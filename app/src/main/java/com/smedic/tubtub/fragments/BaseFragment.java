package com.smedic.tubtub.fragments;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.smedic.tubtub.interfaces.ItemEventsListener;
import com.smedic.tubtub.model.ItemType;
import com.smedic.tubtub.model.YouTubeVideo;

/**
 * Created by smedic on 9.2.17..
 */

public class BaseFragment extends Fragment implements ItemEventsListener {

    @Override
    public void onShareClicked(ItemType type, String videoId) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=" + videoId);
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this song!");
        startActivity(Intent.createChooser(intent, "Share"));
    }

    @Override
    public void onFavoriteClicked(YouTubeVideo video, boolean isChecked) {

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

}
