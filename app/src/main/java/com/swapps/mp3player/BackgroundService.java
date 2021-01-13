package com.swapps.mp3player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class BackgroundService extends Service {
    List<MediaPlayer> mediaPlayers;
    List<String> paths;
    List<String> names;
    int index;
    private Handler handler;
    private BackgroundService context;

    @Override
    public void onCreate() {
        super.onCreate();
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

        sendBroadCast(index);

        mediaPlayers = new ArrayList<>();
        setMediaPlayer(0);

        return startId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (MediaPlayer mediaPlayer : mediaPlayers) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * MediaPlayer
     * @param i
     */
    private void setMediaPlayer(final int i) {
        try {
            mediaPlayers.add(new MediaPlayer());
            mediaPlayers.get(i).setDataSource(paths.get(index));
            mediaPlayers.get(i).prepare();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

        mediaPlayers.get(i).setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
        });

        mediaPlayers.get(i).setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (++index > (paths.size() - 1)) {
                    index = 0;
                }
                sendBroadCast(index);

                try {
                    mediaPlayers.get(i).release();
                    mediaPlayers.set(i, null);

                    mediaPlayers.add(new MediaPlayer());
                    setMediaPlayer(i + 1);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}