package com.airquality.VisionAir;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
public class MainActivity extends AppCompatActivity implements LocationListener {


    public static final String TAG = "MainActivity";
    public static double latitude;
    public static double longitude;
    public static float distance = 0;
    public static String nearest = "";
    int support, flag;
    DatabaseReference ref;
    LocationManager locationManager;
    ImageView checkk;
    ArrayList<cpcbCenterList> arrayList = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.welcome_ui);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        checkk = findViewById(R.id.button);
        ImageView info = findViewById(R.id.infoBtn);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(intent);
            }
        });
        ImageView settings = findViewById(R.id.settingsBtn);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        checkCamera2Support();
        int permissionCamera = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
        int permissionLocation = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionWriteExternal = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionReadExternal = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCamera != PackageManager.PERMISSION_GRANTED || permissionLocation != PackageManager.PERMISSION_GRANTED || permissionWriteExternal != PackageManager.PERMISSION_GRANTED || permissionReadExternal != PackageManager.PERMISSION_GRANTED) {
            allowpermissioncamera();
            allowpermissionstorage();
        }
        else {
            getLocation();
        }
        SharedPreferences pref = getApplicationContext().getSharedPreferences("Pref", 0);
        if(pref.getBoolean("First", true)) {
            Intent intent = new Intent(MainActivity.this, Main2Activity.class);
            startActivity(intent);
        }
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("First", false);
        editor.commit();
        pref = getApplicationContext().getSharedPreferences("Pref", 0);

        //////////////////////////////////////////////////////////ANIMATION////////////////
        //checkk.setVisibility(View.INVISIBLE);
//        appname = findViewById(R.id.textView);
//        back = findViewById(R.id.imageView);
////        buttonimage = findViewById(R.id.buttonimage);
////        buttonimage.setVisibility(View.INVISIBLE);
//        bird = findViewById(R.id.imageView2);
//        bird.setVisibility(View.INVISIBLE);
//        appname.setVisibility(View.INVISIBLE);
////        checkk.setVisibility(View.INVISIBLE);
//        animatordown = AnimationUtils.loadAnimation(this, R.anim.fromdown);
//        animatorup = AnimationUtils.loadAnimation(this, R.anim.fromup);
//        animatorside = AnimationUtils.loadAnimation(this, R.anim.fromside);
//        animatorsideright = AnimationUtils.loadAnimation(this, R.anim.fromsideright);
//        back.setAnimation(animatorsideright);
//        back.getAnimation().setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//                bird.setVisibility(View.VISIBLE);
//                bird.setAnimation(animatorside);
//
//
////                buttonimage.setVisibility(View.VISIBLE);
////                buttonimage.setAnimation(animatordown);
//
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//
//            }
//        });
//
//        animatorside.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
////                checkk.setVisibility(View.VISIBLE);
////                appname.setVisibility(View.VISIBLE);
////                checkk.setAnimation(animatordown);
////                appname.setAnimation(animatorup);
//                checkk.setAnimation(animatordown);
//                appname.setAnimation(animatorup);
//                checkk.setVisibility(View.VISIBLE);
//                appname.setVisibility(View.VISIBLE);
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//
//            }
//        });
        checkk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity();
            }
        });


        ////////////////////////////////////////////////////////////////////////
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


//        openActivity();
    }


    void getLocation() {
        try {

            //Toast.makeText(getApplicationContext(), "Get Location", Toast.LENGTH_SHORT).show();
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 1, this);

            Log.i(TAG, "Location Success");
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.i(TAG, "Location failure");
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                startActivity(intent);
                Toast.makeText(MainActivity.this, "Please set Location to High Accuracy to Continue", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
//        latitude = String.valueOf(location.getLatitude());
//        longitude = String.valueOf(location.getLongitude());
//        //Toast.makeText(getApplicationContext(),"Latitude: " + latitude + "\n Longitude: " + longitude,Toast.LENGTH_LONG).show();
//        Log.i(TAG,"Latitude: " + latitude + "\n Longitude: " + longitude);

        //Toast.makeText(getApplicationContext(), "New Location", Toast.LENGTH_SHORT).show();
        latitude = Double.parseDouble(String.valueOf(location.getLatitude()));
        longitude = Double.parseDouble(String.valueOf(location.getLongitude()));


        Location location1;

        location1 = new Location("All locations");



//        //Toast.makeText(getApplicationContext(),"Latitude: " + latitude + "\n Longitude: " + longitude,Toast.LENGTH_LONG).show();
        Log.i(TAG, "Latitude: " + latitude + "\n Longitude: " + longitude);
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
                    nearest = obj.getMlocationName();
                }
//                Location.distanceBetween(latitude,longitude,obj.mlat,obj.mlon,a);
                ////Toast.makeText(getApplicationContext(),a[0] + " " + a[1] + " " + a[2] + " " + a[4],Toast.LENGTH_LONG).show();
            }
            flag = 1;
            for (i = 0; i < arrayList.size(); i++) {
                Log.i(name[i], "\t \t \t" +
                        " distance" + a[i] + "\t Latitude dest:" + arrayList.get(i).mlat + "\t Longitude dest:" + arrayList.get(i).mlon + "\t :atitude mine" + location.getLatitude() + " Longitude mine" + location.getLongitude() + "");
            }

            Log.i("Nearest Place:" + nearest, " \t distance: " + min);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(MainActivity.this, "Please Set GPS to High Accuracy", Toast.LENGTH_SHORT).show();
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                17);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean allowCamera2Support(int cameraId) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraIdS = manager.getCameraIdList()[cameraId];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIdS);
            support = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

            if (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
                Log.d(TAG, "Camera " + cameraId + " has LEGACY Camera2 support");
            else if (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
                Log.d(TAG, "Camera " + cameraId + " has LIMITED Camera2 support");
            else if (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
                Log.d(TAG, "Camera " + cameraId + " has FULL Camera2 support");
            else
                Log.d(TAG, "Camera " + cameraId + " has unknown Camera2 support?!");

            return support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED || support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void checkCamera2Support() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int numberOfCameras = 0;
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            try {
                numberOfCameras = manager.getCameraIdList().length;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (AssertionError e) {
                e.printStackTrace();
            }

            if (numberOfCameras == 0) {
                Log.d(TAG, "0 cameras");
            } else {
                for (int i = 0; i < numberOfCameras; i++) {
                    if (!allowCamera2Support(i)) {
                        Log.d(TAG, "camera " + i + " doesn't have limited or full support for Camera2 API");
                    } else {
                        // here you can get ids of cameras that have limited or full support for Camera2 API
                        Log.d(TAG, "camera " + i + "have limited or full support for Camera2 API");


                    }
                }
            }
        }
    }

    private void allowpermissionstorage() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);

    }

    private void allowpermissioncamera() {

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION
        }, 100);
//        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//        startActivity(intent);
//        //Toast.makeText(MainActivity.this, "Please set Location to High Accuracy to Continue", Toast.LENGTH_LONG).show();;


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//                alarmToggle.setEnabled(true);
                //Toast.makeText(getApplicationContext(), "opening2", Toast.LENGTH_SHORT).show();
                Log.i("Location: ", "Inside request granted");
                getLocation();
            }
        }
    }

    public void openActivity() {
        Intent i = new Intent();
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if(!isConnected)
        {
            Toast.makeText(MainActivity.this, "Please connect to the Internet to Continue", Toast.LENGTH_SHORT).show();
            return;
        }


//    if(camera.getParameters().getSceneMode().equals("hdr")){
//        i = new Intent(this,CameraActivity.class);
//    }
//    else
//    {
//        i = new Intent(this,Camera2Activity.class);
//
//    }

        Camera.Parameters parameters = Camera.open().getParameters();
        Log.i("Scene modes", parameters.getSupportedSceneModes() + "");

        try {
            if (parameters.getSupportedSceneModes().contains("hdr")) {
//            Log.i("Scene modes","yess");
//            i = new Intent(this,CameraActivity.class);
                i = new Intent(this, CameraActivity.class);
                i.putExtra("latitude: ", latitude);
                i.putExtra("longitude: ", longitude);
                i.putExtra("nearest: ", nearest);
                //Toast.makeText(getApplicationContext(), "Opening CameraActivity", Toast.LENGTH_SHORT).show();
            } else {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && ((support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3) || (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) || (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED))) {
                    i = new Intent(this, HdrViewfinderActivity.class);
                    //Toast.makeText(getApplicationContext(), "Opening HDRViewFinderActivity", Toast.LENGTH_SHORT).show();

                } else {
                    i = new Intent(this, CameraActivitynoHDR.class);
                    //Toast.makeText(getApplicationContext(), "Opening CameraActivitynoHDr", Toast.LENGTH_SHORT).show();


                }
            }

//        if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)&&(support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)){
//
//        }
//        else if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)&&((support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3)||(support==CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)||(support==CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED))){
//        }
            startActivity(i);

        } catch (Exception e) {

            i = new Intent(this, CameraActivitynoHDR.class);
            //Toast.makeText(getApplicationContext(), "Exception Occurs: CameraActivitynoHDR", Toast.LENGTH_SHORT).show();

            startActivity(i);

        }
    }
    @Override
    protected void onStop() {
        locationManager.removeUpdates(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        locationManager.removeUpdates(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        locationManager.removeUpdates(this);
        super.onDestroy();
    }
}