package com.fqxd.gftools.noti.plugin;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessageReceiver extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String Package = remoteMessage.getData().get("Package");
        Log.d("package",Package);

        try{
            getPackageManager().getPackageInfo(Package, PackageManager.GET_ACTIVITIES);
            Intent intent = getPackageManager().getLaunchIntentForPackage(Package);
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Package));
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        if(!getSharedPreferences("Prefs",MODE_PRIVATE).getString("uid","").equals(""))
            FirebaseMessaging.getInstance().subscribeToTopic(getSharedPreferences("Prefs",MODE_PRIVATE).getString("uid","") + "_receiver");
    }
}
