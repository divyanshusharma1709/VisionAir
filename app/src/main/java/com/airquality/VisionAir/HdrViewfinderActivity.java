/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.airquality.VisionAir;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.renderscript.RenderScript;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.opencv.imgproc.Imgproc.INTER_AREA;

//import android.support.annotation.NonNull;
//import android.support.design.widget.Snackbar;
//import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;


/**
 * A small demo of advanced camera functionality with the Android camera2 API.
 *
 * <p>This demo implements a real-time high-dynamic-range camera viewfinder,
 * by alternating the sensor's exposure time between two exposure values on even and odd
 * frames, and then compositing together the latest two frames whenever a new frame is
 * captured.</p>
 *
 * <p>The demo has three modes: Regular auto-exposure viewfinder, split-screen manual exposure,
 * and the fused HDR viewfinder.  The latter two use manual exposure controlled by the user,
 * by swiping up/down on the right and left halves of the viewfinder.  The left half controls
 * the exposure time of even frames, and the right half controls the exposure time of odd frames.
 * </p>
 *
 * <p>In split-screen mode, the even frames are shown on the left and the odd frames on the right,
 * so the user can see two different exposures of the scene simultaneously.  In fused HDR mode,
 * the even/odd frames are merged together into a single image.  By selecting different exposure
 * values for the even/odd frames, the fused image has a higher dynamic range than the regular
 * viewfinder.</p>
 *
 * <p>The HDR fusion and the split-screen viewfinder processing is done with RenderScript; as is the
 * necessary YUV->RGB conversion. The camera subsystem outputs YUV images naturally, while the GPU
 * and display subsystems generally only accept RGB data.  Therefore, after the images are
 * fused/composited, a standard YUV->RGB color transform is applied before the the data is written
 * to the output Allocation. The HDR fusion algorithm is very simple, and tends to result in
 * lower-contrast scenes, but has very few artifacts and can run very fast.</p>
 *
 * <p>Data is passed between the subsystems (camera, RenderScript, and display) using the
 * Android {@link Surface} class, which allows for zero-copy transport of large
 * buffers between processes and subsystems.</p>
 */
public class HdrViewfinderActivity extends AppCompatActivity implements
        SurfaceHolder.Callback, CameraOps.ErrorDisplayer, CameraOps.CameraReadyListener, LocationListener {

    ///////////////////////////////////////////SHIVANI's

    private static final int REQUEST_CODE = 100;
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static final String TAG = "HdrViewfinderDemo";
    private static final String Tagg = "HdrViewfinderDemo";
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    // Durations in nanoseconds
    private static final long MICRO_SECOND = 1000;
    private static final long MILLI_SECOND = MICRO_SECOND * 1000;
    private static final long ONE_SECOND = MILLI_SECOND * 1000;
    public static Bitmap bitmap;
    private static String STORE_DIRECTORY;
    private static int IMAGES_PRODUCED;
    private static MediaProjection sMediaProjection;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    int flag;
    float distance = 0;
    int tem_flag = 0;
    double latitude;
    float humi, pressure, temp, speed, deg;
    double longitude;
    String nearest = " ";
    Mat img, imgcopy;
    ArrayList<cpcbCenterList> arrayList = new ArrayList<>();
    int feature_check = 0;
    float[][] features = new float[1][10];
    float[] labels = new float[1];
    double cpcbLabel;
    DatabaseReference ref;
    String keyDate;
    String stationName;


    ///////////////////////////////////////////
    String pollutant_id;
    String maxPollutant;
    String minPollutant;
    LocationManager locationManager;
    CaptureRequest.Builder mHdrBuilder;
    ArrayList<CaptureRequest> mHdrRequests = new ArrayList<>(2);
    CaptureRequest mPreviewRequest;
    RenderScript mRS;
    ViewfinderProcessor mProcessor;
    CameraManager mCameraManager;
    CameraOps mCameraOps;
    //    Mat img;
//    Bitmap bitmap;
    Bitmap resized = null;
    //    public static final String TAG = "CameraActivity";
//    public static final String Tagg = "new";
    private Camera mCamera;
    private CameraPreview mPreview;
    private Button capture, nextButton;
    private Context myContext;

    //////////////////////////////////////////
    private LinearLayout cameraPreview;
    private boolean cameraFront = false;
    private ProgressDialog pDialog;
    private NumberFormat formatter;
    private String noDecimalLat, noDecimalLong;
    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private OrientationChangeCallback mOrientationChangeCallback;
    /**
     * View for the camera preview.
     */
    private SurfaceView mPreviewView;
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
    private Surface mPreviewSurface;
    private Surface mProcessingHdrSurface;
    private Surface mProcessingNormalSurface;
    private int mRenderMode = ViewfinderProcessor.MODE_NORMAL;
    private long mOddExposure = ONE_SECOND / (4 * 33);
    private long mEvenExposure = ONE_SECOND * 4 / 33;
    private Object mOddExposureTag = new Object();
    private Object mEvenExposureTag = new Object();
    private Object mAutoExposureTag = new Object();
    /**
     * Show help dialogs.
     */
    private View.OnClickListener mHelpButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            MessageDialogFragment.newInstance(R.string.help_text)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        }
    };
    /**
     * Listener for completed captures
     * Invoked on UI thread
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {

            // Only update UI every so many frames
            // Use an odd number here to ensure both even and odd exposures get an occasional update
            long frameNumber = result.getFrameNumber();
            if (frameNumber % 3 != 0) return;

            final Long exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
            if (exposureTime == null) {
                throw new RuntimeException("Cannot get exposure time.");
            }

            // Format exposure time nicely
            String exposureText;
            if (exposureTime > ONE_SECOND) {
                exposureText = String.format(Locale.US, "%.2f s", exposureTime / 1e9);
            } else if (exposureTime > MILLI_SECOND) {
                exposureText = String.format(Locale.US, "%.2f ms", exposureTime / 1e6);
            } else if (exposureTime > MICRO_SECOND) {
                exposureText = String.format(Locale.US, "%.2f us", exposureTime / 1e3);
            } else {
                exposureText = String.format(Locale.US, "%d ns", exposureTime);
            }

            Object tag = request.getTag();
//            Log.i(TAG, "Exposure: " + exposureText);

            if (tag == mEvenExposureTag) {
                mEvenExposureText.setText(exposureText);

                mEvenExposureText.setEnabled(true);
                mOddExposureText.setEnabled(true);
                mAutoExposureText.setEnabled(false);
            } else if (tag == mOddExposureTag) {
                mOddExposureText.setText(exposureText);

                mEvenExposureText.setEnabled(true);
                mOddExposureText.setEnabled(true);
                mAutoExposureText.setEnabled(false);
            } else {
                mAutoExposureText.setText(exposureText);

                mEvenExposureText.setEnabled(false);
                mOddExposureText.setEnabled(false);
                mAutoExposureText.setEnabled(true);
            }
        }
    };
    private GestureDetector.OnGestureListener mViewListener
            = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
//            switchRenderMode(1);
            Date now = new Date();
            android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
            float posX = mPreviewView.getX();
            float posY = mPreviewView.getY();
            float posZ = mPreviewView.getZ();
            //Toast.makeText(HdrViewfinderActivity.this, "start", Toast.LENGTH_SHORT).show();
            startProjection();


            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mRenderMode == ViewfinderProcessor.MODE_NORMAL) return false;

            float xPosition = e1.getAxisValue(MotionEvent.AXIS_X);
            float width = mPreviewView.getWidth();
            float height = mPreviewView.getHeight();

            float xPosNorm = xPosition / width;
            float yDistNorm = distanceY / height;

            final float ACCELERATION_FACTOR = 8;
            double scaleFactor = Math.pow(2.f, yDistNorm * ACCELERATION_FACTOR);

            // Even on left, odd on right
            if (xPosNorm > 0.5) {
                mOddExposure *= scaleFactor;
            } else {
                mEvenExposure *= scaleFactor;
            }

            setHdrBurst();

            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < 9; i++) {    //change to 10?
            features[0][i] = -9999;
        }

        /////////////////////////////////////////
        OpenCVLoader.initDebug();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);

        }
        if (!MainActivity.nearest.equals("")) {
            distance = MainActivity.distance;
            latitude = MainActivity.latitude;
            longitude = MainActivity.longitude;
            formatter = NumberFormat.getNumberInstance();
            formatter.setMinimumFractionDigits(1);
            formatter.setMaximumFractionDigits(1);

            noDecimalLat = formatter.format(latitude);
            noDecimalLong = formatter.format(longitude);
            nearest = MainActivity.nearest;
            Log.i("NEAREST FOUND ", MainActivity.nearest);
            new GetHumidity().execute();
            start_fetch_prev();
        } else {
            getLocation();
        }
        arrayList.add(new cpcbCenterList("Alipur, Delhi - DPCC", 28.815329, 77.15301));
        arrayList.add(new cpcbCenterList("Anand Vihar, Delhi - DPCC", 28.646886, 77.316078));
        arrayList.add(new cpcbCenterList("Aya Nagar, New Delhi - IMD", 28.470692, 77.10994));
        arrayList.add(new cpcbCenterList("Bawana, Delhi - DPCC", 28.7762, 77.05107));
        arrayList.add(new cpcbCenterList("Burari Crossing, New Delhi - IMD", 28.72565, 77.20116));
        arrayList.add(new cpcbCenterList("CRRI Mathura Road, New Delhi - IMD", 28.5512, 77.273575));
        arrayList.add(new cpcbCenterList("Delhi Technological University, Delhi - CPCB", 28.749841, 77.115386));
        arrayList.add(new cpcbCenterList("IGI Airport Terminal - 3, New Delhi - IMD", 28.562777, 77.118004));
        arrayList.add(new cpcbCenterList("IHBAS, Dilshad Garden, Delhi - CPCB", 28.681173, 77.30252));
        arrayList.add(new cpcbCenterList("Mandir Marg", 28.636465, 77.201060));
        arrayList.add(new cpcbCenterList("Mundka, Delhi - DPCC", 28.684677, 77.07658));
        arrayList.add(new cpcbCenterList("NSIT Dwarka, New Delhi - CPCB", 28.60909, 77.03254));
        arrayList.add(new cpcbCenterList("Pusa, Delhi - DPCC", 28.639645, 77.14626));
        arrayList.add(new cpcbCenterList("RK Puram ", 28.563304, 77.186929));
        arrayList.add(new cpcbCenterList("Rohini, Delhi - DPCC", 28.732542, 77.120027));
        arrayList.add(new cpcbCenterList("Shadipur, New Delhi - CPCB", 28.651478, 77.14731));
        arrayList.add(new cpcbCenterList("Sirifort, Delhi - CPCB", 28.550425, 77.215935));
        arrayList.add(new cpcbCenterList("Sri Aurobindo Marg, Delhi - DPCC", 28.531345, 77.190155));
        arrayList.add(new cpcbCenterList("Ashok vihar Delhi-dpcc", 28.695377, 77.181666));
        arrayList.add(new cpcbCenterList("Dr. Karni Singh Shooting Range, Delhi - DPCC", 28.570989, 77.071888));
        arrayList.add(new cpcbCenterList("Dwarka-Sector 8, Delhi - DPCC", 28.571052, 77.071914));
        arrayList.add(new cpcbCenterList("Jahangirpuri, Delhi - DPCC", 28.732861, 77.170620));
        arrayList.add(new cpcbCenterList("Jawaharlal Nehru Stadium, Delhi - DPCC", 28.580254, 77.233851));
        arrayList.add(new cpcbCenterList("Lodhi Road, Delhi - IMD", 28.591849, 77.227238));
        arrayList.add(new cpcbCenterList("Major Dhyan Chand National Stadium, Delhi - DPCC", 28.570180, 76.933780));
        arrayList.add(new cpcbCenterList("Najafgarh, Delhi - DPCC", 28.822993, 77.101863));
        arrayList.add(new cpcbCenterList("Narela, Delhi - DPCC", 28.822886, 77.102007));
        arrayList.add(new cpcbCenterList("Nehru Nagar, Delhi - DPCC", 28.567887, 77.250527));
        arrayList.add(new cpcbCenterList("Okhla Phase-2, Delhi - DPCC", 28.530812, 77.271261));
        arrayList.add(new cpcbCenterList("Patparganj, Delhi - DPCC", 28.623748, 77.287184));
        arrayList.add(new cpcbCenterList("Sonia Vihar, Delhi - DPCC", 28.710435, 77.249447));
        arrayList.add(new cpcbCenterList("Vivek Vihar, Delhi - DPCC", 28.672299, 77.315278));
        arrayList.add(new cpcbCenterList("Wazirpur, Delhi - DPCC", 28.699782, 77.165441));

        flag = 0;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;


        ////////////////////////////////////////////////////////////////////////////////////


        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        LinearLayout linearLayout = findViewById(R.id.control_bar_contents);
        linearLayout.setVisibility(View.INVISIBLE);
        //Toast.makeText(this, mOddExposure + "\t" + mEvenExposure, Toast.LENGTH_SHORT).show();
        rootView = findViewById(R.id.panels);

        mPreviewView = findViewById(R.id.preview);
        mPreviewView.getHolder().addCallback(this);
//        mPreviewView.setGestureListener(this, mViewListener);

        Button helpButton = findViewById(R.id.help_button);
        helpButton.setOnClickListener(mHelpButtonListener);


        mModeText = findViewById(R.id.mode_label);
        mEvenExposureText = findViewById(R.id.even_exposure);
        mOddExposureText = findViewById(R.id.odd_exposure);
        mAutoExposureText = findViewById(R.id.auto_exposure);

        mUiHandler = new Handler(Looper.getMainLooper());

        mRS = RenderScript.create(this);
        mPreviewView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                //Toast.makeText(HdrViewfinderActivity.this, "start", Toast.LENGTH_SHORT).show();
                startProjection();
            }
        });

        // When permissions are revoked the app is restarted so onCreate is sufficient to check for
        // permissions core to the Activity's functionality.
        if (!checkCameraPermissions()) {
            requestCameraPermissions();
        } else {
            findAndOpenCamera();
        }
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

            if (sMediaProjection != null) {
                File externalFilesDir = getExternalFilesDir(null);
                if (externalFilesDir != null) {
                    STORE_DIRECTORY = Environment.getExternalStorageDirectory().toString();
                    File storeDirectory = new File(STORE_DIRECTORY);
                    if (!storeDirectory.exists()) {
                        boolean success = storeDirectory.mkdirs();
                        if (!success) {
                            Log.e(TAG, "failed to create file storage directory.");
                            return;
                        } else {
                            Log.e(TAG, "create file storage directory.");

                        }
                    } else {
                        Log.e(TAG, "already create file storage directory.");

                    }
                } else {
                    Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.");
                    return;
                }

                // display metrics
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                mDisplay = getWindowManager().getDefaultDisplay();

                // create virtual display depending on device width / height
                createVirtualDisplay();

                // register orientation change callback
                mOrientationChangeCallback = new OrientationChangeCallback(this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    mOrientationChangeCallback.enable();
                }

                // register media projection stop callback
                sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
            }
        }
    }

    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    private void stopProjection() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sMediaProjection != null) {
                    sMediaProjection.stop();
                }
            }
        });
    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.info: {
//                MessageDialogFragment.newInstance(R.string.intro_message)
//                        .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
//                break;
//            }
//        }
//        return super.onOptionsItemSelected(item);
//    }

    /****************************************** Factoring Virtual Display creation ****************/
    private void createVirtualDisplay() {
        // get width and height
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;
//        //Toast.makeText(this, mWidth + " " + mHeight , Toast.LENGTH_SHORT).show();
        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
        Log.i(TAG, "VD");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Wait until camera is closed to ensure the next application can open it
        if (mCameraOps != null) {
            mCameraOps.closeCameraAndWait();
            mCameraOps = null;
        }
    }

    /**
     * Return the current state of the camera permissions.
     */
    private boolean checkCameraPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        // Check if the Camera permission is already available.
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            // Camera permission has not been granted.
            Log.i(TAG, "CAMERA permission has NOT been granted.");
            return false;
        } else {
            // Camera permissions are available.
            Log.i(TAG, "CAMERA permission has already been granted.");
            return true;
        }
    }

    /**
     * Attempt to initialize the camera.
     */
    private void initializeCamera() {
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (mCameraManager != null) {
            mCameraOps = new CameraOps(mCameraManager,
                    /*errorDisplayer*/ this,
                    /*readyListener*/ this,
                    /*readyHandler*/ mUiHandler);

            mHdrRequests.add(null);
            mHdrRequests.add(null);
        } else {
            Log.e(TAG, "Couldn't initialize the camera");
        }
    }

    private void requestCameraPermissions() {
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Log.i(TAG, "Displaying camera permission rationale to provide additional context.");
            Snackbar.make(rootView, R.string.camera_permission_rationale, Snackbar
                    .LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request Camera permission
                            ActivityCompat.requestPermissions(HdrViewfinderActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting camera permission");
            // Request Camera permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(HdrViewfinderActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                findAndOpenCamera();
            } else {
                // Permission denied.

                // In this Activity we've chosen to notify the user that they
                // have rejected a core permission for the app since it makes the Activity useless.
                // We're communicating this message in a Snackbar since this is a sample app, but
                // core permissions would typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                Snackbar.make(rootView, R.string.camera_permission_denied_explanation, Snackbar
                        .LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    private void findAndOpenCamera() {
        boolean cameraPermissions = checkCameraPermissions();
        if (!cameraPermissions) {
            return;
        }
        String errorMessage = "Unknown error";
        boolean foundCamera = false;
        initializeCamera();
        if (mCameraOps != null) {
            try {
                // Find first back-facing camera that has necessary capability.
                String[] cameraIds = mCameraManager.getCameraIdList();
                for (String id : cameraIds) {
                    CameraCharacteristics info = mCameraManager.getCameraCharacteristics(id);
                    Integer facing = info.get(CameraCharacteristics.LENS_FACING);
                    Integer level = info.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    boolean hasFullLevel = Objects.equals(level,
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);

                    int[] capabilities = info
                            .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                    Integer syncLatency = info.get(CameraCharacteristics.SYNC_MAX_LATENCY);
                    boolean hasManualControl = hasCapability(capabilities,
                            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR);
                    boolean hasEnoughCapability = hasManualControl && Objects.equals(syncLatency,
                            CameraCharacteristics.SYNC_MAX_LATENCY_PER_FRAME_CONTROL);

                    // All these are guaranteed by
                    // CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, but checking
                    // for only the things we care about expands range of devices we can run on.
                    // We want:
                    //  - Back-facing camera
                    //  - Manual sensor control
                    //  - Per-frame synchronization (so that exposure can be changed every frame)
                    if (Objects.equals(facing, CameraCharacteristics.LENS_FACING_BACK) &&
                            (hasFullLevel || hasEnoughCapability)) {
                        // Found suitable camera - get info, open, and set up outputs
                        mCameraInfo = info;
                        mCameraOps.openCamera(id);
                        configureSurfaces();
                        foundCamera = true;
                        break;
                    }
                }
                if (!foundCamera) {
                    errorMessage = getString(R.string.camera_no_good);
                }
            } catch (CameraAccessException e) {
                errorMessage = getErrorString(e);
            }
            if (!foundCamera) {
                showErrorDialog(errorMessage);
            }
        }
    }

    private boolean hasCapability(int[] capabilities, int capability) {
        for (int c : capabilities) {
            if (c == capability) return true;
        }
        return false;
    }

    private void switchRenderMode(int direction) {
        if (mCameraOps != null) {
            mRenderMode = 2;

            mModeText.setText(getResources().getStringArray(R.array.mode_label_array)[mRenderMode]);

            if (mProcessor != null) {
                mProcessor.setRenderMode(mRenderMode);
            }
            if (mRenderMode == ViewfinderProcessor.MODE_NORMAL) {
//                mCameraOps.setRepeatingRequest(mPreviewRequest,
//                        mCaptureCallback, mUiHandler);
                setHdrBurst();
            } else {
                setHdrBurst();
            }
        }
    }

    /**
     * Configure the surfaceview and RS processing.
     */
    private void configureSurfaces() {
        // Find a good size for output - largest 16:9 aspect ratio that's less than 720p
        final int MAX_WIDTH = 1280;
        final float TARGET_ASPECT = 16.f / 9.f;
        final float ASPECT_TOLERANCE = 0.1f;

        StreamConfigurationMap configs =
                mCameraInfo.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (configs == null) {
            throw new RuntimeException("Cannot get available picture/preview sizes.");
        }
        Size[] outputSizes = configs.getOutputSizes(SurfaceHolder.class);

        Size outputSize = outputSizes[0];

        float outputAspect = (float) outputSize.getWidth() / outputSize.getHeight();
        for (Size candidateSize : outputSizes) {
            if (candidateSize.getWidth() > MAX_WIDTH) continue;
            float candidateAspect = (float) candidateSize.getWidth() / candidateSize.getHeight();
            boolean goodCandidateAspect =
                    Math.abs(candidateAspect - TARGET_ASPECT) < ASPECT_TOLERANCE;
            boolean goodOutputAspect =
                    Math.abs(outputAspect - TARGET_ASPECT) < ASPECT_TOLERANCE;
            if ((goodCandidateAspect && !goodOutputAspect) ||
                    candidateSize.getWidth() > outputSize.getWidth()) {
                outputSize = candidateSize;
                outputAspect = candidateAspect;
            }
        }
        Log.i(TAG, "Resolution chosen: " + outputSize);

        // Configure processing
        mProcessor = new ViewfinderProcessor(mRS, outputSize);
        setupProcessor();

        // Configure the output view - this will fire surfaceChanged
//        mPreviewView.setAspectRatio(outputAspect);
        mPreviewView.getHolder().setFixedSize(outputSize.getWidth(), outputSize.getHeight());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    /**
     * Once camera is open and output surfaces are ready, configure the RS processing
     * and the camera device inputs/outputs.
     */
    private void setupProcessor() {
        if (mProcessor == null || mPreviewSurface == null) return;

        mProcessor.setOutputSurface(mPreviewSurface);
        mProcessingHdrSurface = mProcessor.getInputHdrSurface();
        mProcessingNormalSurface = mProcessor.getInputNormalSurface();

        List<Surface> cameraOutputSurfaces = new ArrayList<>();
        cameraOutputSurfaces.add(mProcessingHdrSurface);
        cameraOutputSurfaces.add(mProcessingNormalSurface);

        mCameraOps.setSurfaces(cameraOutputSurfaces);
    }

    /**
     * Start running an HDR burst on a configured camera session
     */
    public void setHdrBurst() {

        mHdrBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 1600);
        mHdrBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, ONE_SECOND / 30);

        mHdrBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mEvenExposure);
        mHdrBuilder.setTag(mEvenExposureTag);
        mHdrRequests.set(0, mHdrBuilder.build());

        mHdrBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mOddExposure);
        mHdrBuilder.setTag(mOddExposureTag);
        mHdrRequests.set(1, mHdrBuilder.build());

        mCameraOps.setRepeatingBurst(mHdrRequests, mCaptureCallback, mUiHandler);
    }

    /**
     * Callbacks for the FixedAspectSurfaceView
     */

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mPreviewSurface = holder.getSurface();

        setupProcessor();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // ignored
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mPreviewSurface = null;
    }

    /**
     * Callbacks for CameraOps
     */
    @Override
    public void onCameraReady() {
        // Ready to send requests in, so set them up
        try {
            CaptureRequest.Builder previewBuilder =
                    mCameraOps.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(mProcessingNormalSurface);
            previewBuilder.setTag(mAutoExposureTag);
            mPreviewRequest = previewBuilder.build();

            mHdrBuilder =
                    mCameraOps.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mHdrBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF);
            mHdrBuilder.addTarget(mProcessingHdrSurface);

            switchRenderMode(0);

        } catch (CameraAccessException e) {
            String errorMessage = getErrorString(e);
            showErrorDialog(errorMessage);
        }
    }

    /**
     * Utility methods
     */
    @Override
    public void showErrorDialog(String errorMessage) {
        MessageDialogFragment.newInstance(errorMessage)
                .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
    }

    @SuppressLint({"SwitchIntDef", "StringFormatMatches"})
    @Override
    public String getErrorString(CameraAccessException e) {
        String errorMessage;
        switch (e.getReason()) {
            case CameraAccessException.CAMERA_DISABLED:
                errorMessage = getString(R.string.camera_disabled);
                break;
            case CameraAccessException.CAMERA_DISCONNECTED:
                errorMessage = getString(R.string.camera_disconnected);
                break;
            case CameraAccessException.CAMERA_ERROR:
                errorMessage = getString(R.string.camera_error);
                break;
            default:
                errorMessage = getString(R.string.camera_unknown, e.getReason());
                break;
        }
        return errorMessage;
    }

    private Mat readImageFromResources() {
        img = new Mat();

        Utils.bitmapToMat(bitmap, img);
        imgcopy = img;
        return img;
    }

    private float omega() {

        //Resize Image
        org.opencv.core.Size scaleSize = new org.opencv.core.Size(256, 256);
        Imgproc.resize(img, img, scaleSize, 0, 0, INTER_AREA);

        Mat B = Mat.zeros(256, 256, CvType.CV_8U);
        Mat D = Mat.zeros(256, 256, CvType.CV_8U);
        Mat C = Mat.zeros(256, 256, CvType.CV_8U);

//        Log.i(TAG,"B Values: "+B.dump()+" "+B.rows()+ " "+B.cols());

        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2BGR);
        List<Mat> BGR = new ArrayList<Mat>(3);
        Core.split(img, BGR);
        Mat mB = BGR.get(0);
        Mat mG = BGR.get(1);
        Mat mR = BGR.get(2);

//        Log.i(TAG,mB.dump());

        double[][] min = new double[256][256];
        double[][] max = new double[256][256];
        double[][] c = new double[256][256];
        double sum = 0, summ = 0;
        double d = 0, b = 0, cc = 0;
        for (int i = 0; i < B.rows(); i++) {
            for (int j = 0; j < B.cols(); j++) {

                min[i][j] = Math.min(Math.min(mB.get(i, j)[0], mG.get(i, j)[0]), mR.get(i, j)[0]);
                max[i][j] = Math.max(Math.max(mB.get(i, j)[0], mG.get(i, j)[0]), mR.get(i, j)[0]);
                c[i][j] = min[i][j] - max[i][j];

                sum = sum + min[i][j];
                summ = summ + max[i][j];


            }
        }
        Log.i(TAG, "Maximum element: " + max[0][0]);

        d = sum / 65536;
        Log.i(TAG, "d: " + d);
        b = summ / 65536;
        Log.i(TAG, "b: " + b);
        cc = b - d;
        Log.i(TAG, "c: " + cc);


        double A = 0;

        double maxx = max[0][0];
        for (int i = 0; i < max.length; i++) {
            for (int j = 0; j < max[i].length; j++)
                if (max[i][j] > maxx) {
                    maxx = max[i][j];
                }
        }

        Log.i(TAG, "Maximum Element: " + maxx);

        A = (0.33) * (maxx) + (0.66) * b;
        Log.i(TAG, "Airlight: " + A);

        double x1 = 0;
        x1 = (A - d) / (A);
        Log.i(TAG, "X!: " + x1);
        double x2 = 0;
        x2 = cc / A;
        Log.i(TAG, "X2: " + x2);


//x1=((A - d)/float(A))
//  x2=(c/float(A))
        double u = 10.127489;
        double v = -8.336512;
        double s = 0.13606234;
//
        double w;
        w = Math.exp(-0.5 * (u * x1 + v * x2) + s);
        Log.i(TAG, "Omegaaa: " + w);

        int a = 0;
        if (w <= 0.1)
            a = 0;
        else if (w <= 0.3)
            a = 1;
        else if (w <= 0.5)
            a = 2;
        else if (w <= 0.7)
            a = 3;
        else if (w <= 0.8)
            a = 4;
        else if (w <= 0.9)
            a = 5;

        Log.i(TAG, "Omegaaa Factor: " + a);
        return a;
    }

    private float contrast() {

        //Resize Image
        org.opencv.core.Size scaleSize = new org.opencv.core.Size(256, 256);
        Imgproc.resize(img, img, scaleSize, 0, 0, INTER_AREA);

        //convert bgr to gray
        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);

        float s = 0;
        float ss = 0;

        Mat con = img.clone();
        con.convertTo(con, CvType.CV_64FC3);
        double[] a = new double[(int) (img.total() * img.channels())];
        con.get(0, 0, a);
        for (int i = 0; i < a.length; i++) {
            s = (int) (s + a[i]);
        }
        con.put(0, 0, s);

        float avg;
        avg = (s / (256 * 256));

        Mat co = img.clone();
        co.convertTo(co, CvType.CV_64FC3);
        double[] b = new double[(int) (img.total() * img.channels())];
        co.get(0, 0, b);
        for (int i = 0; i < b.length; i++) {
            ss = ss + (float) ((b[i] - avg) * (b[i] - avg));
        }
        co.put(0, 0, ss);

        float contra;
        contra = (float) Math.sqrt(ss / (256 * 256));

        Log.i(TAG, "Contrast: " + contra);
        //Toast.makeText(getApplicationContext(), "Contrast: " + contra, Toast.LENGTH_SHORT).show();
        return contra;
    }


    //Shivani's Code Here Now

    public float entropy() {

        //Resize Image
        org.opencv.core.Size scaleSize = new org.opencv.core.Size(256, 256);
        Imgproc.resize(img, img, scaleSize, 0, 0, INTER_AREA);

        MatOfInt histSize = new MatOfInt(256);

        Mat hist = new Mat(img.size(), img.type());


        ArrayList<Mat> list = new ArrayList<Mat>();
        list.add(img);


        Imgproc.calcHist(list, new MatOfInt(0), new Mat(), hist, histSize, new MatOfFloat(0, 256));
        Core.normalize(hist, hist);

        double ent = 0;
        for (int row = 0; row < hist.rows(); row++) {
            double[] val = hist.get(row, 0);
            for (int p = 0; p < val.length; p++) {
                try {
                    if (val[p] != 0.0) {
                        ent += val[p] * (Math.log(val[p]) / Math.log(2));
                    }
                } catch (Exception e) {
                    //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

        }
        ent = -1 * ent;
        //Toast.makeText(getApplicationContext(), "Entropy: " + ent, Toast.LENGTH_SHORT).show();

        Log.i(TAG, "Entropy: " + ent);
        return (float) ent;


    }

    /*
     *   Features extraction:
     *   omega
     *   contrast
     *   entropy
     *   weather api call - temperature, pressure, humidity, wind degree, wind speed
     */

//    private float omega(){
//
//        //Resize Image
//        org.opencv.core.Size scaleSize = new org.opencv.core.Size(256,256);
//        Imgproc.resize(img,img, scaleSize , 0, 0, INTER_AREA);
//
//        Mat B =  Mat.zeros(256,256, CvType.CV_8U);
//        Mat D = Mat.zeros(256,256,CvType.CV_8U);
//        Mat C = Mat.zeros(256,256,CvType.CV_8U);
//
//        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2BGR);
//        List<Mat> BGR = new ArrayList<Mat>(3);
//        Core.split(img, BGR);
//        Mat mB = BGR.get(0);
//        Mat mG = BGR.get(1);
//        Mat mR = BGR.get(2);
//
//
//
//        B.convertTo(B,CvType.CV_64FC3);
//        double[] b = new double[(int) (B.total()*B.channels())];
//        B.get(0,0,b);
//        Log.i(TAG,"rgjifji: "+b.length);
//        D.convertTo(D,CvType.CV_64FC3);
//        double[] d = new double[(int) (D.total()*D.channels())];
//        D.get(0,0,d);
//
//        C.convertTo(C,CvType.CV_64FC3);
//        double[] c = new double[(int) (C.total()*C.channels())];
//        C.get(0,0,c);
//
//        mB.convertTo(mB,CvType.CV_64FC3);
//        double[] mb = new double[(int) (mB.total()*mB.channels())];
//        mB.get(0,0,mb);
//
//        mG.convertTo(mG,CvType.CV_64FC3);
//        double[] mg = new double[(int) (mG.total()*mG.channels())];
//        mG.get(0,0,mg);
//
//        mR.convertTo(mR,CvType.CV_64FC3);
//        double[] mr = new double[(int) (mR.total()*mR.channels())];
//        mR.get(0,0,mr);
//
//
//        Log.i(TAG,"length: "+mg.length);
//
//        for(int i=0;i<mg.length;i++){
//
//
//            d[i] = Math.min(Math.min(mb[i],mg[i]),mr[i]);
//            b[i] = Math.max(Math.max(mb[i],mg[i]),mr[i]);
//
//            c[i] = b[i]-d[i];
//
//
//
//        }
//        Log.i(TAG,"length d: "+d.length);
//        Log.i(TAG,"length b: "+b.length);
//        Log.i(TAG,"length c: "+c.length);
//
//
//
//
//
//        double sum =0;
//        double summ =0;
//        for(int i=0;i<d.length;i++){
//
//            sum =  (sum+d[i]);
////            Log.i(TAG, "d" + i + ": " + d[i] + " sum : " + sum);
//
//
//        }
//        sum = sum/(D.size(1)*D.size(2));
//        Log.i(TAG,"sum: "+sum);
//
//        for(int i=0;i<b.length;i++){
//
//            summ =  (summ+b[i]);
//
//        }
//        summ = summ/( B.size(1)*B.size(2));
//        Log.i(TAG,"SUMM: "+summ);
//
//        double cc;
//        cc = summ-sum;
//        Log.i(TAG,"c: "+cc);
//
//
//
//        double A;
//        A = (0.33)*(Core.minMaxLoc(mB).maxVal) + (0.06)*summ;
//        Log.i(TAG + "max", (Core.minMaxLoc(B).maxVal) + "");
//        Log.i(TAG,"Airlight: "+ A);
//
//        double x1,x2;
//
//        x1 = (A-sum)/A;
//        Log.i(TAG,"X1: "+x1);
//        Log.i(TAG + "X1 calc",(A-sum)/A + "");
//        x2 = cc/A;
//        Log.i(TAG,"X2: "+x2);
//        Log.i(TAG + "X2 calc",cc/A + "");
//
//        double u = 10.127489 ,v= -8.336512 ,s = 0.13606234;
//        Log.i(TAG, x1 + " " +x2 + " " + Math.exp(-0.5*(u*x1 + v*x2)+s));
//
//        float w;
//        w = (float) Math.exp(-0.5*(u*x1 + v*x2)+s);
//
//        Log.i(TAG,"omega: "+w);
//        //Toast.makeText(getApplicationContext(),"omega: "+w,Toast.LENGTH_SHORT).show();
//
//
//        int a = 0;
//        if(w<=0.1)
//            a = 0;
//        else if(w<=0.3)
//            a=1;
//        else if(w<=0.5)
//            a=2;
//        else if(w<=0.7)
//            a=3;
//        else if(w<=0.8)
//            a=4;
//        else if(w<=0.9)
//            a=5;
//
//        Log.i(TAG,"Omega Factor: "+a);
//        //Toast.makeText(getApplicationContext(),"omega Factor: "+a,Toast.LENGTH_SHORT).show();
//
//        return w;
//    }

    /*
     * Getting location to call two apis
     * one by taking latitude and longitude
     * and another by finding nearest cpcb center
     */
    void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        latitude = Double.parseDouble(String.valueOf(location.getLatitude()));
        longitude = Double.parseDouble(String.valueOf(location.getLongitude()));

        formatter = NumberFormat.getNumberInstance();
        formatter.setMinimumFractionDigits(1);
        formatter.setMaximumFractionDigits(1);

        noDecimalLat = formatter.format(latitude);
        noDecimalLong = formatter.format(longitude);

        Location location1;

        location1 = new Location("All locations");



        Log.i(TAG, "Latitude: " + latitude + "\n Longitude: " + longitude);
        Log.i(TAG, "No decimal Latitude: " + noDecimalLat + "\n no Decimal longitude: " + noDecimalLong);

        //Toast.makeText(getApplicationContext(), "Latitude: " + latitude + "Longitude: " + longitude, Toast.LENGTH_SHORT).show();
        float[] a = new float[arrayList.size()];
        String[] name = new String[arrayList.size()];
        if (flag == 0) {
            int i;
            float min = 9999999;
            for (i = 0; i < arrayList.size(); i++) {
                cpcbCenterList obj = arrayList.get(i);
                //            Location dest = new Location();
                location1.setLatitude(obj.mlat);
                location1.setLongitude(obj.mlon);
                distance = location.distanceTo(location1);//in meters
                a[i] = distance;
                name[i] = obj.getMlocationName();
                if (distance < min) {
                    min = distance;
                    nearest = (obj.getMlocationName());
                }
            }
            flag = 1;
            for (i = 0; i < arrayList.size(); i++) {
                Log.i(TAG, name[i] + "\t \t \t" +
                        " distance" + a[i] + "\t Latitude dest:" + arrayList.get(i).mlat + "\t Longitude dest:" + arrayList.get(i).mlon + "\t :atitude mine" + location.getLatitude() + " Longitude mine" + location.getLongitude() + "");
            }

            Log.i(TAG, "Nearest Place:" + nearest + " \t distance: " + min);
        }
//        if(tem_flag==0) {


        new GetHumidity().execute();
        start_fetch_prev();

    }

    @Override
    public void onProviderDisabled(String provider) {
        //Toast.makeText(HdrViewfinderActivity.this, "Please Enable GPS and connect to the Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    private Float hour() {

        Calendar calendar = Calendar.getInstance();
        float h = calendar.get(Calendar.HOUR_OF_DAY);

        Log.i(TAG, "hour: " + h);
        //Toast.makeText(getApplicationContext(), "Hour: " + h, Toast.LENGTH_SHORT).show();
        return h;

    }

    void start_fetch_prev() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference myRef = database.getReference("message");
        ref = FirebaseDatabase.getInstance().getReference();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                Map<String, Object> td = (HashMap<String,Object>) dataSnapshot.getValue();
//
//                List<String> values = new ArrayList(td.keySet());
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {

                    // Prints the list of all the dates
                    String date = dataSnapshot1.getKey();
//                    Log.i(TAG, date);

                    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH");
                    Calendar calendar = Calendar.getInstance();
//                    calendar.set(Calendar.HOUR, 7);
                    calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) - 1);
                    calendar.set(Calendar.DATE, calendar.get(Calendar.DATE));

//                    calendar.add(Calendar.HOUR,-1);
//            Date oneHourBack = calendar.getTime();
                    Log.i(TAG, "Time: " + dateFormat.format(calendar.getTime()));
//                    Log.i(TAG,"Time mine: "+ Calendar.YEAR + "-" + Calendar.MONTH + "-" + Calendar.DATE + " " + Calendar.HOUR);
//                    Log.i(TAG,"Time mine: "+ calendar.get());
                    Log.i(TAG, "Time date: " + date);
//                    Calendar cal = Calendar.getInstance();
//                    cal.add(Calendar.HOUR,-1);
//                    cal.add(Calendar.MINUTE,-15);
//                    Calendar cal2 = Calendar.getInstance();
//                    cal2.add(Calendar.HOUR,-1);
//                    cal2.add(Calendar.MINUTE,-10);
//                    Calendar cal3 = Calendar.getInstance();
//                    cal3.add(Calendar.HOUR,-1);
//                    cal3.add(Calendar.MINUTE,-5);

                    //replace quotes by prev hour date time
//                    if(date.contains(dateFormat.format(calendar.getTime()).substring(0,13)))
//                    {
//                        Log.i(TAG,"MATCHED: " + date);
//                        keyDate = date;
//                        fetch_prev();
//                        break;  //remove if causes issues
//                    }

//                    int year = calendar.get(Calendar.YEAR);
//                    int month = calendar.getTime().getMonth();
//                    int day = calendar.get(Calendar.DATE);
//                    int hour = calendar.get(Calendar.HOUR);
//                    String year = dateFormat.format(calendar.getTime()).substring(0,4);
//                    String month = dateFormat.format(calendar.getTime()).substring(5,7);
//                    String day = dateFormat.format(calendar.getTime()).substring(8,10);
//                    String hour = dateFormat.format(calendar.getTime()).substring(11,13);
//                    String min = dateFormat.format(calendar.getTime()).substring(14,15);


                    String time = dateFormat.format(calendar.getTime());


//                    Log.i(TAG, "year: " + date.substring(6,10) + " " + year);
////                    Log.i(TAG, "year bool: " + (Integer.parseInt(date.substring(6,10)) == year));
//                    Log.i(TAG, "year bool: " + ((date.substring(6,10)).equals(year)));
//
//                    Log.i(TAG, "month: " + (date.substring(3,5)) + " " + month);
////                    Log.i(TAG, "month bool: " + (Integer.parseInt(date.substring(3,5)) == month));
//                    Log.i(TAG, "month bool: " + ((date.substring(3,5)).equals(month)));

//                    Log.i(TAG, "day: " + date.substring(0,2) + " " + (day));
//                    Log.i(TAG, "hour: " + date.substring(11,13) + " "  + hour);
//                    Log.i(TAG, "min: " + date.substring(14,15) + " "  + min);

                    try {
//                        if (date.substring(6, 10).equals(year)) {
//                            if ((date.substring(3, 5)).equals(month)) {
//                                if ((date.substring(0, 2)).equals(day)) {
//
//                                    if (date.substring(11, 13).equals(hour)) {
                        if (date.contains(time)) {
                            Log.i(TAG, "mATCHED " + calendar.getTime().toString() + " " + date + " ");
                            Log.i(TAG, "MATCHED: " + date);
                            keyDate = date;
                            fetch_prev();
                            break;  //remove if causes issues

                        }
//                                    }
//                                }
//                            }
//                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
//                    {
//                        Log.i(TAG,"MATCHED: " + date);
//                        keyDate = date;
//                        fetch_prev();
//                        break;  //remove if causes issues
//                    }


//                    else if(date.equals(dateFormat.format(cal.getTime()))){
//                        Log.i("MATCHED", date);
//                        keyDate = date;
//                        fetch_prev();
//                        break;
//                    }
//                    else if(date.equals(dateFormat.format(cal2.getTime()))){
//                        Log.i("MATCHED", date);
//                        keyDate = date;
//                        fetch_prev();
//                        break;
//                    }
//                    else if(date.equals(dateFormat.format(cal2.getTime()))){
//                        Log.i("MATCHED", date);
//                        keyDate = date;
//                        fetch_prev();
//                        break;
//                    }
//                    else{
//                        Log.i("MATCHED", date);
//                        keyDate = date;
//                        fetch_prev();
//                        break;
//                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    void fetch_prev() {
//        keyDate ="23-08-2019 16:53:18";
        Log.i(TAG, "START fetch" + keyDate);
//        ref = FirebaseDatabase.getInstance().getReference(keyDate).child("records");
        ref = FirebaseDatabase.getInstance().getReference(keyDate).child("records");


        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    final String key = dataSnapshot1.getKey();
//                    Log.i(TAG, "KEY " + dataSnapshot1.getKey());

//                    Log.i(TAG, "WHOLE " + dataSnapshot1.getValue().toString());

                    DatabaseReference df = ref.child(key);
                    final String value = dataSnapshot1.getValue().toString();
                    df.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {

                            Log.i(TAG, "TEST: " + dataSnapshot2.child("station").getValue() + " " + dataSnapshot2.child("pollutant_id").getValue() + " " + dataSnapshot2.child("pollutant_avg").getValue());
                            //Put nearest in equal(<NEAREST>)
                            if (dataSnapshot2.child("station").getValue().equals(nearest)) {
                                if (dataSnapshot2.child("pollutant_id").getValue().equals("PM2.5")) {

                                    Log.i(TAG, dataSnapshot2.child("station").getValue() + " " + dataSnapshot2.child("pollutant_id").getValue() + " " + dataSnapshot2.child("pollutant_avg").getValue());
                                    features[0][9] = (Float.parseFloat(dataSnapshot2.child("pollutant_avg").getValue().toString()) - 10.0f) / (190.0f - 10.0f);
                                    Log.i(TAG, "Feature 0 9: " + features[0][9]);
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null, fosa = null;
            Bitmap bitmap_size = null;

            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // create bitmap
                    bitmap_size = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap_size.copyPixelsFromBuffer(buffer);

                    // write bitmap to a file
                    fos = new FileOutputStream(STORE_DIRECTORY + "/" + 1 + ".png");
                    fosa = new FileOutputStream(STORE_DIRECTORY + "/" + 11 + ".png");

                    bitmap = Bitmap.createBitmap(bitmap_size, 0, 50, mWidth, mHeight - 100);
//                    resized.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fosa);
                    Log.i("bitmap", bitmap.getByteCount() + "");
                    IMAGES_PRODUCED++;
                    Log.e(TAG, "captured image: " + IMAGES_PRODUCED);
//                    //Toast.makeText(HdrViewfinderActivity.this,   mWidth + " " + mHeight, Toast.LENGTH_SHORT).show();
                    stopProjection();
                    tem_flag = 1;


                    ///////////////////////////////

                    readImageFromResources();

//                    long lstartContrast =System.nanoTime();
//                    contrast();
//                    long lEndContrast = System.nanoTime();
//                    long outputContrast = lEndContrast-lstartContrast;
//                    // //Toast.makeText(getApplicationContext(),"Contrast: " + outputContrast / 1000000,Toast.LENGTH_LONG).show();
//                    Log.i(Tagg,"Contrast: " + outputContrast / 1000000+" ms");
//
//                    long lStartEntropy = System.nanoTime();
//                    entropy();
//                    long lEndEntropy = System.nanoTime();
////
//                    long outputEntropy = lEndEntropy-lStartEntropy;
////                // //Toast.makeText(getApplicationContext(),"Entropy: " + outputEntropy / 1000000,Toast.LENGTH_LONG).show();
//                    Log.i(Tagg,"Entropy: " + outputEntropy / 1000000+" ms");
//
//
//                    long lStartOmega = System.nanoTime();
//                    omega();
//
//                    long lEndOmega = System.nanoTime();
////
//                    long outputOmega = lEndOmega-lStartOmega;
////                // //Toast.makeText(getApplicationContext(),"Entropy: " + outputEntropy / 1000000,Toast.LENGTH_LONG).show();
//                    Log.i(Tagg,"Omega: " + outputOmega / 1000000+" ms");
//
//
//
//                    long lStartHour = System.nanoTime();
//                    hour();
//
//                    long lEndHour = System.nanoTime();
////
//                    long outputHour = lEndHour-lStartHour;
////                // //Toast.makeText(getApplicationContext(),"Entropy: " + outputEntropy / 1000000,Toast.LENGTH_LONG).show();
//                    Log.i(Tagg,"Hour: " + outputHour / 1000000+" ms");
//

                    Log.i(TAG, "Length of features: " + features.length);

                    //hours, cont, ent , hum, tempr, deg, speed, pressure,omega
                    features[0][8] = (omega() / 5.0f);

                    features[0][0] = (hour() - 6.0f) / (18.0f - 6.0f);
                    features[0][1] = (contrast() - 0.7401840498268867f) / (93.6343407580949f - 0.7401840498268867f);
                    features[0][2] = (entropy() - 1.4675911664962769f) / (57.449180603027344f - 1.4675911664962769f);


                    feature_check = 1;
                    if (features[0][3] != -9999) {
                        Intent intent = new Intent(HdrViewfinderActivity.this, Predict_Train.class);
                        Bundle mBundle = new Bundle();
                        mBundle.putSerializable("Features", features);
                        mBundle.putFloatArray("Labels", labels);
                        mBundle.putFloat("Distance", distance);
                        intent.putExtras(mBundle);
                        tem_flag = 1;
                        finishActivity(0);
                        labels[0] = (float) cpcbLabel;
                        for (int i = 0; i < features.length; i++) {
                            for (int j = 0; j < features[i].length; j++) {
                                Log.i(TAG, "features " + i + " " + j + ": " + features[0][j]);
                            }
                        }
                        feature_check = 0;
                        startActivity(intent);
                    }


                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                if (fosa != null) {
                    try {
                        fosa.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                if (bitmap != null) {
                    bitmap.recycle();
                }


                if (image != null) {
                    image.close();
                }
            }
        }
    }

    private class OrientationChangeCallback extends OrientationEventListener {

        OrientationChangeCallback(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            final int rotation = mDisplay.getRotation();
            if (rotation != mRotation) {
                mRotation = rotation;
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.e("ScreenCapture", "stopping projection.");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
                    sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }

    /**
     * Async task class to get json by making HTTP call
     * /**
     * Async task class to get json by making HTTP call
     */
    private class GetHumidity extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
//            pDialog = new ProgressDialog(CameraActivity.this);
//            pDialog.setMessage("Please wait...");
//            pDialog.setCancelable(false);
//            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + noDecimalLat + "&lon=" + noDecimalLong + "&appid=0e53582379625c9fbf0effdbc9cf84e4";
            String url1 = "https://api.data.gov.in/resource/3b01bcb8-0b14-4abf-b6f2-c1bfd384ba69?format=json&api-key=579b464db66ec23bdd000001f8be9fca0a2849646ab0617351ce187b&filters[station]=" + nearest;


            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);
            String jsonStrAqi = sh.makeServiceCall(url1);

            Log.e(TAG, "Response from url: " + jsonStr);
            Log.e(TAG, "Response from url cpcb: " + jsonStrAqi);


            if (jsonStr != null || jsonStrAqi != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONObject jsonMain = jsonObj.getJSONObject("main");
                    long lstartContrast = System.nanoTime();
                    humi = Float.parseFloat(jsonMain.getString("humidity"));
                    Log.i("humi", humi + "");
                    long lEndContrast = System.nanoTime();
                    long outputContrast = lEndContrast - lstartContrast;
                    // //Toast.makeText(getApplicationContext(),"Contrast: " + outputContrast / 1000000,Toast.LENGTH_LONG).show();
//                    Log.i(Tagg,"Humi: " + outputContrast / 1000000+" ms");
                    long lStartEntropy = System.nanoTime();
                    temp = Float.parseFloat(jsonMain.getString("temp"));

                    long lEndEntropy = System.nanoTime();
//
                    long outputEntropy = lEndEntropy - lStartEntropy;
//                // //Toast.makeText(getApplicationContext(),"Entropy: " + outputEntropy / 1000000,Toast.LENGTH_LONG).show();
                    Log.i(Tagg, "temp: " + outputEntropy / 1000000 + " ms");


                    long lStartOmega = System.nanoTime();
                    pressure = Float.parseFloat(jsonMain.getString("pressure"));


                    long lEndOmega = System.nanoTime();
//
                    long outputOmega = lEndOmega - lStartOmega;
//                // //Toast.makeText(getApplicationContext(),"Entropy: " + outputEntropy / 1000000,Toast.LENGTH_LONG).show();
                    Log.i(Tagg, "Pressure: " + outputOmega / 1000000 + " ms");


                    JSONObject jsonObject = jsonObj.getJSONObject("wind");
                    long lStartHour = System.nanoTime();
                    deg = Float.parseFloat(jsonObject.getString("deg"));


                    long lEndHour = System.nanoTime();
//
                    long outputHour = lEndHour - lStartHour;
//                // //Toast.makeText(getApplicationContext(),"Entropy: " + outputEntropy / 1000000,Toast.LENGTH_LONG).show();
                    Log.i(Tagg, "deg: " + outputHour / 1000000 + " ms");

                    speed = Float.parseFloat(jsonObject.getString("speed"));

//                    //Toast.makeText(getApplicationContext(),"HUmidity: "+humi,Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Humidity: " + humi + "temperature:" + temp + "pressure: " + pressure + "degree: " + deg + "wind speed: " + speed);

                    JSONObject jsonObject2 = new JSONObject(jsonStrAqi);
                    JSONArray jsonObject1 = jsonObject2.getJSONArray("records");

                    for (int i = 0; i < jsonObject1.length(); i++) {

                        JSONObject c = jsonObject1.getJSONObject(i);
                        stationName = c.getString("station");
                        pollutant_id = c.getString("pollutant_id");
                        maxPollutant = c.getString("pollutant_max");
                        minPollutant = c.getString("pollutant_min");

                        if (pollutant_id.equals("PM2.5")) {
                            if (maxPollutant.equals("NA")) {
                                cpcbLabel = 19.0;
                            } else {
                                cpcbLabel = Float.parseFloat(c.getString("pollutant_avg"));
                                Log.i(TAG, "cpcb Cabel: " + cpcbLabel);

                            }
                        }

                        Log.i(TAG, "Station: " + stationName + "pollutant_id: " + pollutant_id + "\nmaxPollutant: " + maxPollutant + "\nminPollutant: " + minPollutant);
                    }
                    features[0][3] = (humi - 6.0f) / (100.0f - 6.0f);
                    features[0][4] = (temp - 25.7f) / (48.0f - 25.7f);
                    features[0][5] = (deg - 10f) / (350f - 10f);
                    features[0][6] = (speed - 1f) / (28f - 1f);
                    features[0][7] = (pressure - 992.0f) / (1003.0f - 992.0f);
                    labels[0] = (float) cpcbLabel;
                    for (int i = 0; i < features.length; i++) {
                        for (int j = 0; j < features[i].length; j++) {
                            Log.i(TAG, "features: " + features[0][j]);
                        }
                    }


                    for (int i = 0; i < labels.length; i++) {
                        Log.i(TAG, "Labels: " + labels[i]);
                    }


                    if (feature_check == 1) {       //all features extracted and api call in the end
                        Intent intent = new Intent(HdrViewfinderActivity.this, Predict_Train.class);
                        Bundle mBundle = new Bundle();
                        mBundle.putSerializable("Features", features);
                        mBundle.putFloatArray("Labels", labels);
                        mBundle.putFloat("Distance", distance);
                        intent.putExtras(mBundle);
                        tem_flag = 1;
                        finishActivity(0);
                        labels[0] = (float) cpcbLabel;
                        for (int i = 0; i < features.length; i++) {
                            for (int j = 0; j < features[i].length; j++) {
                                Log.i(TAG, "features: " + features[0][j]);
                            }
                        }
                        startActivity(intent);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(getApplicationContext(),
//                                    "Json parsing error: " + e.getMessage(),
//                                    Toast.LENGTH_LONG)
//                                    .show();
                        }
                    });

                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(getApplicationContext(),
//                                "Couldn't get json from server. Check LogCat for possible errors!",
//                                Toast.LENGTH_LONG)
//                                .show();
                    }
                });

            }


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
//            if (pDialog.isShowing())
//                pDialog.dismiss();

        }

    }


}
