package com.example.velonathea;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.WindowManager;

import java.io.File;

public class FullImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_full_image);

        Bundle dataToPass = getIntent().getExtras();
        String fileName = dataToPass.getString("fileName");

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String path = prefs.getString("path", Environment.DIRECTORY_PICTURES);

        ZoomableImageView image = findViewById(R.id.full_image);
        File f = new File(path + "/" + fileName);
        if (f.exists()) {
            Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
            image.setImageBitmap(bm);
            image.setMaxZoom(8f);
        }
    }
}