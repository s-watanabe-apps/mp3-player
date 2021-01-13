package com.swapps.mp3player;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
    private Context context;
    private int resource;
    private List<SongItem> items;
    private LayoutInflater inflater;
    private SharedPreferences preferences;

    public SongListAdapter(Context context, int resource, List<SongItem> items) {
        super(context, resource, items);

        this.context = context;
        this.resource = resource;
        this.items = items;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        preferences = context.getSharedPreferences(SettingActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(resource, null);
        }

        int background = preferences.getInt(SettingActivity.SETTING_BACKGROUND, 0);
        int backgroundColor;
        int filenameColor;
        if(background == 0) {
            backgroundColor = Color.parseColor(context.getString(R.string.list_color_white_background));
            filenameColor = Color.parseColor(context.getString(R.string.list_color_white_filename));
        } else{
            backgroundColor = Color.parseColor(context.getString(R.string.list_color_black_background));
            filenameColor = Color.parseColor(context.getString(R.string.list_color_black_filename));
        }

        int sort = preferences.getInt("sort", 0);

        LinearLayout layoutList = view.findViewById(R.id.layout_song_list);
        layoutList.setBackgroundColor(backgroundColor);

        // リストビューに表示する要素を取得
        final SongItem item = items.get(position);

        // ファイル名
        TextView songName = view.findViewById(R.id.song_name);
        songName.setText(item.getName());
        songName.setTextColor(filenameColor);

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
