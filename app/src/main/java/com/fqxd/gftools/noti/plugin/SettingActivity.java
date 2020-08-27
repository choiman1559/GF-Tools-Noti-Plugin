package com.fqxd.gftools.noti.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Set;

public class SettingActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        if(!BuildConfig.DEBUG && isPackageInstalled("com.fqxd.gftools", getPackageManager())) {
            Toast.makeText(this, "소전툴즈가 설치되어 있는 기기에서는 사용이 불가합니다!", Toast.LENGTH_SHORT).show();
            finish();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);
        mAuth = FirebaseAuth.getInstance();

        final SharedPreferences prefs = getSharedPreferences("Prefs", MODE_PRIVATE);

        final SwitchCompat onoff = findViewById(R.id.Enable);
        final Button glogin = findViewById(R.id.glogin);
        final TextView guid = findViewById(R.id.guid);

        if (!prefs.getString("uid", "").equals(""))
            guid.setText("Logined as " + mAuth.getCurrentUser().getEmail());
        else guid.setVisibility(View.GONE);

        onoff.setChecked(prefs.getBoolean("Enabled", false));
        onoff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(onoff.isChecked() && Build.VERSION.SDK_INT > 28 && !Settings.canDrawOverlays(SettingActivity.this)) {
                Toast.makeText(SettingActivity.this, "이 기능을 사용하기 위해 다른 앱 위에 그리기 권한이 필요합니다!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                onoff.setChecked(false);
            } else {
                Set<String> sets = NotificationManagerCompat.getEnabledListenerPackages(SettingActivity.this);
                if (sets.contains(getPackageName())) {
                    prefs.edit().putBoolean("Enabled", onoff.isChecked()).apply();
                    if (!getSharedPreferences("Prefs", MODE_PRIVATE).getString("uid", "").equals(""))
                        FirebaseMessaging.getInstance().subscribeToTopic(getSharedPreferences("Prefs", MODE_PRIVATE).getString("uid", ""));
                } else {
                    Toast.makeText(SettingActivity.this, "이 기능을 사용하기 위해 알람 엑세스 권한이 필요합니다!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    prefs.edit().putBoolean("Enabled", false).apply();
                    onoff.setChecked(false);
                }
            }
        });

        if (!prefs.getString("uid", "").equals("")) {
            glogin.setText(R.string.GLogout);
            onoff.setEnabled(true);
        }
        else  {
            glogin.setText(R.string.GLogin);
            onoff.setChecked(false);
            onoff.setEnabled(false);
            prefs.edit().putBoolean("Enabled", false).apply();
        }
        glogin.setOnClickListener(v -> {
            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            assert cm.getActiveNetworkInfo() != null;
            boolean isOnline = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();

            if (prefs.getString("uid", "").equals("") && isOnline) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            } else if(isOnline) signOut(onoff,prefs);
            else Toast.makeText(SettingActivity.this,"Check internet and try again!",Toast.LENGTH_LONG).show();
        });
    }

    public void signOut(SwitchCompat onoff, SharedPreferences prefs) {
        mAuth.signOut();
        mGoogleSignInClient.signOut();
        SettingActivity.this.recreate();
        prefs.edit().remove("uid").apply();

        onoff.setChecked(false);
        onoff.setEnabled(false);
        prefs.edit().putBoolean("Enabled", false).apply();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(SettingActivity.this, "인증 실패", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SettingActivity.this, "구글 로그인 인증 성공", Toast.LENGTH_SHORT).show();
                        getSharedPreferences("Prefs", MODE_PRIVATE).edit().putString("uid", mAuth.getUid()).apply();
                        SettingActivity.this.recreate();
                    }
                });
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
