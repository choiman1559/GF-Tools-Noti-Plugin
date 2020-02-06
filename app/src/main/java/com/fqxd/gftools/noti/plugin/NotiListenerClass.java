package com.fqxd.gftools.noti.plugin;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.crashlytics.android.Crashlytics.TAG;

public class NotiListenerClass extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if(NotiListenerClass.this.getSharedPreferences("Prefs",MODE_PRIVATE).getBoolean("Enabled",false)) {
            if(isGF(sbn.getPackageName())) {
                addData(sbn,NotiListenerClass.this);
            }
        }
    }

    public boolean isGF(String Packagename) {
        if(Packagename.equals(getString(R.string.target_cn_bili))) return true;
        else if(Packagename.equals(getString(R.string.target_cn_uc))) return true;
        else if(Packagename.equals(getString(R.string.target_en))) return true;
        else if(Packagename.equals(getString(R.string.target_jp))) return true;
        else if(Packagename.equals(getString(R.string.target_kr))) return true;
        else if(Packagename.equals(getString(R.string.target_tw))) return true;
        else return false;
    }

    private void addData(StatusBarNotification sbn, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();

        Notification notification = sbn.getNotification();
        Bundle extra = notification.extras;

        data.put("text", extra.getCharSequence(Notification.EXTRA_TEXT));
        data.put("subtext", extra.getCharSequence(Notification.EXTRA_SUB_TEXT));
        data.put("title", extra.getString(Notification.EXTRA_TITLE));
        data.put("package",sbn.getPackageName());
        data.put("id",sbn.getId());

        db.collection(context.getSharedPreferences("Prefs",Context.MODE_PRIVATE).getString("uid","error")).document("data")
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }
}
