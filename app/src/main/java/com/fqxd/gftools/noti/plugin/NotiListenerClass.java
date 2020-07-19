package com.fqxd.gftools.noti.plugin;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.provider.Settings.Global.DEVICE_NAME;

public class NotiListenerClass extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if (NotiListenerClass.this.getSharedPreferences("Prefs", MODE_PRIVATE).getBoolean("Enabled", false)) {
            if (isGF(sbn.getPackageName())) {

                Notification notification = sbn.getNotification();
                Bundle extra = notification.extras;

                addData(sbn, NotiListenerClass.this);
                String TOPIC = "/topics/" + getSharedPreferences("Prefs", MODE_PRIVATE).getString("uid", "");
                String TITLE = extra.getString(Notification.EXTRA_TITLE);
                String TEXT = extra.getCharSequence(Notification.EXTRA_TEXT).toString();
                String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
                String DEVICE_ID = getMACAddress();
                String Package = "" + sbn.getPackageName();

                JSONObject notificationHead = new JSONObject();
                JSONObject notifcationBody = new JSONObject();
                try {
                    notifcationBody.put("title", TITLE);
                    notifcationBody.put("message", TEXT);
                    notifcationBody.put("package", Package);
                    notifcationBody.put("type", "send");
                    notifcationBody.put("device_name", DEVICE_NAME);
                    notifcationBody.put("device_id",  DEVICE_ID);

                    notificationHead.put("to", TOPIC);
                    notificationHead.put("data", notifcationBody);
                } catch (JSONException e) {
                    Log.e("Noti", "onCreate: " + e.getMessage());
                }
                sendNotification(notificationHead);
            }
        }
    }

    static String getMACAddress() {
        String interfaceName = "wlan0";
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                byte[] mac = intf.getHardwareAddress();
                if (mac == null) return "";
                StringBuilder buf = new StringBuilder();
                for (byte b : mac) buf.append(String.format("%02X:", b));
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public boolean isGF(String Packagename) {
        if (Packagename.equals(getString(R.string.target_cn_bili))) return true;
        if (Packagename.equals(getString(R.string.target_cn_uc))) return true;
        if (Packagename.equals(getString(R.string.target_en))) return true;
        if (Packagename.equals(getString(R.string.target_jp))) return true;
        if (Packagename.equals(getString(R.string.target_kr))) return true;
        if (Packagename.equals(getString(R.string.target_tw))) return true;
        return (BuildConfig.DEBUG && Packagename.equals("xyz.notitest.noti"));
    }

    private void sendNotification(JSONObject notification) {
        final String FCM_API = "https://fcm.googleapis.com/fcm/send";
        final String serverKey = "key=" + getString(R.string.serverKey);
        final String contentType = "application/json";
        final String TAG = "NOTIFICATION TAG";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(FCM_API, notification,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(NotiListenerClass.this, "알람 전송 실패! 인터넷 환경을 확인해주세요!", Toast.LENGTH_LONG).show();
                        Log.i(TAG, "onErrorResponse: Didn't work");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", serverKey);
                params.put("Content-Type", contentType);
                return params;
            }
        };
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    @Deprecated
    private void addData(StatusBarNotification sbn, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();

        Notification notification = sbn.getNotification();
        Bundle extra = notification.extras;

        data.put("text", extra.getCharSequence(Notification.EXTRA_TEXT));
        data.put("subtext", extra.getCharSequence(Notification.EXTRA_SUB_TEXT));
        data.put("title", extra.getString(Notification.EXTRA_TITLE));
        data.put("package", sbn.getPackageName());
        data.put("id", sbn.getId());

        db.collection(context.getSharedPreferences("Prefs", Context.MODE_PRIVATE).getString("uid", "error")).document("data")
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Noti", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Noti", "Error writing document", e);
                    }
                });
    }
}
