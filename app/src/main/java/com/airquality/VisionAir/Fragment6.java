package com.airquality.VisionAir;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;


public class Fragment6 extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v= inflater.inflate(R.layout.fragment_fragment6, container, false);
        return v;


    }

    @Override
    public void onResume() {
        super.onResume();
        Main2Activity.button.setVisibility(View.VISIBLE);
    }

}
