package com.swapps.mp3player;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final Map<String, List<Item>> items;
    private final Map<String, Boolean> groupChecks;
    private AdapterInterface listener;

    public ExpandableListAdapter(Context context, Map<String, List<Item>> items) {
        this.context = context;
        this.items = items;
        SharedPreferences preferences = context.getSharedPreferences(SettingActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);

        groupChecks = new HashMap<>();
        for (String key : items.keySet()) {
            groupChecks.put(key, false);
        }
    }

    public void setListener(AdapterInterface listener) {
        this.listener = listener;
    }

    private String getKey(int position) {
        return new ArrayList(items.keySet()).get(position).toString();
    }

    /**
     * 親要素の数を取得
     * @return int
     */
    @Override
    public int getGroupCount() {
        return items.size();
    }

    /**
     * 子要素の数を取得
     * @param groupPosition int
     * @return int
     */
    @Override
    public int getChildrenCount(int groupPosition) {
        return Objects.requireNonNull(items.get(getKey(groupPosition))).size();
    }

    /**
     * 親要素を取得
     * @param groupPosition int
     * @return Object
     */
    @Override
    public Object getGroup(int groupPosition) {
        return getKey(groupPosition);
    }

    /**
     * 子要素を取得
     * @param groupPosition int
     * @param childPosition int
     * @return Object
     */
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return Objects.requireNonNull(items.get(getKey(groupPosition))).get(childPosition);
    }

    /**
     * 親要素の固有IDを取得
     * @param groupPosition int
     * @return long
     */
    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    /**
     * 子要素の固有IDを取得
     * @param groupPosition int
     * @return long
     */
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    /**
     * 固有IDを持つかどうか
     * @return boolean
     */
    @Override
    public boolean hasStableIds() {
        return false;
    }

    /**
     * 親要素のレイアウトを生成
     *
     * @param groupPosition int
     * @param isExpanded boolean
     * @param convertView View
     * @param parent ViewGroup
     * @return View
     */
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.dir_list_layout, parent, false);
        }

        String key = getKey(groupPosition);

        // アイコン
        ImageView icon = convertView.findViewById(R.id.dir_icon);
        icon.setImageResource(isExpanded ? R.drawable.dir_open : R.drawable.dir_close);

        // ディレクトリ名
        TextView viewDirName = convertView.findViewById(R.id.dir_name);
        String[] array = key.split("/");
        viewDirName.setText(array[array.length - 1]);

        // ディレクトリパス
        TextView viewDirPath = convertView.findViewById(R.id.dir_path);
        viewDirPath.setText(key.substring(0, key.lastIndexOf("/") + 1));

        // グループチェックボックス
        CheckBox dirCheck = convertView.findViewById(R.id.dir_check);
        dirCheck.setChecked(Boolean.TRUE.equals(groupChecks.get(getKey(groupPosition))));
        dirCheck.setOnClickListener(v -> {
            boolean checked = ((CheckBox) v).isChecked();

            groupChecks.put(getKey(groupPosition), checked);

            for (Item item : Objects.requireNonNull(items.get(getKey(groupPosition)))) {
                item.setChecked(checked);
            }

            listener.updateRequest();
        });

        return convertView;
    }

    /**
     * 子要素のレイアウトを生成
     *
     * @param groupPosition int
     * @param childPosition int
     * @param isLastChild boolean
     * @param convertView View
     * @param parent ViewGroup
     * @return View
     */
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Log.d("test", "getChildView:" + groupPosition + ", " + childPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_list_layout, parent, false);
        }

        // リストビューに表示する要素を取得
        final Item item = Objects.requireNonNull(items.get(getKey(groupPosition))).get(childPosition);

        // ファイル名
        TextView viewItemName = convertView.findViewById(R.id.file_name);
        viewItemName.setText(item.getName());

        // サイズ
        TextView fileSize = convertView.findViewById(R.id.file_size);
        fileSize.setText((item.getSize() / 60) + ":" + String.format("%02d", item.getSize() % 60));

        // アイテムチェックボックス
        CheckBox itemCheck = convertView.findViewById(R.id.item_check);
        itemCheck.setChecked(item.isChecked());

        itemCheck.setOnClickListener(v -> {
            boolean checked = ((CheckBox) v).isChecked();
            Objects.requireNonNull(items.get(getKey(groupPosition))).get(childPosition).setChecked(checked);
            listener.updateRequest();
        });

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
