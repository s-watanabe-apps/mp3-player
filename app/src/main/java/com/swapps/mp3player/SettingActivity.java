package com.swapps.mp3player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingActivity extends AppCompatActivity {

    public static String PREFERENCES_NAME = "preferences";
    public static String SETTING_INCLUDE_MP4 = "includeMp4";
    SharedPreferences preferences;
    int includeMp4;
    int resultOk = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        preferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        includeMp4 = preferences.getInt(SETTING_INCLUDE_MP4, 0);

        // 検索対象
        final CheckBox checkIncludeMp4 = findViewById(R.id.checkIncludeMp4);
        checkIncludeMp4.setChecked(includeMp4 == 1);
        checkIncludeMp4.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            if (checkIncludeMp4.isChecked()) {
                includeMp4 = 1;
            } else {
                includeMp4 = 0;
            }
            editor.putInt(SETTING_INCLUDE_MP4, includeMp4);
            editor.apply();
            resultOk = 1;
        });

        // キャッシュ制御
        final TextView textClearCache = findViewById(R.id.textClearCache);
        final File file = new File(getFilesDir(), MainActivity.CACHE_FILE_NAME);
        if (file.exists()) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            textClearCache.setText(file.getName() + "\n" + format.format(new Date(file.lastModified())));
        }
        Button buttonClearCache = findViewById(R.id.buttonClearCache);
        buttonClearCache.setOnClickListener(v -> {
            try {
                file.delete();
                textClearCache.setText("---");
                resultOk = 1;
            } catch(Exception ignored) {
                //
            }
        });


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            int result = RESULT_CANCELED;
            if(resultOk == 1) {
                result = RESULT_OK;
            }
            Intent intent = new Intent();
            setResult(result, intent);
            finish();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
