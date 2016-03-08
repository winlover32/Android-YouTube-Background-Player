package com.smedic.tubtub.utils;

import android.content.Context;
import android.util.Log;

import com.smedic.tubtub.VideoItem;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import java.util.ArrayList;

/**
 * Created by smedic on 9.2.16..
 */
public class SnappyDb {

    private static final String TAG = "SMEDIC SNAPPY DB";
    private static final String KEY_PREFIX = "yt_id:";
    private DB snappyDB;
    private boolean isInitialized = false;

    private static SnappyDb ourInstance = new SnappyDb();

    public static SnappyDb getInstance() {
        return ourInstance;
    }

    private SnappyDb() {
    }

    public boolean init(Context context, String databaseName) {
        try {
            snappyDB = DBFactory.open(context, databaseName);
        } catch (SnappydbException e) {
            e.printStackTrace();
            return false;
        }
        isInitialized = true;
        return true;
    }

    public boolean insertVideo(VideoItem item) {
        if (!isInitialized) return false;

        try {
            snappyDB.put(KEY_PREFIX + item.getId(), item);
            Log.d(TAG, "Inserted video: " + KEY_PREFIX + item.getId() + ", DB size: " + snappyDB.countKeys(KEY_PREFIX));
        } catch (SnappydbException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public VideoItem getVideoItem(String key) {
        if (!isInitialized) return new VideoItem(); //TODO reconsider

        if (!key.contains(KEY_PREFIX)) { //if key does not contain "magic" word, add it
            key = KEY_PREFIX + key;
        }

        VideoItem videoItem = null;
        try {
            videoItem = snappyDB.getObject(key, VideoItem.class);
            Log.d(TAG, "Got video: " + key + ", DB size: " + snappyDB.countKeys(KEY_PREFIX));
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        return videoItem;
    }

    public ArrayList<VideoItem> getAllVideoItems() {
        if (!isInitialized) return new ArrayList<>();

        ArrayList<VideoItem> outList = new ArrayList<>();
        try {
            String[] keys = snappyDB.findKeys(KEY_PREFIX);
            for (int i = 0; i < keys.length; i++) {
                VideoItem tempItem = getVideoItem(keys[i]);
                outList.add(tempItem);
            }
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        return outList;
    }

    public boolean removeVideo(String key) {
        if (!isInitialized) return false;
        try {
            snappyDB.del(KEY_PREFIX + key);
            Log.d(TAG, "Removed video: " + KEY_PREFIX + key + ", DB size: " + snappyDB.countKeys(KEY_PREFIX));
        } catch (SnappydbException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean removeAllVideos() {
        if (!isInitialized) return false;

        try {
            String[] keys = snappyDB.findKeys(KEY_PREFIX);
            for (int i = 0; i < keys.length; i++) {
                snappyDB.del(keys[i]);
            }
        } catch (SnappydbException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean close() {
        if (!isInitialized) return false;

        try {
            snappyDB.close();
        } catch (SnappydbException e) {
            e.printStackTrace();
            return false;
        }

        isInitialized = false;
        return true;
    }
}
