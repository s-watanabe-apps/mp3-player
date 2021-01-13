package com.swapps.mp3player;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ListAdapter extends ArrayAdapter<Item> {
    private Context context;
    private int resource;
    private List<Item> items;
    private LayoutInflater inflater;
    private SharedPreferences preferences;

    public ListAdapter(Context context, int resource, List<Item> items) {
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
        int normalTextColor;
        int sortTextColor;
        if(background == 0) {
            backgroundColor = Color.parseColor(context.getString(R.string.list_color_white_background));
            filenameColor = Color.parseColor(context.getString(R.string.list_color_white_filename));
            normalTextColor = Color.parseColor(context.getString(R.string.list_color_white_normal_text));
            sortTextColor = Color.parseColor(context.getString(R.string.list_color_white_sort_text));
        } else{
            backgroundColor = Color.parseColor(context.getString(R.string.list_color_black_background));
            filenameColor = Color.parseColor(context.getString(R.string.list_color_black_filename));
            normalTextColor = Color.parseColor(context.getString(R.string.list_color_black_normal_text));
            sortTextColor = Color.parseColor(context.getString(R.string.list_color_black_sort_text));
        }

        int sort = preferences.getInt("sort", 0);

        LinearLayout layoutList = view.findViewById(R.id.layout_list);
        layoutList.setBackgroundColor(backgroundColor);

        // リストビューに表示する要素を取得
        final Item item = items.get(position);

        // ファイル名
        TextView fileName = view.findViewById(R.id.file_name);
        fileName.setText(item.getName());
        fileName.setTextColor(filenameColor);

        // サイズ
        TextView fileSize = view.findViewById(R.id.file_size);
        fileSize.setText((item.getSize() / 60) + ":" + String.format("%02d", item.getSize() % 60));
        if(sort == 1) {
            fileSize.setTextColor(sortTextColor);
        } else{
            fileSize.setTextColor(normalTextColor);
        }

        // 最終更新
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        TextView jsonLastModified = view.findViewById(R.id.file_last_modified);
        jsonLastModified.setText(format.format(item.getLastModified()));
        if(sort == 2) {
            jsonLastModified.setTextColor(sortTextColor);
        } else{
            jsonLastModified.setTextColor(normalTextColor);
        }

        // パス名
        TextView jsonPath = view.findViewById(R.id.file_path);
        jsonPath.setText(item.getPath());
        if(sort == 0) {
            jsonPath.setTextColor(sortTextColor);
        } else{
            jsonPath.setTextColor(normalTextColor);
        }

        final CheckBox itemCheck = view.findViewById(R.id.item_check);
        itemCheck.setChecked(item.isChecked());
        itemCheck.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (!item.isChecked()) {
                    item.setChecked(true);
                } else {
                    item.setChecked(false);
                }
                itemCheck.setChecked(item.isChecked());
            }
        });

        return view;
    }
}
