package com.airquality.VisionAir;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class Fragment1 extends Fragment {

    public interface ClickInterface1 {
        public void buttonClicked();
    }


    ClickInterface1 clickInterface;

    public void setClickInterface(ClickInterface1 clickInterface){
        this.clickInterface = clickInterface;
    }

    public void swipe() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fragment1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setClickInterface(clickInterface);
//        Button button = view.findViewById(R.id.aa);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                clickInterface.buttonClicked();
//
//            }
//        });

    }


}
