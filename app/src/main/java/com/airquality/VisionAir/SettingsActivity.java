package com.airquality.VisionAir;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

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
                }
                else
                {
                    pref = getApplicationContext().getSharedPreferences("Pref", 0);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("Contrib", false);
                    editor.commit();
                }
            }
        });
    }
}
