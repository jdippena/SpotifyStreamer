package com.imber.spotifystreamer;

import android.content.Context;

import com.imber.spotifystreamer.adapters.ArtistViewAdapter.ArtistData;
import com.imber.spotifystreamer.adapters.TrackViewAdapter.TrackData;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class Utility {
    public static String getArtistImageURL(Context context, Artist a) {
        if (a.images.size() == 0) return null;
        float imageSize = context.getResources().getDimension(R.dimen.thumbnail_height);
        List<Image> images = a.images;
        int pos = 0;
        for (Image image : images) {
            if (imageSize <= Math.min(image.height, image.width)) pos = images.indexOf(image);
        }
        return images.get(pos).url;
    }

    public static ArrayList<ArtistData> createArtistDataArrayList(Context context, List<Artist> artistList) {
        ArrayList<ArtistData> data = new ArrayList<>(artistList.size());
        for (Artist a : artistList) {
            data.add(new ArtistData(
                    getArtistImageURL(context, a),
                    a.name,
                    a.id));
        }
        return data;
    }

    public static String getTrackAlbumArtUrl(Context context, Track t) {
        if (t.album.images.size() == 0) return null;
        float imageSize = context.getResources().getDimension(R.dimen.thumbnail_height);
        List<Image> images = t.album.images;
        int pos = 0;
        for (Image image : images) {
            if (imageSize <= Math.min(image.height, image.width)) pos = images.indexOf(image);
        }
        return images.get(pos).url;
    }

    public static ArrayList<TrackData> createTrackDataArrayList(Context context, List<Track> trackList) {
        ArrayList<TrackData> data = new ArrayList<>(trackList.size());
        for (Track t : trackList) {
            data.add(new TrackData(
                    getTrackAlbumArtUrl(context, t),
                    t.name,
                    t.album.name,
                    t.id));
        }
        return data;
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
