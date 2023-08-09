package com.swapps.mp3player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class UpdateReceiver extends BroadcastReceiver {

    public static Handler handler;

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle bundle = intent.getExtras();
        assert bundle != null;
        int index = bundle.getInt("index");

        if(handler !=null){
            Message msg = new Message();

            Bundle data = new Bundle();
            data.putInt("index", index);
            msg.setData(data);
            handler.sendMessage(msg);
        }
    }

    /**
     * メイン画面の表示を更新
     */
    public void registerHandler(Handler locationUpdateHandler) {
        handler = locationUpdateHandler;
    }

}