package com.imber.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import retrofit.RetrofitError;

public class MainFragment extends Fragment implements MainActivity.Callback{
    Context mContext;
    private final String LOG_TAG = getClass().getSimpleName();
    private ArtistViewAdapter mArtistAdapter;
    private String mPaletteIntentLabel;
    private String mPaletteIntentLabelBar;
    private String mArtistNameIntentLabel;

    public MainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mPaletteIntentLabel = getString(R.string.palette_intent_label);
        mPaletteIntentLabelBar  = getString(R.string.palette_intent_label_status_bar);
        mArtistNameIntentLabel = getString(R.string.artist_name_intent_label);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_layout, container, false);
        mContext = getActivity();

        ListView listView = (ListView) rootView.findViewById(R.id.main_list_view);
        if (mArtistAdapter == null) {
            mArtistAdapter = new ArtistViewAdapter(mContext,
                    R.layout.list_item_artist,
                    new ArrayList<Artist>());
        }
        listView.setAdapter(mArtistAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int paletteColorMain;
                int paletteColorBar;
                //get palette colors
                if (((ArtistViewAdapter) parent.getAdapter()).defaultPictureArray[position]) {
                    paletteColorMain = Color.GRAY;
                    paletteColorBar = Color.DKGRAY;
                } else {
                    ImageView imageView = (ImageView) view.findViewById(R.id.list_item_artist_image);
                    Bitmap b = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                    paletteColorMain = Palette.from(b).generate().getMutedColor(Color.GRAY);
                    paletteColorBar = (paletteColorMain & 0xfefefefe) >> 1;
                }

                Intent detailIntent = new Intent(mContext, ArtistDetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, mArtistAdapter.getItem(position).id)
                        .putExtra(mArtistNameIntentLabel, mArtistAdapter.getItem(position).name)
                        .putExtra(mPaletteIntentLabel, paletteColorMain)
                        .putExtra(mPaletteIntentLabelBar, paletteColorBar);
                startActivity(detailIntent);
            }
        });
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(mContext, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void updateResults(String newText) {
        new FetchArtistAndPicture().execute(newText);
    }

    @Override
    public void submitResults(String finalQuery) {
        if (mArtistAdapter.isEmpty()) {
            Toast.makeText(mContext, "No results found", Toast.LENGTH_SHORT).show();
        }
    }

    class FetchArtistAndPicture extends AsyncTask<String, Void, ArrayList<Artist>> {
        @Override
        protected ArrayList<Artist> doInBackground(String... params) {
            ArrayList<Artist> data = new ArrayList<>();

            SpotifyService service = new SpotifyApi().getService();
            String artistName = params[0];
            try {
                data.addAll(service.searchArtists(artistName).artists.items);
            } catch (RetrofitError e) {
                Log.e(LOG_TAG, e.getMessage() + "Spotify Service didn't get all artists");
            }
            return data;
        }

        @Override
        protected void onPostExecute(ArrayList<Artist> result) {
            if (result != null) {
                mArtistAdapter.clear();
                mArtistAdapter.addAll(result);
            }
        }
    }

    class ArtistViewAdapter extends ArrayAdapter<Artist> {
        boolean[] defaultPictureArray;

        ArtistViewAdapter(Context context, int listItemLayoutId, ArrayList<Artist> artists) {
            super(context, listItemLayoutId, artists);
        }

        @Override
        public void notifyDataSetChanged() {
            defaultPictureArray = new boolean[getCount()];
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Artist a = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_artist, null);
            }

            ((TextView) convertView.findViewById(R.id.list_item_artist_text)).setText(a.name);

            ImageView imageView = (ImageView) convertView.findViewById(R.id.list_item_artist_image);
            if (a.images.size() > 0) {
                Picasso.with(mContext)
                        .load(getArtistImageURL(a)).placeholder(R.drawable.default_person)
                        .into(imageView);
                defaultPictureArray[position] = false;
            } else {
                Picasso.with(getContext())
                        .load(R.drawable.default_person).into(imageView);
                defaultPictureArray[position] = true;
            }

            return convertView;
        }

        private String getArtistImageURL(Artist a) {
            float scale = getResources().getDisplayMetrics().density;
            float imageWidth = 100 * scale;
            List<Image> images = a.images;
            int pos = 0;
            for (Image image : images) {
                if (imageWidth <= Math.min(image.height, image.width)) pos = images.indexOf(image);
            }
            return images.get(pos).url;
        }
    }
}
