package com.airquality.VisionAir;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class SimpleFragmentAdapter extends FragmentPagerAdapter {
    public SimpleFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new Fragment1();
        } else if (position == 1){
            return new Fragment2();
        } else if (position == 2) {
            return new Fragment3();
        }
//        else if(position == 3) {
//            return new Fragment4();
//        }
        else {
            return new Fragment4();
        }
    }

    @Override
    public int getCount() {
        return 4;
    }
}
