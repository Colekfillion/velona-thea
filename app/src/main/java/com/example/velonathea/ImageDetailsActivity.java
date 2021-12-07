package com.example.velonathea;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class ImageDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String path = prefs.getString("path", Environment.DIRECTORY_PICTURES);

        MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
        SQLiteDatabase db = myOpenHelper.getWritableDatabase();

        EditText fileNameView = findViewById(R.id.image_details_filename);
        EditText nameView = findViewById(R.id.image_details_name);
        EditText authorView = findViewById(R.id.image_details_author);
        EditText linkView = findViewById(R.id.image_details_link);

        Bundle dataToPass = getIntent().getExtras();
        String fileName = dataToPass.getString("fileName");

        Cursor c = db.rawQuery("SELECT name, author, link FROM image WHERE file_name = ? LIMIT 1;", new String[] { fileName });
        c.moveToFirst();

        String name = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_NAME));
        String author = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_AUTHOR));
        String link = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_LINK));
        fileNameView.setText(fileName);
        authorView.setText(author);
        linkView.setText(link);
        nameView.setText(name);

        c.close();

        Button updateImageDataButton = findViewById(R.id.updateImageDataButton);
        updateImageDataButton.setOnClickListener(v -> {

            String newFileName = fileNameView.getText().toString();
            String newName = nameView.getText().toString();
            String newAuthor = authorView.getText().toString();
            String newLink = linkView.getText().toString();

            //Making sure data has changed to reduce unnecessary updates
            ContentValues cv = new ContentValues();
            boolean hasChanged = false;
            if (!fileName.equals(newFileName)) {
                cv.put(MyOpenHelper.COL_IMAGE_FILENAME, newFileName);
                hasChanged = true;
            }
            if (!name.equals(newName)) {
                cv.put(MyOpenHelper.COL_IMAGE_NAME, newName);
                hasChanged = true;
            }
            if (!author.equals(newAuthor)) {
                cv.put(MyOpenHelper.COL_IMAGE_AUTHOR, newAuthor);
                hasChanged = true;
            }
            if (!link.equals(newLink)) {
                cv.put(MyOpenHelper.COL_IMAGE_LINK, newLink);
                hasChanged = true;
            }

            if (hasChanged) {
                db.update(MyOpenHelper.IMAGE_TABLE, cv, "id = (SELECT id FROM image WHERE file_name = ?)", new String[]{fileName});
                Toast.makeText(getApplicationContext(), "updated", Toast.LENGTH_LONG).show();
                db.close();
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "no change", Toast.LENGTH_LONG).show();
            }
        });
    }
}