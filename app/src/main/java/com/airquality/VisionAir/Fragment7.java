package com.airquality.VisionAir;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;


public class Fragment7 extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v= inflater.inflate(R.layout.fragment_fragment7, container, false);
        Button button = v.findViewById(R.id.yes);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences pref = getActivity().getSharedPreferences("Pref", 0);
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("Contrib", true);
                editor.commit();
                MyAsyncTask setPrefFlag = new MyAsyncTask(getActivity().getParent(), null, "setPrefFlag", "True", new MyAsyncTask.AsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                        Log.i("SetPrefFlag", output);
                    }
                });
                setPrefFlag.execute();
                Intent intent = new Intent(getActivity(),MainActivity.class);
                startActivity(intent);
            }
        });
        Button noButton = v.findViewById(R.id.no);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences pref = getActivity().getSharedPreferences("Pref", 0);
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("Contrib", false);
                editor.commit();
                Intent intent = new Intent(getActivity(),MainActivity.class);
                startActivity(intent);
            }
        });
        return v;


    }


}
