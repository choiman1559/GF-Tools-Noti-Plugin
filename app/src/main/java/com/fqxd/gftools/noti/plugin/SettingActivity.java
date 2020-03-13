package com.fqxd.gftools.noti.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Set;

public class SettingActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;

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
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        mAuth = FirebaseAuth.getInstance();

        final SharedPreferences prefs = getSharedPreferences("Prefs", MODE_PRIVATE);

        final Switch onoff = findViewById(R.id.Enable);
        final Button glogin = findViewById(R.id.glogin);
        final TextView guid = findViewById(R.id.guid);

        if (!prefs.getString("uid", "").equals(""))
            guid.setText("User Id : " + prefs.getString("uid", ""));
        else guid.setText(R.string.UserId);

        onoff.setChecked(prefs.getBoolean("Enabled", false));
        onoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Set<String> sets = NotificationManagerCompat.getEnabledListenerPackages(SettingActivity.this);
                if (sets.contains(getPackageName())) {
                    prefs.edit().putBoolean("Enabled", onoff.isChecked()).apply();
                    if(!getSharedPreferences("Prefs",MODE_PRIVATE).getString("uid","").equals(""))
                        FirebaseMessaging.getInstance().subscribeToTopic(getSharedPreferences("Prefs",MODE_PRIVATE).getString("uid","") + "_receiver");
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
        glogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                assert cm.getActiveNetworkInfo() != null;
                Boolean isOnline = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();

                if (prefs.getString("uid", "").equals("") && isOnline) {
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                } else if(isOnline) signOut(onoff,prefs);
                else Toast.makeText(SettingActivity.this,"Check internet and try again!",Toast.LENGTH_LONG).show();
            }
        });
    }

    public void signOut(Switch onoff,SharedPreferences prefs) {
        mAuth.signOut();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
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
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(SettingActivity.this, "인증 실패", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingActivity.this, "구글 로그인 인증 성공", Toast.LENGTH_SHORT).show();
                            final TextView guid = findViewById(R.id.guid);
                            guid.setText("User Id : " + mAuth.getUid());
                            getSharedPreferences("Prefs", MODE_PRIVATE).edit().putString("uid", mAuth.getUid()).apply();
                            SettingActivity.this.recreate();
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
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
