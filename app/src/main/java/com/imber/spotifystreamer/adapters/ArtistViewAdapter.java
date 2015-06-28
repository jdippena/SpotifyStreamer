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
import com.imber.spotifystreamer.Utility;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Artist;

public class ArtistViewAdapter extends ArrayAdapter<Artist> {
    Context mContext;

    public ArtistViewAdapter(Context context, int listItemLayoutId, ArrayList<Artist> artists) {
        super(context, listItemLayoutId, artists);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Artist a = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_artist, null);
        }

        if (convertView.getTag() == null) {
            convertView.setTag(new ViewHolder(convertView));
        }
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.textView.setText(a.name);

        if (a.images.size() > 0) {
            Picasso.with(mContext)
                    .load(Utility.getArtistImageURL(mContext, a)).placeholder(R.drawable.default_person)
                    .into(viewHolder.imageView);
        } else {
            Picasso.with(getContext())
                    .load(R.drawable.default_person).into(viewHolder.imageView);
        }
        return convertView;
    }

    public static class ArtistData implements Parcelable {
        String artistPictureUrl;
        String artistName;

        public ArtistData(String artistPictureUrl, String artistName) {
            this.artistPictureUrl = artistPictureUrl;
            this.artistName = artistName;
        }

        public static final Parcelable.Creator<ArtistData> CREATOR
                = new Parcelable.Creator<ArtistData>() {
            public ArtistData createFromParcel(Parcel in) {
                return new ArtistData(in.readString(), in.readString());
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
