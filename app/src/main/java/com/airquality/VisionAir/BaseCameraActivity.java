package com.airquality.VisionAir;

import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class BaseCameraActivity extends AppCompatActivity {

    public static final String KEY_IMAGE = "image";
    protected static final int MIN_WIDTH = 512;
    private static final String TAG = BaseCameraActivity.class.getSimpleName();

    public static Intent createCameraIntent(Context context) {
        boolean legacy = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
            legacy = true;
        } else {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            try {
                if (manager != null) {
                    for (String cameraId : manager.getCameraIdList()) {
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                        Integer deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                        if (deviceLevel != null && deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                            legacy = true;
                        }
                    }
                }
            } catch (CameraAccessException | NullPointerException e) {
                Log.w(TAG, e);
            }
        }
        if (legacy) {
            return new Intent(context, CameraActivity.class);
        } else {
            return new Intent(context, CameraActivity.class);
        }
    }

}
