package com.imber.spotifystreamer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

public class DataFragment extends Fragment{
    ArrayList<Data> dataList = new ArrayList<>();

    public DataFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public ArrayList<Data> getDataList() {
        return dataList;
    }

    public void addData(Data data) {
        this.dataList.add(data);
    }

    // wrapper for data
    class Data {
        Bitmap bitmap;
        String text;

        Data(Bitmap bitmap, String text) {
            this.bitmap = bitmap;
            this.text = text;
        }
    }
}
