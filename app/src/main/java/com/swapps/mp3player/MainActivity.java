package com.swapps.mp3player;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.swapps.mp3player.comparator.DateComparator;
import com.swapps.mp3player.comparator.NameComparator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements AdapterInterface {

    public static final int REQUEST_PERMISSION_EXTERNAL_STORAGE = 1;
    public static final int REQUEST_CODE_SETTING = 2;
    public static final int REQUEST_CODE_MP3 = 3;
    public static final String CACHE_FILE_NAME = "cache.json";

    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    ProgressDialog progressDialog;
    Handler handler;
    ExpandableListView listView;
    Map<String, List<Item>> mapItems;
    ExpandableListAdapter adapter;
    SharedPreferences preferences;
    List<String> extensions;

    // 全面広告
    private InterstitialAd interstitialAd;
    public static int AD_LOAD_TIMEOUT = 30;
    private int adLoadCount = 0;

    private void showInterstitial() {
        if(interstitialAd != null && interstitialAd.isLoaded()) {
            interstitialAd.show();
        } else{
            goToNextLevel();
        }
    }

    private void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().setRequestAgent("android_studio:ad_template").build();
        interstitialAd.loadAd(adRequest);
    }

    private void goToNextLevel() {
        interstitialAd = newInterstitialAd();
        loadInterstitial();
    }

    private InterstitialAd newInterstitialAd() {
        InterstitialAd interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                //Log.d(getClass().getName(), "onAdLoaded()");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                //Log.d(getClass().getName(), "onAdFailedToLoad(" + errorCode + ")");
            }

            @Override
            public void onAdClosed() {
                //Log.d(getClass().getName(), "onAdClosed()");
                goToNextLevel();
            }
        });

        return interstitialAd;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        checkPermission();
    }

    /**
     * 権限確認
     */
    public void checkPermission() {
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.permission_read_external_storage_title))
                    .setMessage(getString(R.string.permission_read_external_storage_body))
                    .setCancelable(false)
                    .setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> finish())
                    .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> requestPermissions(PERMISSIONS_STORAGE, REQUEST_PERMISSION_EXTERNAL_STORAGE))
                    .show();
        } else{
            init();
        }
    }

    /**
     * パーミッション設定イベント
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                finish();
            }
        }
    }
    
    /**
     * 画面初期化処理
     */
    private void init() {
        preferences = getSharedPreferences(SettingActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);

        interstitialAd = null;
        interstitialAd = newInterstitialAd();
        loadInterstitial();

        // 検索対象
        extensions = new ArrayList<>();
        extensions.add("mp3");
        if (preferences.getInt(SettingActivity.SETTING_INCLUDE_MP4, 0) == 1) {
            extensions.add("mp4");
        }

        listView = findViewById(R.id.list_view);

        listView.setOnGroupClickListener((parent, v, groupPosition,  id) -> {
            //Log.d("test", "isChildSelectable:" + parent.getExpandableListAdapter().isChildSelectable(groupPosition, 0));

            return false;
        });

        listView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            ArrayList<String> paths = new ArrayList<>();
            for(List<Item> items : mapItems.values()) {
                for(Item item : items) {
                    if (item.isChecked()) {
                        paths.add(item.getPath() + "/" + item.getName());
                    }
                }
            }

            if (paths.size() == 0) {
                Item item = (Item) adapter.getChild(groupPosition, childPosition);
                paths.add(item.getPath() + "/" + item.getName());
            }
            Intent intent = new Intent(MainActivity.this, Mp3Activity.class);
            intent.putStringArrayListExtra("paths", paths);
            startActivityForResult(intent, REQUEST_CODE_MP3);

            return false;
        });

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle(getString(R.string.search_json_file));
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        handler = new Handler();

        new Thread(() -> {
            // 内部ストレージ
            String path = Environment.getExternalStorageDirectory().getPath();
            searchFiles(path);

            final Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    handler.post(() -> {
                        if((interstitialAd != null && interstitialAd.isLoaded()) || adLoadCount >= AD_LOAD_TIMEOUT) {
                            // 準備OK
                            timer.cancel();
                            progressDialog.dismiss();
                            if(interstitialAd != null) {
                                int count = preferences.getInt("count", 0);
                                if (count % 3 == 1) {
                                    showInterstitial();
                                }
                                SharedPreferences.Editor editor = preferences.edit();
                                count++;
                                editor.putInt("count", count);
                                editor.apply();
                            }
                        } else{
                            // ロード中
                            adLoadCount++;
                            //Log.d("debug", "ad_load:" + adLoadCount);
                            if(adLoadCount >= AD_LOAD_TIMEOUT){
                                interstitialAd = null;
                            }
                        }
                    });
                }
            },0,500);
        }).start();
    }

    /**
     * ファイル検索してViewにセット
     * @param path 検索する場所
     */
    private void searchFiles(String path) {
        if ((mapItems = readCache()) == null) {
            List<Item> listItems = searchTargetFiles(path);

            mapItems = new HashMap<>();

            for (Item item : listItems) {
                if (!mapItems.containsKey(item.getPath())) {
                    mapItems.put(item.getPath(), new ArrayList<>());
                }
                Objects.requireNonNull(mapItems.get(item.getPath())).add(item);
            }

            writeCache(mapItems);
        }

        // 出力結果をリストビューに表示
        handler.post(this::setListView);
    }

    /**
     * 対象ファイル検索
     * @param path 検索する場所
     */
    private List<Item> searchTargetFiles(final String path) {
        handler.post(() -> progressDialog.setMessage(path));

        List<Item> listItems = new ArrayList<>();
        final File directory = new File(path);
        if (!directory.canRead()) {
            return new ArrayList<>();
        }

        // java.io.File クラスのメソッド list()
        // 指定したディレクトリに含まれるファイル、ディレクトリの一覧を String 型の配列で返す。
        String[] files = directory.list();
        assert files != null;
        for (String fileName : files){

            File subFile;
            subFile = new File(directory.getPath() + "/" + fileName);

            if(subFile.isDirectory()){
                //Log.d(TAG, "dir:" + subFile.getName());
                listItems.addAll(searchTargetFiles(subFile.getPath()));
                //listDirectory.add(directory.getPath() + "/" + fileName[n]);
            } else{
                int pos = subFile.getName().lastIndexOf(".");

                if (pos != -1) {
                    String fileExtensions = subFile.getName().substring(pos + 1).toLowerCase();
                    Log.d("test", "file:" + subFile.getName() + " -> " + fileExtensions);
                    if(subFile.getName().charAt(0) != '.'
                            && extensions.contains(fileExtensions)) {
                        Item item = new Item();
                        item.setName(subFile.getName());
                        item.setPath(subFile.getParent());
                        item.setSize(MediaUtils.getDuration(subFile));
                        item.setLastModified(getCreatedAt(subFile.getPath()));
                        listItems.add(item);
                    }
                }
            }
        }

        return listItems;
    }

    /**
     * ファイルの作成日時.
     * @param path 対象ファイルパス
     * @return Date
     */
    public Date getCreatedAt(String path) {
        try {
            BasicFileAttributes basicAttr;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                basicAttr = Files.readAttributes(Paths.get(path), BasicFileAttributes.class);
            } else {
                return null;
            }

            return new Date(basicAttr.creationTime().toMillis());
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * ExpandableListViewセット
     */
    public void setListView() {
        int sort = preferences.getInt("sort", 0);
        for (String key : mapItems.keySet()) {
            if (sort == 0) {
                Collections.sort(Objects.requireNonNull(mapItems.get(key)), new NameComparator());
            } else {
                Collections.sort(Objects.requireNonNull(mapItems.get(key)), new DateComparator());
            }
        }

        adapter = new ExpandableListAdapter(this, mapItems);
        adapter.setListener(this);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.main_menu_orderby:
                // 並べ替え
                final String[] itemList = getResources().getStringArray(R.array.sort_names);

                final int sort = preferences.getInt("sort", 0);
                final SharedPreferences.Editor editor = preferences.edit();
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_menu_sort_by_size)
                        .setTitle(getString(R.string.sort_title))
                        .setSingleChoiceItems(itemList, sort, (dialog, whichButton) -> {
                            editor.putInt("sort", whichButton);
                            editor.apply();
                        })
                        .setPositiveButton(getString(R.string.dialog_ok), (dialog, whichButton) -> {
                            if(sort != preferences.getInt("sort", 0)) {
                                setListView();
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), (dialog, whichButton) -> {
                            if(sort != preferences.getInt("sort", 0)) {
                                editor.putInt("sort", sort);
                                editor.apply();
                            }
                        })
                        .show();
                break;
            case R.id.main_menu_setting:
                // 設定
                Intent intent = new Intent(this, SettingActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SETTING);
                break;
            default:
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case (REQUEST_CODE_SETTING):
                // SettingActivityから戻ってきた場合
                if (resultCode == RESULT_OK) {
                    init();
                } else if (resultCode == RESULT_CANCELED) {
                    setListView();
                } else {
                    //その他
                    //Toast.makeText(this, "What!?", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_MP3:
                // Mp3Activityから戻ってきた場合
                //if (resultCode == RESULT_OK) {
                //    int pos = data.getIntExtra("index", -1);
                //    if(pos > -1) {
                //        File file = new File(listItems.get(pos).getPath() + "/" + listItems.get(pos).getName());
                //        listItems.get(pos).setSize(file.length());
                //        listItems.get(pos).setLastModified(new Date(file.lastModified()));
                //        adapter.notifyDataSetChanged();
                //    }
                //}
            default:
                break;
        }
    }

    /**
     * 検索結果をキャッシュから取得.
     * @return Map<String, List<Item>> Item
     */
    private Map<String, List<Item>> readCache() {
        try {
            File file = new File(getFilesDir(), CACHE_FILE_NAME);
            if (!file.canRead()) {
                return null;
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Path path = Paths.get(file.getPath());
                FileTime fileTime = Files.getLastModifiedTime(path);
                Date now = new Date();

                //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                //Log.d("test", "now:" + format.format(now));
                //Log.d("test", "fileTime:" + format.format(new Date(fileTime.toMillis())));

                long diff = (now.getTime() - fileTime.toMillis()) / 1000;
                //Log.d("test", "diff:" + diff);
                if (diff > (60 * 60 * 24)) {
                    return null;
                }
            } else {
                return null;
            }

            FileInputStream inputStream = openFileInput(file.getName());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            String lineBuffer;
            String json = "";
            while (true){
                lineBuffer = reader.readLine();
                if (lineBuffer != null){
                    json += lineBuffer;
                } else {
                    break;
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<Map<String, List<Item>>>() {});
        } catch (Exception e) {
            Log.e("ERROR", Objects.requireNonNull(e.getMessage()));
            return null;
        }
    }

    /**
     * 検索結果をキャッシュへ出力.
     * @param items Map<String, List<Item>>
     */
    private void writeCache(Map<String, List<Item>> items) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(items);
            FileOutputStream outputStream = openFileOutput(CACHE_FILE_NAME, Context.MODE_PRIVATE);
            outputStream.write(json.getBytes());
        } catch (Exception e) {
            Log.e("ERROR", Objects.requireNonNull(e.getMessage()));
        }
    }

    /**
     * アダプターからの更新依頼
     */
    @Override
    public void updateRequest() {
        adapter.notifyDataSetChanged();
    }
}
