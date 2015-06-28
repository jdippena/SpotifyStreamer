package com.imber.spotifystreamer;

import android.content.Context;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class Utility {
    public static String getArtistImageURL(Context context, Artist a) {
        float imageSize = context.getResources().getDimension(R.dimen.thumbnail_height);
        List<Image> images = a.images;
        int pos = 0;
        for (Image image : images) {
            if (imageSize <= Math.min(image.height, image.width)) pos = images.indexOf(image);
        }
        return images.get(pos).url;
    }

    public static String getTrackAlbumArtUrl(Context context, Track t) {
        float imageSize = context.getResources().getDimension(R.dimen.thumbnail_height);
        List<Image> images = t.album.images;
        int pos = 0;
        for (Image image : images) {
            if (imageSize <= Math.min(image.height, image.width)) pos = images.indexOf(image);
        }
        return images.get(pos).url;
    }

    public static String formatTrackLength(long ms) {
        int totalSeconds = (int) ms / 1000;
        int hours = totalSeconds / (3600);
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = (totalSeconds % 3600) % 60;
        String result = "";
        if (hours > 0) result += hours + ":";
        result += minutes + ":";
        return String.format(result + "%02d", seconds);
    }
}
