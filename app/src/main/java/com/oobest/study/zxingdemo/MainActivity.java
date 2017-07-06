package com.oobest.study.zxingdemo;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.oobest.study.zxingdemo.model.api.ApiClient;
import com.oobest.study.zxingdemo.model.entity.Timestamp;
import com.oobest.study.zxingdemo.model.util.EntityUtils;
import com.oobest.study.zxingdemo.util.MobileUtils;

import org.joda.time.DateTime;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
                case R.id.navigation_list:
                    setTitle(R.string.title_list);
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
            setTitle(position == 0 ? R.string.title_parcel : R.string.title_list);
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
        adapter.addFragment(ParcelListFragment.newInstance());
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


    @Override
    protected void onResume() {
        super.onResume();

        if (!MobileUtils.isNetworkAvailable(this)) {
            Snackbar.make(mViewPager, "请校连接网络", Toast.LENGTH_LONG).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.finish();
                }
            }, 4000);
            return;
        }

        ApiClient.service.getTimestamp().enqueue(new Callback<Timestamp>() {
            @Override
            public void onResponse(Call<Timestamp> call, Response<Timestamp> response) {
                Timestamp time = response.body();
                if (time != null) {


                    int current = (int) (System.currentTimeMillis() / 1000);
                    // Log.d(TAG, "onResponse: current=" + current);
                    // Log.d(TAG, "onResponse: time.getTimestamp()=" + time.getTimestamp());
                    if (Math.abs(current - time.getTimestamp()) > 60) {
                        Snackbar.make(mViewPager, "请校正手机时间", Toast.LENGTH_LONG).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.finish();
                            }
                        }, 4000);
                    }
                }
            }

            @Override
            public void onFailure(Call<Timestamp> call, Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }
}
