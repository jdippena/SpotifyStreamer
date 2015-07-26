package com.imber.spotifystreamer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
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
    private String mArtistName;
    private final String ARTIST_NAME_TAG = "artist_name";

    public static TrackFragment newInstance(Context context, String artistId, String artistName) {
        TrackFragment tf = new TrackFragment();
        Bundle args = new Bundle();
        args.putString(context.getString(R.string.artist_id_label), artistId);
        args.putString(context.getString(R.string.artist_name_label), artistName);
        tf.setArguments(args);
        return tf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mContext = getActivity();
        ActionBarActivity parent = (ActionBarActivity) getActivity();

        Bundle args = getArguments();
        if (args != null && args.containsKey(mContext.getString(R.string.artist_id_label))) {
            ActionBar bar = null;
            if (parent.getSupportActionBar() != null) {
                bar = parent.getSupportActionBar();
            }
            String artistName = args.getString(mContext.getString(R.string.artist_name_label));
            mArtistName = artistName;
            if (bar != null) bar.setSubtitle(artistName);
            String artistId = args.getString(mContext.getString(R.string.artist_id_label));
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
        ListView listView = (ListView) rootView.findViewById(R.id.track_list);
        if (savedInstanceState == null) {
            mTrackData = new ArrayList<>();
        } else {
            mTrackData = savedInstanceState.getParcelableArrayList(TRACK_DATA_TAG);
            mArtistName = savedInstanceState.getString(ARTIST_NAME_TAG);
            ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
            if (bar != null) bar.setSubtitle(mArtistName);
        }
        mTrackAdapter = new TrackViewAdapter(mContext, R.layout.list_item_track, mTrackData);
        listView.setAdapter(mTrackAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // calls ArtistActivity in large layout, TrackActivity in small layout
                try {
                    ((Util.Listeners.OnTrackSelectedListener) getActivity())
                            .onTrackItemSelected(mTrackData, position);
                } catch (ClassCastException e) {
                    throw new ClassCastException(getActivity().toString() +
                            " must implement OnTrackSelectedListener");
                }
            }
        });
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(TRACK_DATA_TAG, mTrackData);
        outState.putString(ARTIST_NAME_TAG, mArtistName);
    }

    // only used for checking if trackFragment needs to be updated in tablet layouts when
    // notification is clicked
    public String getArtistName() {
        return mArtistName;
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
                mTrackData = Util.createTrackDataArrayList(mContext, result);
                mTrackAdapter.addAll(mTrackData);
            }
        }
    }
}
