package com.swapps.mp3player;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

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
