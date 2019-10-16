package com.airquality.VisionAir;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.RenderScript;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class Camera2Activity extends AppCompatActivity {

    private static final String TAG = "HdrViewfinderDemo";

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    // Durations in nanoseconds
    private static final long MICRO_SECOND = 1000;
    private static final long MILLI_SECOND = MICRO_SECOND * 1000;
    private static final long ONE_SECOND = MILLI_SECOND * 1000;
    CaptureRequest.Builder mHdrBuilder;
    ArrayList<CaptureRequest> mHdrRequests = new ArrayList<>(2);
    CaptureRequest mPreviewRequest;
    RenderScript mRS;
    //    ViewfinderProcessor mProcessor;
    CameraManager mCameraManager;
    CameraOps mCameraOps;
    /**
     * View for the camera preview.
     */
    private FixedAspectSurfaceView mPreviewView;
    /**
     * Root view of this activity.
     */
    private View rootView;
    /**
     * This shows the current mode of the app.
     */
    private TextView mModeText;
    // These show lengths of exposure for even frames, exposure for odd frames, and auto exposure.
    private TextView mEvenExposureText, mOddExposureText, mAutoExposureText;
    private Handler mUiHandler;
    private CameraCharacteristics mCameraInfo;

    //    private int mRenderMode = ViewfinderProcessor.MODE_NORMAL;
    private Surface mPreviewSurface;
    private Surface mProcessingHdrSurface;
    private Surface mProcessingNormalSurface;
    private long mOddExposure = ONE_SECOND / 33;
    private long mEvenExposure = ONE_SECOND / 33;

    private Object mOddExposureTag = new Object();
    private Object mEvenExposureTag = new Object();
    private Object mAutoExposureTag = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
    }


}
