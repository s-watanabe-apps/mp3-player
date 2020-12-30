package com.swapps.mp3player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BackgroundService extends Service {
    MediaPlayer mediaPlayer;
    List<String> paths;
    List<String> names;
    int index;
    private Handler handler;
    private BackgroundService context;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
    }

    public void registerHandler(Handler UpdateHandler) {
        handler = UpdateHandler;
    }

    protected void sendBroadCast(int index) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.putExtra("index", index);
        broadcastIntent.setAction("UPDATE_ACTION");
        getBaseContext().sendBroadcast(broadcastIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        paths = intent.getStringArrayListExtra("paths");
        index = 0;

        names = new ArrayList<>();
        for (String path : paths) {
            names.add(new File(path).getName());
        }

        try {
            sendBroadCast(index);
            mediaPlayer.setDataSource(paths.get(index));
            mediaPlayer.prepare();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (++index > (paths.size() - 1)) {
                    index = 0;
                }
                sendBroadCast(index);

                try {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(paths.get(index));
                    mediaPlayer.prepare();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        return startId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}