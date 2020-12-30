package com.swapps.mp3player;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class SettingActivity extends AppCompatActivity {

    SharedPreferences preferences;
    int background;
    int storage;
    int hideClear = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        preferences = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        background = preferences.getInt("background", 0);
        storage = preferences.getInt("storage", 0);

        // 背景の設定
        RadioGroup radioGroupBackground = findViewById(R.id.radioGroupBackground);
        radioGroupBackground.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.radioBackgroundWhite) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("background", 0);
                    editor.apply();
                } else{
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("background", 1);
                    editor.apply();
                }
            }
        });

        final RadioButton radioButtonBackgroundWhite = findViewById(R.id.radioBackgroundWhite);
        final RadioButton radioButtonBackgroundBlack = findViewById(R.id.radioBackgroundBlack);
        if(background == 0) {
            radioButtonBackgroundWhite.setChecked(true);
        } else{
            radioButtonBackgroundBlack.setChecked(true);
        }


        ImageView imageBackgroundWhite = findViewById(R.id.imageBackgroundWhite);
        imageBackgroundWhite.setBackgroundResource(R.drawable.border);
        imageBackgroundWhite.setImageResource(R.drawable.background_white);
        imageBackgroundWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioButtonBackgroundWhite.setChecked(true);
            }
        });

        ImageView imageBackgroundBlack = findViewById(R.id.imageBackgroundBlack);
        imageBackgroundBlack.setBackgroundResource(R.drawable.border);
        imageBackgroundBlack.setImageResource(R.drawable.background_black);
        imageBackgroundBlack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioButtonBackgroundBlack.setChecked(true);
            }
        });

        // 非表示クリア
        findViewById(R.id.buttonHideClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<String> hideFiles = new HashSet<>();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet("hideFiles", hideFiles);
                editor.commit();
                hideClear = 1;
                Toast.makeText(SettingActivity.this, getString(R.string.setting_hide_clear_message), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            int result = RESULT_CANCELED;
            if(hideClear == 1) {
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
