package com.smedic.tubtub.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by smedic on 23.3.16..
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SMEDIC" , "onRecive");
    }
}
