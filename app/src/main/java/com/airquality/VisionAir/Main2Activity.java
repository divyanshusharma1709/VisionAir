package com.airquality.VisionAir;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

public class Main2Activity extends AppCompatActivity implements Fragment1.ClickInterface1 {

    public static ViewPager viewPager;
    public static Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);


        viewPager = findViewById(R.id.viewpager);
        SimpleFragmentAdapter adapter = new SimpleFragmentAdapter(getSupportFragmentManager());

        // Set the adapter onto the view pager
        viewPager.setAdapter(adapter);
//        Fragment1 fragment1 = new Fragment1();
//        fragment1.setClickInterface(this);

        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(viewPager.getCurrentItem()==5){
                    button.setVisibility(View.INVISIBLE);
                }
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1,true);
//                Toast.makeText(getApplicationContext(), viewPager.getCurrentItem() + "",Toast.LENGTH_LONG).show();


            }
        });


    }

    @Override
    public void buttonClicked() {

        viewPager.setCurrentItem(2,true);

        //do your code here
    }

}

