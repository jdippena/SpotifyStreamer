package com.imber.spotifystreamer;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RetrofitError;

public class ArtistDetailFragment extends Fragment {
    Context mContext;
    TrackViewAdapter mTrackAdapter;
    private final String LOG_TAG = getClass().getSimpleName();

    public ArtistDetailFragment newInstance() {
        return new ArtistDetailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mContext = getActivity();
        ActionBarActivity parent = (ActionBarActivity) getActivity();

        final String paletteIntentLabel = getString(R.string.palette_intent_label);
        final String paletteIntentLabelBar = getString(R.string.palette_intent_label_status_bar);

        Intent intent = parent.getIntent();
        if (intent != null && intent.hasExtra(paletteIntentLabel)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                parent.getWindow().setStatusBarColor(intent.getIntExtra(paletteIntentLabelBar, Color.DKGRAY));
            }
            if (parent.getSupportActionBar() != null) {
                parent.getSupportActionBar()
                        .setBackgroundDrawable(new ColorDrawable(intent
                                .getIntExtra(paletteIntentLabel, Color.GRAY)));
            }

            //get artist id from intent
            String artistId = intent.getStringExtra(Intent.EXTRA_TEXT);
            String countryCode = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString(getString(R.string.pref_country_code_key),
                            getString(R.string.pref_country_code_default));
            new FetchTracksAndAlbum().execute(artistId, countryCode);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_detail_layout, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.artist_detail_list);
        mTrackAdapter = new TrackViewAdapter(mContext, R.layout.list_item_detail, new ArrayList<Track>());
        listView.setAdapter(mTrackAdapter);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_artist_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(mContext, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class FetchTracksAndAlbum extends AsyncTask<String, Void, ArrayList<Track>> {
        @Override
        protected ArrayList<Track> doInBackground(String... params) {
            ArrayList<Track> tracks = new ArrayList<>();

            SpotifyService service = new SpotifyApi().getService();
            String artistId = params[0];
            String countryCode = params[1];
            try {
                HashMap<String, Object> map = new HashMap<>(1);
                map.put("country", countryCode);
                tracks.addAll(service.getArtistTopTrack(artistId, map).tracks);
            } catch (RetrofitError e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            return tracks;
        }

        @Override
        protected void onPostExecute(ArrayList<Track> result) {
            //TODO: what if result is null?
            if (result != null) {
                mTrackAdapter.addAll(result);
            }
        }
    }

    class TrackViewAdapter extends ArrayAdapter<Track> {
        TrackViewAdapter(Context context, int listItemLayoutId, ArrayList<Track> data) {
            super(context, listItemLayoutId, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Track track = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_detail, null);
            }
            String textDisplay = track.name + "\n" + track.album.name;
            ((TextView) convertView.findViewById(R.id.list_item_detail_text)).setText(textDisplay);

            ImageView imageView = (ImageView) convertView.findViewById(R.id.list_item_detail_image);

            if (track.album.images.size() > 0) {
                String url = track.album.images.get(0).url;
                Picasso.with(getContext()).load(url).placeholder(R.drawable.default_album)
                        .into(imageView);
            } else {
                Picasso.with(getContext()).load(R.drawable.default_album).into(imageView);
            }

            return convertView;
        }
    }

}
