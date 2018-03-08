package com.dkadem.bulut.downsong.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dkadem.bulut.downsong.R;
import com.dkadem.bulut.downsong.nesneler.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dkadem on 03.08.2016.
 */
public class SongAdapter extends BaseAdapter{

    private LayoutInflater mInflater;
    private List<Song> mSarkiListesi;

    public SongAdapter(Context ctx, List<Song> songs){
        mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSarkiListesi = songs;
    }

    @Override
    public int getCount() {
        return mSarkiListesi.size();
    }

    @Override
    public Song getItem(int position) {
        return mSarkiListesi.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View myView = mInflater.inflate(R.layout.line_song,null);
        TextView textSarkiAdi = (TextView)myView.findViewById(R.id.textSarkiAdi);
        TextView textSarkici = (TextView)myView.findViewById(R.id.textSanatci);
        TextView textSure = (TextView)myView.findViewById(R.id.textSure);

        Song song = mSarkiListesi.get(position);
        textSarkiAdi.setText(song.name);
        textSarkici.setText(song.author);
        textSure.setText(song.time);

        return myView;


    }
}

