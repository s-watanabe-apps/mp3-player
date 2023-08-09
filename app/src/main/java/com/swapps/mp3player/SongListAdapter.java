package com.swapps.mp3player;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;

public class SongListAdapter extends ArrayAdapter<SongItem> {
    private final Context context;
    private final int resource;
    private final List<SongItem> items;
    private final LayoutInflater inflater;
    private final SharedPreferences preferences;

    public SongListAdapter(Context context, int resource, List<SongItem> items) {
        super(context, resource, items);

        this.context = context;
        this.resource = resource;
        this.items = items;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        preferences = context.getSharedPreferences(SettingActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View view;

        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(resource, null);
        }

        // リストビューに表示する要素を取得
        final SongItem item = items.get(position);

        // ファイル名
        TextView songName = view.findViewById(R.id.song_name);
        songName.setText(item.getName());
        //songName.setTextColor(filenameColor);

        // 総再生時間
        TextView songDuration = view.findViewById(R.id.song_duration);
        songDuration.setText(
                (item.getDuration() / 60) + ":" + String.format("%02d", item.getDuration() % 60)
        );

        // 現在の再生時間
        TextView songNow = view.findViewById(R.id.song_now);
        songNow.setText(
                (item.getNow() / 60) + ":" + String.format("%02d", item.getNow() % 60)
        );

        // アイコン
        ImageView itemPlay = view.findViewById(R.id.item_play);
        if (item.getPlay() == 1) {
            itemPlay.setImageDrawable(context.getDrawable(R.drawable.play));
        } else {
            itemPlay.setImageDrawable(null);
        }

        return view;
    }
}
