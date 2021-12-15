package ca.quadrexium.velonathea.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.ViewPagerAdapter;

public class FullMediaActivity extends BaseActivity {

    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        ArrayList<String> fileNames = data.getStringArrayList("fileNames");

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String path = prefs.getString("path", Environment.DIRECTORY_PICTURES);

        viewPager = findViewById(R.id.activity_full_media_vp);
        viewPagerAdapter = new ViewPagerAdapter(FullMediaActivity.this, fileNames, path);
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(data.getInt("position"));
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_full_media; }
}