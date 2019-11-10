package com.airquality.VisionAir;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {
    SharedPreferences pref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        final EditText passET = findViewById(R.id.passET);
        final Button submit = findViewById(R.id.passBTN);
        passET.setVisibility(View.GONE);
        submit.setVisibility(View.GONE);
        final CheckBox optIn = findViewById(R.id.pmbox);
        final CheckBox debugger = findViewById(R.id.devCB);
        pref = getApplicationContext().getSharedPreferences("Pref", 0);

        optIn.setChecked(pref.getBoolean("Contrib", false));
        debugger.setChecked(pref.getBoolean("Debugger", false));

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
                    MyAsyncTask setPrefFlag = new MyAsyncTask(SettingsActivity.this, null, "setPrefFlag", "False", new MyAsyncTask.AsyncResponse() {
                        @Override
                        public void processFinish(String output) {
                            Log.i("SetPrefFlag", output);
                        }
                    });
                    setPrefFlag.execute();
                }
            }
        });
        debugger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(debugger.isChecked())
                {
                    passET.setVisibility(View.VISIBLE);
                    submit.setVisibility(View.VISIBLE);
                    submit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(passET.getText().toString().equals("visionaryfed"))
                            {
                                pref = getApplicationContext().getSharedPreferences("Pref", 0);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putBoolean("Debugger", true);
                                editor.commit();
                                passET.setVisibility(View.GONE);
                                submit.setVisibility(View.GONE);
                                debugger.setChecked(true);
                                passET.setText("");
                                Toast.makeText(SettingsActivity.this, "Debugging Mode Enabled", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                pref = getApplicationContext().getSharedPreferences("Pref", 0);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putBoolean("Debugger", false);
                                editor.commit();
                                debugger.setChecked(false);
                                Toast.makeText(SettingsActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
                else
                {
                    pref = getApplicationContext().getSharedPreferences("Pref", 0);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("Debugger", false);
                    editor.commit();
                    debugger.setChecked(false);
                    Toast.makeText(SettingsActivity.this, "Debugger Mode Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
