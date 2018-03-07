package com.phoenix.silentcamera;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.phoenix.silentcamera.receiver.PatternLockReceiver;

public class MonitorControlActivity extends AppCompatActivity {
    private static final String TAG = "MonitorControlActivity";

    private static final String STATUS = "status";

    private static final int REQUEST_MONITOR = 1;
    private static final int REQUEST_PERMISSION = 2;

    private Context mContext;

    private ComponentName mAdminReceiver;
    private DevicePolicyManager mPolicyManager;
    private boolean mPermited = false;

    private Switch mSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_monitor_control);
        mSwitch = findViewById(R.id.switch1);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    checkPermission();
                    activeAdminManager();
                } else {
                    disableManager();
                }
            }
        });
        checkPermission();
        initPolicyManager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initiateSwitchStatus();
    }

    private boolean initiateSwitchStatus() {
        SharedPreferences preferences = this.getPreferences(MODE_PRIVATE);
        boolean isOn = preferences.getBoolean(STATUS, false);
        if(isOn) {
            mSwitch.setChecked(true);
        } else {
            mSwitch.setChecked(false);
        }
        return isOn;
    }

    private void setMonitorStatus(boolean isOn) {
        SharedPreferences preferences = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(STATUS, isOn);
        editor.commit();
    }

    private void initPolicyManager() {
        mPolicyManager = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminReceiver = new ComponentName(mContext, PatternLockReceiver.class);
    }

    private void activeAdminManager(){
        Log.d(TAG, "active Admin Manager");
        if(!mPermited) {
            disableManager();
            return;
        }
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminReceiver);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_warning_descript));

        startActivityForResult(intent, 1);
    }

    private void disableManager() {
        try {
            mPolicyManager.removeActiveAdmin(mAdminReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setMonitorStatus(false);
    }

    private void checkPermission() {
        boolean permitted = true;
        if(Build.VERSION.SDK_INT >= 23) {
            if (PackageManager.PERMISSION_GRANTED != PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permitted = false;
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
            if (PackageManager.PERMISSION_GRANTED != PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                permitted = false;
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
            }
        }
        mPermited = permitted;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_MONITOR) {
            if (resultCode == RESULT_OK) {
                mSwitch.setChecked(true);
                setMonitorStatus(true);
            } else {
                mSwitch.setChecked(false);
                setMonitorStatus(false);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION){
            boolean granted = true;
            for(int i : grantResults) {
                if(i != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                }
            }
            if(!granted) {
                mPermited = false;
                disableManager();
                Toast.makeText(this, "您需要开启权限才能使用", Toast.LENGTH_SHORT).show();
            } else {
                mPermited = true;
                if(!initiateSwitchStatus()) {
                    activeAdminManager();
                }
            }
        }
    }
}
