package com.smedic.tubtub;

import java.io.Serializable;

/**
 * Created by smedic on 3.2.16..
 */
public class YouTubeVideo implements Serializable {
    private String id;
    private String title;
    private String thumbnailURL;
    private String duration;

    public YouTubeVideo() {
        this.id = "";
        this.title = "";
        this.thumbnailURL = "";
        this.duration = "";
    }

    public YouTubeVideo(String id, String title, String thumbnailURL, String duration) {
        this.id = id;
        this.title = title;
        this.thumbnailURL = thumbnailURL;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnail) {
        this.thumbnailURL = thumbnail;
    }

    @Override
    public String toString() {
        return "YouTubeVideo {" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
