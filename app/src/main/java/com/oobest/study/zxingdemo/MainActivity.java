package com.oobest.study.zxingdemo;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ViewPager mViewPager;

    private MenuItem mMenuItem;

    private BottomNavigationView mNavigation;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_parcel:
                    setTitle(R.string.title_parcel);
                    mViewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_dashboard:
                    setTitle(R.string.title_dashboard);
                    mViewPager.setCurrentItem(1);
                    return true;
            }
            return false;
        }
    };

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (mMenuItem != null) {
                mMenuItem.setChecked(false);
            } else {
                mNavigation.getMenu().getItem(0).setChecked(false);
            }
            setTitle(position == 0 ? R.string.title_parcel : R.string.title_dashboard);
            mNavigation.getMenu().getItem(position).setChecked(true);
            mMenuItem = mNavigation.getMenu().getItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(R.string.title_parcel);

        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mNavigation = (BottomNavigationView) findViewById(R.id.navigation);
        mNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(MailParcelFragment.newInstance(getString(R.string.title_parcel)));
        adapter.addFragment(DashboardFragment.newInstance());
        mViewPager.setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.app_bar_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return super.onCreateOptionsMenu(menu);
    }


}
