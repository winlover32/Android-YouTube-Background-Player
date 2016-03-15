package com.smedic.tubtub.utils;

import android.content.Context;
import android.util.Log;

import com.smedic.tubtub.YouTubePlaylist;
import com.smedic.tubtub.YouTubeVideo;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import java.util.ArrayList;

/**
 * Wrapper class for SnappyDB NoSql database
 * Enables adding and removing videos and playlists from DB
 * Created by smedic on 9.2.16..
 */
public class SnappyDb {

    private static final String TAG = "SMEDIC SNAPPY DB";
    private static String KEY_PREFIX_VIDEO = "yt_id:";
    private static String KEY_PREFIX_PLAYLIST = "yt_pl_id:";
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

    public boolean insert(YouTubeVideo item) {
        if (!isInitialized) return false;

        try {
            snappyDB.put(KEY_PREFIX_VIDEO + item.getId(), item);
            if (Config.DEBUG)
                Log.d(TAG, "Inserted video: " + KEY_PREFIX_VIDEO + item.getId() + ", DB size: " + snappyDB.countKeys(KEY_PREFIX_VIDEO));
        } catch (SnappydbException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean insertPlaylist(YouTubePlaylist item) {
        if (!isInitialized) return false;

        try {
            snappyDB.put(KEY_PREFIX_PLAYLIST + item.getId(), item);
            if (Config.DEBUG)
                Log.d(TAG, "Inserted playlist: " + KEY_PREFIX_PLAYLIST + item.getId() + ", DB size: " + snappyDB.countKeys(KEY_PREFIX_PLAYLIST));
        } catch (SnappydbException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public YouTubeVideo getVideoItem(String key) {
        if (!isInitialized) return new YouTubeVideo(); //TODO reconsider

        if (!key.contains(KEY_PREFIX_VIDEO)) { //if key does not contain "magic" word, add it
            key = KEY_PREFIX_VIDEO + key;
        }

        YouTubeVideo videoItem = null;
        try {
            videoItem = snappyDB.getObject(key, YouTubeVideo.class);
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        return videoItem;
    }

    public YouTubePlaylist getPlaylistItem(String key) {
        if (!isInitialized) return new YouTubePlaylist(); //TODO reconsider

        if (!key.contains(KEY_PREFIX_PLAYLIST)) { //if key does not contain "magic" word, add it
            key = KEY_PREFIX_PLAYLIST + key;
        }

        YouTubePlaylist playlistItem = null;
        try {
            playlistItem = snappyDB.getObject(key, YouTubePlaylist.class);
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        return playlistItem;
    }

    public ArrayList<YouTubeVideo> getAllVideoItems() {
        if (!isInitialized) return new ArrayList<>();

        ArrayList<YouTubeVideo> outList = new ArrayList<>();
        try {
            String[] keys = snappyDB.findKeys(KEY_PREFIX_VIDEO);
            for (int i = 0; i < keys.length; i++) {
                YouTubeVideo tempItem = getVideoItem(keys[i]);
                outList.add(tempItem);
            }
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        return outList;
    }

    public ArrayList<YouTubePlaylist> getAllPlaylistItems() {
        if (!isInitialized) return new ArrayList<>();

        ArrayList<YouTubePlaylist> outList = new ArrayList<>();
        try {
            String[] keys = snappyDB.findKeys(KEY_PREFIX_PLAYLIST);
            for (int i = 0; i < keys.length; i++) {
                YouTubePlaylist tempItem = getPlaylistItem(keys[i]);
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
            snappyDB.del(KEY_PREFIX_VIDEO + key);
            if (Config.DEBUG)
                Log.d(TAG, "Removed video: " + KEY_PREFIX_VIDEO + key + ", DB size: " + snappyDB.countKeys(KEY_PREFIX_VIDEO));
        } catch (SnappydbException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean removePlaylist(String key) {
        if (!isInitialized) return false;
        try {
            snappyDB.del(KEY_PREFIX_PLAYLIST + key);
            if (Config.DEBUG)
                Log.d(TAG, "Removed Playlist: " + KEY_PREFIX_PLAYLIST + key + ", DB size: " + snappyDB.countKeys(KEY_PREFIX_PLAYLIST));
        } catch (SnappydbException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean removeAllVideos() {
        if (!isInitialized) return false;

        try {
            String[] keys = snappyDB.findKeys(KEY_PREFIX_VIDEO);
            for (int i = 0; i < keys.length; i++) {
                snappyDB.del(keys[i]);
            }
        } catch (SnappydbException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean removeAllPlaylists() {
        if (!isInitialized) return false;

        try {
            String[] keys = snappyDB.findKeys(KEY_PREFIX_PLAYLIST);
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
