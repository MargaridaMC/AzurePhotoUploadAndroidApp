package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DownloadNotificationSelectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

       // String filename = intent.getStringExtra("filename");

        Log.d("TAG", "got notification click");

        //This is used to close the notification tray
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);

        // Attach to email
        BlobListView.attachImageToEmail(context, null, "output");
    }



}

