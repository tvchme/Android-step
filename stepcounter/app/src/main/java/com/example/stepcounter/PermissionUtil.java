package com.example.stepcounter;


import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {

    // 位置权限请求码
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // 检查位置权限
    public static boolean checkLocationPermissions(Activity activity) {
        String[] permissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        };

        return checkPermissions(activity, permissions);
    }

    // 检查权限组
    private static boolean checkPermissions(Activity activity, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // 请求位置权限
    public static void requestLocationPermissions(Activity activity) {
        String[] permissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(activity, permissions, LOCATION_PERMISSION_REQUEST_CODE);
    }

    // 处理权限请求结果
    public static boolean handlePermissionResult(int requestCode, String[] permissions,
                                                 int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}