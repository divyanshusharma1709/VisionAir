package com.airquality.VisionAir;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.google.firebase.database.FirebaseDatabase;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {
    SharedPreferences pref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        final CheckBox optIn = findViewById(R.id.pmbox);
        pref = getApplicationContext().getSharedPreferences("Pref", 0);
        optIn.setChecked(pref.getBoolean("Contrib", false));
        optIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (optIn.isChecked()) {
                    pref = getApplicationContext().getSharedPreferences("Pref", 0);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("Contrib", true);
                    editor.commit();
                    MyAsyncTask setPrefFlag = new MyAsyncTask(SettingsActivity.this, null, "setPrefFlag", "True", new MyAsyncTask.AsyncResponse() {
                        @Override
                        public void processFinish(String output) {
                            Log.i("SetPrefFlag", output);
                        }
                    });
                    setPrefFlag.execute();
                }
                else
                {
                    pref = getApplicationContext().getSharedPreferences("Pref", 0);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("Contrib", false);
                    editor.commit();
                    MyAsyncTask setPrefFlag = new MyAsyncTask(SettingsActivity.this, null, "setPrefFlag", "True", new MyAsyncTask.AsyncResponse() {
                        @Override
                        public void processFinish(String output) {
                            Log.i("SetPrefFlag", output);
                        }
                    });
                    setPrefFlag.execute();
                }
            }
        });
    }
}
