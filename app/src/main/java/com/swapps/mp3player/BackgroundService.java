package com.swapps.mp3player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
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
    private static final int NOTIFICATION_ID = 10;
    private int startId;

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
        this.startId = startId;

        names = new ArrayList<>();
        for (String path : paths) {
            names.add(new File(path).getName());
        }

        sendBroadCast(index);

        mediaPlayers = new ArrayList<>();
        setMediaPlayer(0);

        return startId;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(channel);
        return channelId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForeground(true);
        stopSelf(startId);

        for (MediaPlayer mediaPlayer : mediaPlayers) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

                mediaPlayer.release();
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

        mediaPlayers.get(i).setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        });

        mediaPlayers.get(i).setOnCompletionListener(mp -> {
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
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, BackgroundService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            Notification notification = new NotificationCompat.Builder(this,
                    createNotificationChannel(getPackageName(), "MP3-Player Background Service"))
                    .setOngoing(true)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(new File(paths.get(index)).getName() + getString(R.string.running_message))
                    .setSmallIcon(R.drawable.icon)
                    .setContentIntent(pendingIntent)
                    .setTicker("Title")
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }
}