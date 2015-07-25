package com.imber.spotifystreamer.adapters;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.imber.spotifystreamer.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class TrackViewAdapter extends ArrayAdapter<TrackViewAdapter.TrackData> {
    Context mContext;

    public TrackViewAdapter(Context context, int listItemLayoutId, ArrayList<TrackData> data) {
        super(context, listItemLayoutId, data);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TrackData data = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_detail, null);
        }

        if (convertView.getTag() == null) {
            convertView.setTag(new ViewHolder(convertView));
        }
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.trackTextView.setText(data.trackName);
        viewHolder.albumTextView.setText(data.albumName);

        if (data.trackPictureUrl != null) {
            Picasso.with(getContext())
                    .load(data.trackPictureUrl)
                    .placeholder(R.drawable.default_album)
                    .into(viewHolder.imageView);
        } else {
            Picasso.with(getContext())
                    .load(R.drawable.default_album)
                    .into(viewHolder.imageView);
        }

        return convertView;
    }

    //wrapper class for storing needed artist data for rotation purposes
    public static class TrackData implements Parcelable {
        public String trackPictureUrl;
        public String trackName;
        public String albumName;
        public String artistName;
        public String trackId;
        public String trackUrl;

        public TrackData(String trackPictureUrl, String trackName, String albumName, String artistName,
                         String trackId, String trackUrl) {
            this.trackPictureUrl = trackPictureUrl;
            this.trackName = trackName;
            this.albumName = albumName;
            this.artistName = artistName;
            this.trackId = trackId;
            this.trackUrl = trackUrl;
        }

        public static final Parcelable.Creator<TrackData> CREATOR
                = new Parcelable.Creator<TrackData>() {
            public TrackData createFromParcel(Parcel in) {
                return new TrackData(in.readString(), in.readString(), in.readString(), in.readString(),
                        in.readString(), in.readString());
            }

            public TrackData[] newArray(int size) {
                return new TrackData[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(trackPictureUrl);
            dest.writeString(trackName);
            dest.writeString(albumName);
            dest.writeString(artistName);
            dest.writeString(trackId);
            dest.writeString(trackUrl);
        }
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