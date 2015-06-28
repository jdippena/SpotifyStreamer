package com.imber.spotifystreamer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.imber.spotifystreamer.R;
import com.imber.spotifystreamer.Utility;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

public class TrackViewAdapter extends ArrayAdapter<Track> {
    Context mContext;

    public TrackViewAdapter(Context context, int listItemLayoutId, ArrayList<Track> data) {
        super(context, listItemLayoutId, data);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Track track = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_detail, null);
        }

        if (convertView.getTag() == null) {
            convertView.setTag(new ViewHolder(convertView));
        }
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.trackTextView.setText(track.name);
        viewHolder.albumTextView.setText(track.album.name);

        if (track.album.images.size() > 0) {
            Picasso.with(getContext()).load(Utility.getTrackAlbumArtUrl(mContext, track))
                    .placeholder(R.drawable.default_album)
                    .into(viewHolder.imageView);
        } else {
            Picasso.with(getContext()).load(R.drawable.default_album).into(viewHolder.imageView);
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView trackTextView;
        TextView albumTextView;

        ViewHolder(View view) {
            imageView = (ImageView) view.findViewById(R.id.list_item_detail_image);
            trackTextView = (TextView) view.findViewById(R.id.list_item_detail_track);
            albumTextView = (TextView) view.findViewById(R.id.list_item_detail_album);
        }
    }
}