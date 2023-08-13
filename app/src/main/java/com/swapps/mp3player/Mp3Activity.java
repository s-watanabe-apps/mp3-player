package com.swapps.mp3player;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SeekBar;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.swapps.mp3player.MediaUtils.getDuration;

import androidx.appcompat.app.AppCompatActivity;

public class Mp3Activity extends AppCompatActivity {
    private UpdateReceiver updateReceiver;
    private IntentFilter intentFilter;
    private AudioManager audioManager;
    private SharedPreferences preferences;
    private ListView listView;
    private SongListAdapter adapter;
    private List<SongItem> listItems;

    ProgressDialog progressDialog;
    Handler handler;
    Timer timer;

    private final Handler updateHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            final int index = (Integer) bundle.get("index");
            for (int i = 0; i < listItems.size(); i++) {
                if (i == index) {
                    listItems.get(i).setNow(0);
                    listItems.get(i).setPlay(1);
                } else {
                    listItems.get(i).setNow(0);
                    listItems.get(i).setPlay(0);
                }
            }

            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    handler.post(() -> {
                        listItems.get(index).setNow(listItems.get(index).getNow() + 1);
                        adapter.notifyDataSetChanged();
                    });
                }
            },1000,1000);

            adapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.mp3_play_list_prepare));
        progressDialog.setMessage("---");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        handler = new Handler();
        new Thread(() -> handler.post(() -> {
            init();
            progressDialog.dismiss();
        })).start();

    }

    /**
     * 画面初期化処理
     */
    private void init() {
        //Log.d("test", "Mp3.init start");

        preferences = getSharedPreferences(SettingActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);

        Intent intent = new Intent(getApplication(), BackgroundService.class);
        ArrayList<String> paths = getIntent().getStringArrayListExtra("paths");
        intent.putStringArrayListExtra("paths", paths);
        startForegroundService(intent);

        updateReceiver = new UpdateReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction("UPDATE_ACTION");
        registerReceiver(updateReceiver, intentFilter);
        updateReceiver.registerHandler(updateHandler);

        SeekBar seekBar = findViewById(R.id.seekBar);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekBar.setMax(max);
        seekBar.setProgress(volume);

        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.getProgress(), 0);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        //
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        //
                    }

                }
        );

        listView = findViewById(R.id.song_list_view);
        listItems = new ArrayList<>();
        assert paths != null;
        for (String path : paths) {
            SongItem item = new SongItem();
            String name = new File(path).getName();
            progressDialog.setMessage(name);
            item.setName(name);
            item.setDuration(getDuration(new File(path)));
            listItems.add(item);
        }
        adapter = new SongListAdapter(this, R.layout.song_list_item, listItems);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        //Log.d("test", "Mp3.init end");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(getApplication(), BackgroundService.class);
        stopService(intent);
        unregisterReceiver(updateReceiver);
    }
}
