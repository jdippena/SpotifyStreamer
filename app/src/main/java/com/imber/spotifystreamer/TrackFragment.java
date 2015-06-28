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
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.imber.spotifystreamer.adapters.TrackViewAdapter;
import com.imber.spotifystreamer.adapters.TrackViewAdapter.TrackData;

import java.util.ArrayList;
import java.util.HashMap;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RetrofitError;

public class TrackFragment extends Fragment {
    private final String LOG_TAG = getClass().getSimpleName();
    private Context mContext;
    private TrackViewAdapter mTrackAdapter;
    private ArrayList<TrackData> mTrackData;
    private final String TRACK_DATA_TAG = "track_data";

    public TrackFragment newInstance() {
        return new TrackFragment();
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
            //set status and action bar colors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                parent.getWindow().setStatusBarColor(intent.getIntExtra(paletteIntentLabelBar, Color.DKGRAY));
            }
            if (parent.getSupportActionBar() != null) {
                ActionBar bar = parent.getSupportActionBar();
                bar.setBackgroundDrawable(new ColorDrawable(
                        intent.getIntExtra(paletteIntentLabel, Color.GRAY)
                ));
                String artistName = intent.getStringExtra(getString(R.string.artist_name_intent_label));
                bar.setSubtitle(artistName);
            }

            //get artist id from intent
            String artistId = intent.getStringExtra(Intent.EXTRA_TEXT);
            String countryCode = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString(getString(R.string.pref_country_code_key),
                            getString(R.string.pref_country_code_default));
            if (savedInstanceState == null) {
                new FetchTracksAndAlbum().execute(artistId, countryCode);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_track_layout, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.artist_detail_list);
        if (savedInstanceState == null) {
            mTrackData = new ArrayList<>();
        } else {
            mTrackData = savedInstanceState.getParcelableArrayList(TRACK_DATA_TAG);
        }
        mTrackAdapter = new TrackViewAdapter(mContext, R.layout.list_item_detail, mTrackData);
        listView.setAdapter(mTrackAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(mContext, Player.class)
                        .putExtra(Intent.EXTRA_TEXT, mTrackAdapter.getItem(position).trackId);
                startActivity(intent);
            }
        });
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(mContext, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(TRACK_DATA_TAG, mTrackData);
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
            if (result != null) {
                mTrackAdapter.clear();
                mTrackData = Utility.createTrackDataArrayList(mContext, result);
                mTrackAdapter.addAll(mTrackData);
            }
        }
    }
}
