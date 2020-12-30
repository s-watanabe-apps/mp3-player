package com.swapps.mp3player;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.swapps.mp3player.comparator.DateComparator;
import com.swapps.mp3player.comparator.DirectoryComparator;
import com.swapps.mp3player.comparator.SizeComparator;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1;
    public static final int REQUEST_CODE_SETTING = 2;
    public static final int REQUEST_CODE_MP3 = 3;
    public static final String tag ="debug";

    ProgressDialog progressDialog;
    Handler handler;
    ListView listView;
    List<Item> listItems;
    ListAdapter adapter;
    SharedPreferences preferences;
    Set<String> hideFiles;

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

        checkPermission();
    }

    /**
     * 権限確認
     * @return void
     */
    public void checkPermission() {
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.permission_read_external_storage_title))
                    .setMessage(getString(R.string.permission_read_external_storage_body))
                    .setCancelable(false)
                    .setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
                        }
                    })
                    .show();
        } else{
            init();
        }
    }

    /**
     * パーミッション設定イベント
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //
                    init();
                } else{
                    //Toast.makeText(this, getString(R.string.permission_no_granted_message), Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }


    /**
     * 画面初期化処理
     * @return void
     */
    private void init() {
        preferences = getSharedPreferences("preferences", Context.MODE_PRIVATE);

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        interstitialAd = null;
        interstitialAd = newInterstitialAd();
        loadInterstitial();

        // 非表示ファイルのロード
        Set<String> oldHideFiles = preferences.getStringSet("hideFiles",  new HashSet<String>());
        hideFiles = new HashSet<>();
        hideFiles.addAll(oldHideFiles);

        listView = findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                ArrayList<String> paths = new ArrayList<>();
                for (Item item : listItems) {
                    if (item.isChecked()) {
                        paths.add(item.getPath() + "/" + item.getName());
                    }
                }
                if (paths.size() == 0) {
                    paths.add(listItems.get(pos).getPath() + "/" + listItems.get(pos).getName());
                }
                Intent intent = new Intent(MainActivity.this, Mp3Activity.class);
                intent.putStringArrayListExtra("paths", paths);
                startActivityForResult(intent, REQUEST_CODE_MP3);
            }
        });

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle(getString(R.string.search_json_file));
        progressDialog.setMessage("---");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                //int storage = preferences.getInt("storage", 0);
                //Log.d(TAG, "storage:" + storage);

                // 内部ストレージ
                String path = Environment.getExternalStorageDirectory().getPath();
                searchFiles(path);

                final Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
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
                                        editor.commit();
                                    }
                                } else{
                                    // ロード中
                                    adLoadCount++;
                                    Log.d(tag, "ad_load:" + adLoadCount);
                                    if(adLoadCount >= AD_LOAD_TIMEOUT){
                                        interstitialAd = null;
                                    }
                                }
                            }
                        });
                    }
                },0,500);
            }
        }).start();
    }

    /**
     * ファイル検索してViewにセット
     * @param path 検索する場所
     */
    private void searchFiles(String path) {
        listItems = searchTargetFiles(path);

        // 出力結果をリストビューに表示
        handler.post(new Runnable() {
            @Override
            public void run() {
                setListView();
            }
        });
    }

    /**
     * 対象ファイル検索
     * @param path 検索する場所
     * @return
     */
    private List<Item> searchTargetFiles(final String path){
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(path);
            }
        });

        List<Item> listItems = new ArrayList<>();
        final File directory = new File(path);
        if (!directory.canRead()) {
            return new ArrayList<>();
        }

        // java.io.File クラスのメソッド list()
        // 指定したディレクトリに含まれるファイル、ディレクトリの一覧を String 型の配列で返す。
        String[] files = directory.list();
        for (String fileName : files){

            File subFile;
            subFile = new File(directory.getPath() + "/" + fileName);

            if(subFile.isDirectory()){
                //Log.d(TAG, "dir:" + subFile.getName());
                listItems.addAll(searchTargetFiles(subFile.getPath()));
                //listDirectory.add(directory.getPath() + "/" + fileName[n]);
            } else{
                // 非表示に設定されたファイル
                if (hideFiles.contains(subFile.getPath())) {
                    continue;
                }
                //Log.d(TAG, "file:" + subFile.getName());
                int pos = subFile.getName().lastIndexOf(".");
                if (pos != -1) {
                    if(!subFile.getName().substring(0, 1).equals(".")
                            && subFile.getName().substring(pos + 1).toLowerCase().equals("mp3")) {
                        Item item = new Item();
                        item.setName(subFile.getName());
                        item.setPath(subFile.getParent());
                        item.setSize(subFile.length());
                        item.setLastModified(new Date(subFile.lastModified()));
                        listItems.add(item);
                    }
                }
            }
        }

        return listItems;
    }

    public void setListView() {
        clearListView();

        if(preferences.getInt("background", 0) == 0) {
            listView.setBackgroundColor(Color.parseColor(getString(R.string.list_color_white_background)));
        } else{
            listView.setBackgroundColor(Color.parseColor(getString(R.string.list_color_black_background)));
        }

        int sort = preferences.getInt("sort", 0);
        if(sort == 1) {
            Collections.sort(listItems, new SizeComparator());
        } else if(sort == 2) {
            Collections.sort(listItems, new DateComparator());
        } else{
            Collections.sort(listItems, new DirectoryComparator());
        }

        for (int i = 0; i < listItems.size(); i++) {
            Log.d("wata", "name:" + listItems.get(i).getName());
        }

        adapter = new ListAdapter(MainActivity.this, R.layout.list_item, listItems);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);
    }

    public void clearListView() {
        adapter = new ListAdapter(MainActivity.this, R.layout.list_item, new ArrayList<Item>());
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
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
                final String itemList[] = getResources().getStringArray(R.array.sort_names);

                final int sort = preferences.getInt("sort", 0);
                final SharedPreferences.Editor editor = preferences.edit();
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_menu_sort_by_size)
                        .setTitle(getString(R.string.sort_title))
                        .setSingleChoiceItems(itemList, sort, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                editor.putInt("sort", whichButton);
                                editor.apply();
                            }
                        })
                        .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if(sort != preferences.getInt("sort", 0)) {
                                    setListView();
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if(sort != preferences.getInt("sort", 0)) {
                                    editor.putInt("sort", sort);
                                    editor.apply();
                                }
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
                    Toast.makeText(this, "What!?", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_MP3:
                // Mp3Activityから戻ってきた場合
                if (resultCode == RESULT_OK) {
                    int pos = data.getIntExtra("index", -1);
                    if(pos > -1) {
                        File file = new File(listItems.get(pos).getPath() + "/" + listItems.get(pos).getName());
                        listItems.get(pos).setSize(file.length());
                        listItems.get(pos).setLastModified(new Date(file.lastModified()));
                        adapter.notifyDataSetChanged();
                    }
                }
            default:
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        switch(v.getId()){
            case R.id.list_view:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                String title = listItems.get(info.position).getName();
                menu.setHeaderTitle(title);
                getMenuInflater().inflate(R.menu.list_menu, menu);
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        final Item item = listItems.get(info.position);

        switch(menuItem.getItemId()){
            case R.id.list_menu_hide:
                // ファイルを非表示にする
                String filePath = item.getPath() + "/" + item.getName();
                if (!hideFiles.contains(filePath)) {
                    hideFiles.add(filePath);
                }
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet("hideFiles", hideFiles);
                editor.apply();

                listItems.remove(info.position);
                setListView();
                return true;
            case R.id.list_menu_rename:
                // ファイル名を変更する
                LayoutInflater inflater = LayoutInflater.from(this);
                final View renameLayout = inflater.inflate(R.layout.rename_dialog, null);
                new AlertDialog.Builder(this)
                        .setTitle(listItems.get(info.position).getName())
                        .setMessage(getString(R.string.rename_dialog_text))
                        .setView(renameLayout)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                renameFile(
                                        item.getPath() + "/" + item.getName(),
                                        item.getPath() + "/" + ((EditText) renameLayout.findViewById(R.id.editRename))
                                                .getText().toString().trim() + ".mp3"
                                );
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //
                            }
                        })
                        .show();
                return true;
        }
        return false;
    }

    private void renameFile(String targetFile, String renameFile) {
        File file = new File(targetFile);
        File dest = new File(renameFile);
        try {
            copy(file, dest);
            file.delete();
        } catch (Exception e) {
            Toast.makeText(this, "Error to move new app: " + file + " > " + dest, Toast.LENGTH_LONG).show();
        }

/*
        if(canRename(file, dest)) {
            if(!file.renameTo(dest)) {
                Toast.makeText(this, "Error to move new app: " + file + " > " + dest, Toast.LENGTH_LONG).show();
            }
        } else {
            try {
                copy(file, dest);
                file.delete();
            } catch (Exception e) {
                Toast.makeText(this, "Error to move new app: " + file + " > " + dest, Toast.LENGTH_LONG).show();
            }
        }
*/
    }

    private void copy(final File f1, final File f2) throws IOException {
        f2.createNewFile();

        final RandomAccessFile file1 = new RandomAccessFile(f1, "r");
        final RandomAccessFile file2 = new RandomAccessFile(f2, "rw");

        file2.getChannel().write(file1.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, f1.length()));

        file1.close();
        file2.close();
    }

    private boolean canRename(final File f1, final File f2) {
        final String p1 = f1.getAbsolutePath().replaceAll("^(/mnt/|/)", "");
        final String p2 = f2.getAbsolutePath().replaceAll("^(/mnt/|/)", "");

        return p1.replaceAll("\\/\\w+", "").equals(p2.replaceAll("\\/\\w+", ""));
    }
}
