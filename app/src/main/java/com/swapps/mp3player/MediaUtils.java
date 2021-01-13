package com.swapps.mp3player;

import android.media.MediaPlayer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;

public class MediaUtils {
    public static int getDuration(File audioFile) {
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            FileInputStream inputStream = new FileInputStream(audioFile);;
            FileDescriptor fileDescriptor = inputStream.getFD();
            mediaPlayer.setDataSource(fileDescriptor);
            mediaPlayer.prepare();
            int length = mediaPlayer.getDuration();
            mediaPlayer.release();
            return length / 1000;
        } catch (Exception e) {
            return 0;
        }
    }
}
