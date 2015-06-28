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

public class ArtistViewAdapter extends ArrayAdapter<ArtistViewAdapter.ArtistData> {
    Context mContext;

    public ArtistViewAdapter(Context context, int listItemLayoutId, ArrayList<ArtistData> artistData) {
        super(context, listItemLayoutId, artistData);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ArtistData data = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_artist, null);
        }

        if (convertView.getTag() == null) {
            convertView.setTag(new ViewHolder(convertView));
        }
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.textView.setText(data.artistName);

        if (data.artistPictureUrl != null) {
            Picasso.with(mContext)
                    .load(data.artistPictureUrl)
                    .placeholder(R.drawable.default_person)
                    .into(viewHolder.imageView);
        } else {
            Picasso.with(getContext())
                    .load(R.drawable.default_person)
                    .into(viewHolder.imageView);
        }
        return convertView;
    }

    //wrapper class for storing needed artist data for rotation purposes
    public static class ArtistData implements Parcelable {
        public String artistPictureUrl;
        public String artistName;
        public String artistId;

        public ArtistData(String artistPictureUrl, String artistName, String artistId) {
            this.artistPictureUrl = artistPictureUrl;
            this.artistName = artistName;
            this.artistId = artistId;
        }

        public static final Parcelable.Creator<ArtistData> CREATOR
                = new Parcelable.Creator<ArtistData>() {
            public ArtistData createFromParcel(Parcel in) {
                return new ArtistData(in.readString(), in.readString(), in.readString());
            }

            public ArtistData[] newArray(int size) {
                return new ArtistData[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(artistPictureUrl);
            dest.writeString(artistName);
            dest.writeString(artistId);
        }
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView textView;

        ViewHolder(View view) {
            this.imageView = (ImageView) view.findViewById(R.id.list_item_artist_image);
            this.textView = (TextView) view.findViewById(R.id.list_item_artist_text);
        }
    }
}
