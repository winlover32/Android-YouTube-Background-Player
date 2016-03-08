package com.smedic.tubtub;

import java.io.Serializable;

/**
 * Created by smedic on 3.2.16..
 */
public class VideoItem implements Serializable {
    private String title;
    //private String description;
    private String thumbnailURL;
    private String id;
    private String duration;

    public VideoItem() {
        this.title = "";
        this.thumbnailURL = "";
        this.id = "";
        this.duration = "";
    }

    public VideoItem(String title, String thumbnailURL, String id, String duration) {
        this.title = title;
        this.thumbnailURL = thumbnailURL;
        this.id = id;
        this.duration = duration;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    //public String getDescription() { return description; }
    //public void setDescription(String description) { this.description = description; }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnail) {
        this.thumbnailURL = thumbnail;
    }

    @Override
    public String toString() {
        return "VideoItem {" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    /*@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VideoItem videoItem = (VideoItem) o;

        if (!title.equals(videoItem.title)) return false;
        if (thumbnailURL != null ? !thumbnailURL.equals(videoItem.thumbnailURL) : videoItem.thumbnailURL != null)
            return false;
        if (!id.equals(videoItem.id)) return false;
        return !(duration != null ? !duration.equals(videoItem.duration) : videoItem.duration != null);

    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + (thumbnailURL != null ? thumbnailURL.hashCode() : 0);
        result = 31 * result + id.hashCode();
        result = 31 * result + (duration != null ? duration.hashCode() : 0);
        return result;
    }*/
}
