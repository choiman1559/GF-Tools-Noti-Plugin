package com.fqxd.gftools.noti.plugin;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static com.fqxd.gftools.noti.plugin.NotiListenerClass.getMACAddress;

public class MessageReceiver extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = getMACAddress();
        Map<String, String> data = remoteMessage.getData();
        if(data.get("type").equals("reception") &&
                data.get("device_id").equals(DEVICE_ID) && data.get("device_name").equals(DEVICE_NAME)) {
            String Package = data.get("Package");
            Log.d("package", Package);

            new Handler(Looper.getMainLooper()).postDelayed(() -> Toast.makeText(this, "소전툴즈에서 원격 실행됨\n보낸 기기 : " + data.get("from_name"), Toast.LENGTH_SHORT).show(),0);
            try {
                getPackageManager().getPackageInfo(Package, PackageManager.GET_ACTIVITIES);
                Intent intent = getPackageManager().getLaunchIntentForPackage(Package);
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Package));
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        if(!getSharedPreferences("Prefs",MODE_PRIVATE).getString("uid","").equals(""))
            FirebaseMessaging.getInstance().subscribeToTopic(getSharedPreferences("Prefs",MODE_PRIVATE).getString("uid",""));
    }
}
