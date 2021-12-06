package com.example.velonathea;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class ImageDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
        SQLiteDatabase db = myOpenHelper.getReadableDatabase();

        TextView fileNameView = findViewById(R.id.image_details_filename);
        TextView nameView = findViewById(R.id.image_details_name);
        TextView authorView = findViewById(R.id.image_details_author);
        TextView linkView = findViewById(R.id.image_details_link);

        Bundle dataToPass = getIntent().getExtras();
        String fileName = dataToPass.getString("fileName");

        Cursor c = db.rawQuery("SELECT name, author, link FROM image WHERE file_name = ? LIMIT 1;", new String[] { fileName });
        long dbStartTime = SystemClock.elapsedRealtime();
        c.moveToFirst();
        long dbTimeToExecute = SystemClock.elapsedRealtime() - dbStartTime;
        Toast.makeText(getApplicationContext(), "time to execute: " + dbTimeToExecute + "ms", Toast.LENGTH_LONG).show();

        String name = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_NAME));
        String author = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_AUTHOR));
        String link = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_LINK));
        fileNameView.setText(fileName);
        authorView.setText(author);
        linkView.setText(link);
        nameView.setText(name);

        c.close();
        db.close();
    }
}