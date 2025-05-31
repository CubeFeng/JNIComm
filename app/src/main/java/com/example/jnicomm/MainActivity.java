package com.example.jnicomm;

import static pub.devrel.easypermissions.EasyPermissions.hasPermissions;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jnicomm.databinding.ActivityMainBinding;
import com.example.jnilibrary.HyperMateAdapter;
import com.example.jnilibrary.NativeApi;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks  {
    private static final String TAG = "MainActivity";

    private final static int REQUEST_PERMISSION = 0x1001;

    private List<String> mPermissionList = new ArrayList<>();

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initUI();
        checkPermission();
    }

    private void initUI() {
        binding.btnInit.setOnClickListener(v -> {
            HyperMateAdapter.getInstance().init();
            NativeApi.initNative();
        });

        binding.btnCallNative.setOnClickListener(v -> {
//            NativeApi.getAddress();
            HyperMateAdapter.getInstance().startScan();
        });

    }

    void checkPermission() {
        if (!hasPermissions()) {
            EasyPermissions.requestPermissions(
                    new PermissionRequest.Builder(
                            this,
                            REQUEST_PERMISSION,
                            mPermissionList.toArray(new String[0]))
                            .setRationale("需要蓝牙权限来扫描和连接设备")
                            .setPositiveButtonText("确定")
                            .setNegativeButtonText("取消")
                            .build()
            );
        }
    }

    public boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 版本大于等于 Android12 时
            // 只包括蓝牙这部分的权限，其余的需要什么权限自己添加
            mPermissionList.add(android.Manifest.permission.BLUETOOTH_SCAN);
            mPermissionList.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            // Android 版本小于 Android12 及以下版本
            mPermissionList.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        return EasyPermissions.hasPermissions(this, mPermissionList.toArray(new String[0]));
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        String result = String.join(", ", perms);
        Log.d(TAG, ">>> onPermissionsGranted：" + result);
        if (requestCode == REQUEST_PERMISSION) {
            if (perms.containsAll(mPermissionList)) {
                // 所有蓝牙权限都已授予，开始扫描
                HyperMateAdapter.getInstance().init();
                HyperMateAdapter.getInstance().startScan();
            }
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
        finish();
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        String result = String.join(", ", permissions);
//        Log.d(TAG, ">>> onRequestPermissionsResult: " + result);
//        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
//    }
}