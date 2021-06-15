package com.swapps.mp3player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.swapps.mp3player.MediaUtils.getDuration;

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        init();
                        progressDialog.dismiss();
                    }
                });
            }
        }).start();

    }

    private void init() {
        Log.d("test", "Mp3.init start");

        preferences = getSharedPreferences(SettingActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        if(preferences.getInt(SettingActivity.SETTING_BACKGROUND, 0) == 0) {
            findViewById(R.id.layoutMp3).setBackgroundColor(Color.parseColor(getString(R.string.list_color_white_background)));
            ((TextView) findViewById(R.id.textVolume)).setTextColor(Color.parseColor(getString(R.string.list_color_black_background)));
            ((TextView) findViewById(R.id.textSongList)).setTextColor(Color.parseColor(getString(R.string.list_color_black_background)));
        } else{
            findViewById(R.id.layoutMp3).setBackgroundColor(Color.parseColor(getString(R.string.list_color_black_background)));
            ((TextView) findViewById(R.id.textVolume)).setTextColor(Color.parseColor(getString(R.string.list_color_white_background)));
            ((TextView) findViewById(R.id.textSongList)).setTextColor(Color.parseColor(getString(R.string.list_color_white_background)));
        }

        Intent intent = new Intent(getApplication(), BackgroundService.class);
        ArrayList<String> paths = getIntent().getStringArrayListExtra("paths");
        intent.putStringArrayListExtra("paths", paths);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

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

        Log.d("test", "Mp3.init end");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(getApplication(), BackgroundService.class);
        stopService(intent);
        unregisterReceiver(updateReceiver);
    }

    private Handler updateHandler = new Handler() {
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
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listItems.get(index).setNow(listItems.get(index).getNow() + 1);
                        adapter.notifyDataSetChanged();
                    }
                });
                }
            },1000,1000);

            adapter.notifyDataSetChanged();
        }
    };
}
