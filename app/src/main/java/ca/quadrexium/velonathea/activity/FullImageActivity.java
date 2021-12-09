package ca.quadrexium.velonathea.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.view.ZoomableImageView;

public class FullImageActivity extends BaseActivity {

    String path;
    ZoomableImageView image;
    private View.OnTouchListener iteratorOtl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_full_image_tb);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        Bundle data = getIntent().getExtras();
        ArrayList<String> fileNames = data.getStringArrayList("fileNames");
        AtomicInteger position = new AtomicInteger(data.getInt("position"));

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        path = prefs.getString("path", Environment.DIRECTORY_PICTURES);

        image = findViewById(R.id.activity_full_image_zimgv_image);
        image.setMaxZoom(8f);
        loadImage(fileNames.get(position.get()));

        iteratorOtl = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
                float x = event.getX();
                if (x / screenWidth > 0.8 && position.get() < fileNames.size()-1) {
                    position.getAndIncrement();
                } else if (x / screenWidth < 0.2 && position.get() > 0) {
                    position.getAndDecrement();
                }
                loadImage(fileNames.get(position.get()));
            }
            return true;
        };
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_full_image;
    }

    private void loadImage(String fileName) {
        File f = new File(path + "/" + fileName);
        Bitmap bm = null;
        if (f.exists()) {
            try {
                bm = BitmapFactory.decodeFile(f.getAbsolutePath());
            } catch (RuntimeException e) {
                Toast.makeText(getApplicationContext(), "Cannot draw " + fileName + ", image too large", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "File " + fileName + " not found", Toast.LENGTH_SHORT).show();
        }
        try {
            image.setImageBitmap(bm);
        } catch (NullPointerException e) {
            //TODO: Find a template image bitmap, like a loading icon, and set it here
        }
    }

    public boolean onCreateOptionsMenu(Menu m) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_full_image_toolbar, m);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_togglezoom) {
            boolean isChecked = item.isChecked();
            if (isChecked) {
                image.setOnTouchListener(ZoomableImageView.otl);
            } else {
                image.setOnTouchListener(iteratorOtl);
            }
            item.setChecked(!isChecked);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}