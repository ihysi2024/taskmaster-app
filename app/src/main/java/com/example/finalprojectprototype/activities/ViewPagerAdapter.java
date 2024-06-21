package com.example.finalprojectprototype.activities;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.finalprojectprototype.calendar.CalenderFragment;
import com.example.finalprojectprototype.tasks.TasksFragment;
import com.example.finalprojectprototype.tracker.TrackerFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    public ViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        if (position == 1) {
            fragment = new CalenderFragment();
        }
        else if (position == 0) {
            fragment = new TasksFragment();
        }
        else if (position == 2) {
            fragment = new TrackerFragment();
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    public CharSequence getPageTitle(int position) {
        String title = null;
        if (position == 0) {
            title = "Tasks";
        }
        else if (position == 1) {
            title = "Calendar";
        }
        else if (position == 2) {
            title = "Tracker";
        }
        return title;
    }
}
