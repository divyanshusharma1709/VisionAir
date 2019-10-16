package com.airquality.VisionAir;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;

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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.opencv.imgproc.Imgproc.INTER_AREA;

public class CameraActivitynoHDR extends AppCompatActivity implements LocationListener {

    public static final String TAG = "CameraActivity";
    public static final String Tagg = "new";
    public static Bitmap bitmap;
    long startTime;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    int flag, feature_check = 0;
    float distance;
    GraphView graphview;
    long output, outputentropy, outputhour, outputomega, outputapi;
    DatabaseReference ref;
    String keyDate;
    int tem_flag = 0;
    double latitude;
    float humi, pressure, temp, speed, deg;
    double longitude;
    String nearest = " ";
    Mat img;
    int currentHourIn24Format;
    ArrayList<cpcbCenterList> arrayList = new ArrayList<>();
    byte[] graphDef;
    Session sess;
    Graph graph;
    File file;
    int flagt = 0;
    Tensor<String> checkpointPrefix;
    String checkpointDir;
    float[][] features = new float[1][10];
    float[] labels = new float[1];
    int epochs = 1;
    double cpcbLabel;
    Mat imgcopy;
    String stationName;
    String pollutant_id;
    String maxPollutant;
    String minPollutant;
    LocationManager locationManager;
    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            //Toast.makeText(getApplicationContext(), "Bitmap", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Bitmap");

            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);


            readImageFromResources();
            contrast();
            entropy();
            omega();
            hour();




            Log.i(TAG, "Length of features: " + features.length);

            //hours, cont, ent , hum, tempr, deg, speed, pressure,omega

            features[0][8] = (omega() / 5.0f);

            features[0][0] = (hour() - 6.0f) / (18.0f - 6.0f);
            features[0][1] = (contrast() - 0.7401840498268867f) / (93.6343407580949f - 0.7401840498268867f);
            features[0][2] = (entropy() - 1.4675911664962769f) / (57.449180603027344f - 1.4675911664962769f);
            feature_check = 1;
            if (features[0][3] != -9999) {
                Intent intent = new Intent(CameraActivitynoHDR.this, Predict_Train.class);
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


            double contrast = 0;


        }
    };
    private Camera mCamera;
    private CameraPreview mPreview;
    private Button capture, nextButton;
    private Context myContext;
    private LinearLayout cameraPreview;
    private boolean cameraFront = false;
    private ProgressDialog pDialog;
    private NumberFormat formatter;
    private String noDecimalLat, noDecimalLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getSupportActionBar().hide();
        file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "/Weights.bin");
        startTime = SystemClock.elapsedRealtime();

        setContentView(R.layout.activity_camera);

        for (int i = 0; i < 9; i++) {
            features[0][i] = -9999;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);

        }


        if (MainActivity.nearest != "") {
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
        Log.i("Main nearest", MainActivity.nearest);

        OpenCVLoader.initDebug();
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

        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        cameraPreview = findViewById(R.id.cPreview);
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);

        capture = findViewById(R.id.btnCam);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "SCENE MODE before" + mCamera.getParameters().getSceneMode());

                Camera.Parameters cameraParameters = mCamera.getParameters();
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);


//                cameraParameters.setSceneMode("hdr");

//                cameraParameters.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
                cameraParameters.setJpegQuality(100);

                mCamera.setParameters(cameraParameters);
                Log.i(TAG, "SCENE MODE after" + mCamera.getParameters().getSceneMode());

                mCamera.takePicture(null, null, mPicture);

            }
        });


        mCamera.startPreview();


    }

    public void onResume() {

        super.onResume();
        if (mCamera == null) {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
//            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
            Log.d("nu", "null");
        } else {
            Log.d("nu", "no null");
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

//    private Camera.PictureCallback getPictureCallback() {
//         mPicture = new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//                //Toast.makeText(getApplicationContext(),"Bitmap",Toast.LENGTH_SHORT).show();
//                Log.i(TAG,"Bitmap");
//
//                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
////                readImageFromResources();
////                contrast();
////                entropy();
//            }
//        };
//        return mPicture;
//    }

    private Mat readImageFromResources() {
        img = new Mat();

        Utils.bitmapToMat(bitmap, img);
        imgcopy = img;
        return img;
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
//        Size scaleSize = new Size(256,256);
//        Imgproc.resize(imgcopy,imgcopy, scaleSize , 0, 0, INTER_AREA);
//
//        Mat B =  Mat.zeros(256,256,CvType.CV_8U);
//        Mat D = Mat.zeros(256,256,CvType.CV_8U);
//        Mat C = Mat.zeros(256,256,CvType.CV_8U);
//
//        Imgproc.cvtColor(imgcopy, imgcopy, Imgproc.COLOR_RGB2BGR);
//        List<Mat> BGR = new ArrayList<Mat>(3);
//        Core.split(imgcopy, BGR);
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
//        B.get(0,0,mr);
//
//
//        Log.i(TAG,"length: "+mg.length);
//
//        for(int i=0;i<mg.length;i++){
//
//
//                d[i] = Math.min(Math.min(mb[i],mg[i]),mr[i]);
//                b[i] = Math.max(Math.max(mb[i],mg[i]),mr[i]);
//
//                c[i] = b[i]-d[i];
//
//
//
//        }
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
//        A = (0.33)*(Core.minMaxLoc(B).maxVal) + (0.06)*summ;
//        Log.i(TAG,"Airlight: "+ A);
//
//        double x1,x2;
//
//        x1 = (A-sum)/A;
//        Log.i(TAG,"X1: "+x1);
//
//        x2 = cc/A;
//
//        double u = 10.127489 ,v= -8.336512 ,s = 0.13606234;
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
//        a=1;
//        else if(w<=0.5)
//        a=2;
//        else if(w<=0.7)
//        a=3;
//        else if(w<=0.8)
//        a=4;
//        else if(w<=0.9)
//        a=5;
//
//        Log.i(TAG,"Omega Factor: "+a);
//        //Toast.makeText(getApplicationContext(),"omega Factor: "+a,Toast.LENGTH_SHORT).show();
//
//        return w;
//    }


//    private float omegaa(){
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
//        Log.i(TAG, img.get(0,0)[0] + " " +  img.get(0,0)[1] + img.get(0,0)[2]);
////        Log.i(TAG, mB.dump());
////        Log.i(TAG, mG.dump());
////        Log.i(TAG, mR.dump());
//
//        double sum = 0, summ = 0;
//
//        for(int i = 0; i< mB.rows(); i++){
//            for (int j=0; j< mB.cols() ; j++)
//            {
//                double BGmin = Math.min(mB.get(i,j)[0], mG.get(i,j)[0]);
//                double BGRmin = Math.min(BGmin, mR.get(i,j)[0]);
////                double min = Math.min(BGmin, BGRmin);
//                sum = sum + BGRmin;
//                D.put(i,j, BGRmin);
//
//                double BGmax = Math.max(mB.get(i,j)[0], mG.get(i,j)[0]);
//                double BGRmax = Math.max(BGmax, mR.get(i,j)[0]);
//                double finalmax = (BGRmax);
//                summ = summ +  finalmax;
//                B.put(i,j,finalmax);
//
////                Log.i(TAG, mB.get(i,j)[0] + " " + mG.get(i,j)[0] + " " + mR.get(i,j)[0]);
//
////                Log.i(TAG, "Min: " + sum + " Max: " + summ + " minn: " + BGRmin + " Max: " + finalmax);
//
//                C.put(i,j, finalmax - BGRmin);
//
//
//            }
//
//        }
//
//        sum = sum/65536;
//
//        summ = summ/65536;
//
//        Log.i(TAG,"sum: "+ sum);
//        Log.i(TAG,"summ: "+ summ);
//
//
//        double cc = summ - sum;
//        Log.i(TAG,"cc: "+ cc);
//
//
//        double A;
//        A = (0.33)*(Core.minMaxLoc(mB).maxVal) + (0.06)*summ;
//
//        Log.i(TAG + "max", (Core.minMaxLoc(B).maxVal) + "");
//        Log.i(TAG,"Airlight: "+ A);
//
//        double x1,x2;
//
//        x1 = (A-sum)/A;
//        Log.i(TAG,"X1: "+x1);
//        Log.i(TAG + "X1 cal",(A-sum)/A + "");
//        x2 = cc/A;
//        Log.i(TAG,"X2: "+x2);
//        Log.i(TAG + "X2 cal",cc/A + "");
//
//        double u = 10.127489 ,v= -8.336512 ,s = 0.13606234;
//        Log.i(TAG, x1 + " " +x2 + " " + Math.exp(-0.5*(u*x1 + v*x2)+s));
//
//        float w;
//        w = (float) Math.exp(-0.5*(u*x1 + v*x2)+s);
//
//        Log.i(TAG,"omegaaaa: "+w);
//        //Toast.makeText(getApplicationContext(),"omegaaaa: "+w,Toast.LENGTH_SHORT).show();
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
//        Log.i(TAG,"Omegaaa Factor: "+a);
//        //Toast.makeText(getApplicationContext(),"omegaaaa Factor: "+a,Toast.LENGTH_SHORT).show();
//
//        return w;
//
////        for(int i=0; i<D.dump().length(); i++){
////            sum = sum + ;
////        }
//
//
//
////        return 1;
//
//    }


    private float omega() {

        //Resize Image
        Size scaleSize = new Size(256, 256);
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
        Size scaleSize = new Size(256, 256);
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

    public float entropy() {

        //Resize Image
        Size scaleSize = new Size(256, 256);
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
     * Getting location to call two apis
     * one by taking latitude and longitude
     * and another by finding nearest cpcb center
     */
    void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 1, this);
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

        distance = 0;


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


//        }
        new GetHumidity().execute();
        start_fetch_prev();

    }

    @Override
    public void onProviderDisabled(String provider) {
        //Toast.makeText(CameraActivitynoHDR.this, "Please Enable GPS and connect to the Internet", Toast.LENGTH_SHORT).show();
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
//    void start_fetch_prev(){
//        FirebaseDatabase database = FirebaseDatabase.getInstance();
////        DatabaseReference myRef = database.getReference("message");
//        ref = FirebaseDatabase.getInstance().getReference();
//
//        ref.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
////                Map<String, Object> td = (HashMap<String,Object>) dataSnapshot.getValue();
////
////                List<String> values = new ArrayList(td.keySet());
//                for(DataSnapshot dataSnapshot1: dataSnapshot.getChildren()){
//
//                    // Prints the list of all the dates
//                    String date = dataSnapshot1.getKey().toString();
////                    Log.i(TAG, date);
//
//                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                    Calendar calendar = Calendar.getInstance();
////                    calendar.set(Calendar.HOUR, 7);
//                    calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR)-1);
//                    calendar.set(Calendar.DATE, calendar.get(Calendar.DATE));
//
////                    calendar.add(Calendar.HOUR,-1);
////            Date oneHourBack = calendar.getTime();
//                    Log.i(TAG,"Time: "+dateFormat.format(calendar.getTime()).substring(0,13));
////                    Log.i(TAG,"Time mine: "+ Calendar.YEAR + "-" + Calendar.MONTH + "-" + Calendar.DATE + " " + Calendar.HOUR);
////                    Log.i(TAG,"Time mine: "+ calendar.get());
//                    Log.i(TAG, "Time date: " + date);
////                    Calendar cal = Calendar.getInstance();
////                    cal.add(Calendar.HOUR,-1);
////                    cal.add(Calendar.MINUTE,-15);
////                    Calendar cal2 = Calendar.getInstance();
////                    cal2.add(Calendar.HOUR,-1);
////                    cal2.add(Calendar.MINUTE,-10);
////                    Calendar cal3 = Calendar.getInstance();
////                    cal3.add(Calendar.HOUR,-1);
////                    cal3.add(Calendar.MINUTE,-5);
//
//                    //replace quotes by prev hour date time
////                    if(date.contains(dateFormat.format(calendar.getTime()).substring(0,13)))
////                    {
////                        Log.i(TAG,"MATCHED: " + date);
////                        keyDate = date;
////                        fetch_prev();
////                        break;  //remove if causes issues
////                    }
//
////                    int year = calendar.get(Calendar.YEAR);
////                    int month = calendar.getTime().getMonth();
////                    int day = calendar.get(Calendar.DATE);
////                    int hour = calendar.get(Calendar.HOUR);
//                    String year = dateFormat.format(calendar.getTime()).substring(0,4);
//                    String month = dateFormat.format(calendar.getTime()).substring(5,7);
//                    String day = dateFormat.format(calendar.getTime()).substring(8,10);
//                    String hour = dateFormat.format(calendar.getTime()).substring(11,13);
//                    String min = dateFormat.format(calendar.getTime()).substring(14,15);
//
////                    Log.i(TAG, "year: " + date.substring(6,10) + " " + year);
//////                    Log.i(TAG, "year bool: " + (Integer.parseInt(date.substring(6,10)) == year));
////                    Log.i(TAG, "year bool: " + ((date.substring(6,10)).equals(year)));
////
////                    Log.i(TAG, "month: " + (date.substring(3,5)) + " " + month);
//////                    Log.i(TAG, "month bool: " + (Integer.parseInt(date.substring(3,5)) == month));
////                    Log.i(TAG, "month bool: " + ((date.substring(3,5)).equals(month)));
//
//                    Log.i(TAG, "day: " + date.substring(0,2) + " " + (day));
//                    Log.i(TAG, "hour: " + date.substring(11,13) + " "  + hour);
//                    Log.i(TAG, "min: " + date.substring(14,15) + " "  + min);
//
//                    try {
//                        if (date.substring(6, 10).equals(year)) {
//                            if ((date.substring(3, 5)).equals(month)) {
//                                if ((date.substring(0, 2)).equals(day)) {
//
//                                    if (date.substring(11, 13).equals(hour)) {
//                                        if (date.substring(14, 15).equals(min)) {
//                                            Log.i(TAG, "mATCHED " + calendar.getTime().toString() + " " + date + " ");
//                                            Log.i(TAG, "MATCHED: " + date);
//                                            keyDate = date;
//                                            fetch_prev();
//                                            break;  //remove if causes issues
//
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    catch (RuntimeException e){
//                        e.printStackTrace();
//                    }
////                    {
////                        Log.i(TAG,"MATCHED: " + date);
////                        keyDate = date;
////                        fetch_prev();
////                        break;  //remove if causes issues
////                    }
//
//
//
//
//
//
////                    else if(date.equals(dateFormat.format(cal.getTime()))){
////                        Log.i("MATCHED", date);
////                        keyDate = date;
////                        fetch_prev();
////                        break;
////                    }
////                    else if(date.equals(dateFormat.format(cal2.getTime()))){
////                        Log.i("MATCHED", date);
////                        keyDate = date;
////                        fetch_prev();
////                        break;
////                    }
////                    else if(date.equals(dateFormat.format(cal2.getTime()))){
////                        Log.i("MATCHED", date);
////                        keyDate = date;
////                        fetch_prev();
////                        break;
////                    }
////                    else{
////                        Log.i("MATCHED", date);
////                        keyDate = date;
////                        fetch_prev();
////                        break;
////                    }
//                }
//
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
//    }

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

    /**
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
            long currentTime = SystemClock.elapsedRealtime();
            if(((currentTime - startTime)/1000) > 30)
            {
                //Toast.makeText(CameraActivitynoHDR.this, "Cannot fetch data", Toast.LENGTH_SHORT).show();
            }

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

                    humi = Float.parseFloat(jsonMain.getString("humidity"));
                    temp = Float.parseFloat(jsonMain.getString("temp"));
                    pressure = Float.parseFloat(jsonMain.getString("pressure"));

                    Log.i(TAG,"Humi: "+humi);
                    Log.i(TAG,"Temp: "+temp);
                    Log.i(TAG,"press: "+pressure);


                    JSONObject jsonObject = jsonObj.getJSONObject("wind");

                    deg = Float.parseFloat(jsonObject.getString("deg"));
                    speed = Float.parseFloat(jsonObject.getString("speed"));

                    Log.i(TAG,"Deg: "+deg);
                    Log.i(TAG,"Speed: "+speed);


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
                        Intent intent = new Intent(CameraActivitynoHDR.this, Predict_Train.class);
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
                                    //"Json parsing error: " + e.getMessage(),
                                    //Toast.LENGTH_LONG).show();
                        }
                    });

                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(getApplicationContext(),
                                //"Couldn't get json from server. Check LogCat for possible errors!",
                                //Toast.LENGTH_LONG)
                                //.show();
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



    /* Federated Learning */


    //    public void logWeight(float[] flat) {
//        String s = "";
//        for (int z = 0; z < flat.length / 10; z++) {
//            s += "  " + flat[z];
//        }
//        Log.i("Array: ", s);
//    }

}
