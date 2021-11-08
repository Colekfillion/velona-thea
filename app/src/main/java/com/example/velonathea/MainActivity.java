package com.example.velonathea;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Request read permissions if not granted
        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            },1);
        }

        EditText searchBar = findViewById(R.id.activity_main_searchbar);

        Button tagButton = findViewById(R.id.activity_main_searchtagbutton);
        tagButton.setOnClickListener(v -> searchIntent("tag", searchBar.getText().toString()));
        Button authorButton = findViewById(R.id.activity_main_searchauthorbutton);
        authorButton.setOnClickListener(v -> searchIntent("author", searchBar.getText().toString()));
        Button titleButton = findViewById(R.id.activity_main_searchtitlebutton);
        titleButton.setOnClickListener(v -> searchIntent("title", searchBar.getText().toString()));

        //Loads db rows from text file
        Button loadButton = findViewById(R.id.activity_main_loadbutton);
        loadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, 1);
        });

        //Shows row count of each table in DB in a toast
        Button debugButton = findViewById(R.id.activity_main_debugbutton);
        debugButton.setOnClickListener(v -> {
            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getWritableDatabase();

            long numImages = DatabaseUtils.queryNumEntries(db, MyOpenHelper.IMAGE_TABLE);
            long numTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.TAG_TABLE);
            long numImageTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.IMAGE_TAG_TABLE);
            Toast.makeText(getApplicationContext(), "image:" + numImages + ".tag:" + numTags + ".imagetags:" + numImageTags, Toast.LENGTH_LONG).show();
        });
    }

    //Given search mode and text, go to SearchResultsActivity
    private void searchIntent(String searchMode, String searchFor) {
        Bundle dataToPass = new Bundle();
        Intent i = new Intent(this, SearchResultsActivity.class);
        dataToPass.putString("searchMode", searchMode);
        dataToPass.putString("searchFor", "%" + searchFor.toLowerCase() + "%");
        i.putExtras(dataToPass);
        startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0]!=PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
        case 1:
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                parseFile(uri);
            }
            break;
        }
    }

    //Given a Uri to a text file, parse it into db rows
    private void parseFile(Uri uri) {

        //Get file contents as string
        String content = "";
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }
            //content contains the text in output.txt.
            content = total.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (content.equals("")) {
            //Prepare for inserting rows into db
            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getWritableDatabase();
            ContentValues newRowValues = new ContentValues();

            String[] rows = content.split("\n");
            for (String row : rows) {
                String[] rowValues = row.split("\t");
                String fileName = rowValues[0];
                String name = rowValues[1];
                String author = rowValues[2];
                String link = "";
                try {
                    link = rowValues[3];
                } catch (ArrayIndexOutOfBoundsException e) {
                    //no link to source, do nothing
                }
                String tagsList = "";
                try {
                    tagsList = rowValues[4];
                } catch (ArrayIndexOutOfBoundsException e) {
                    //no tags, do nothing
                }

                //Insert new row into database
                newRowValues.put(MyOpenHelper.COL_IMAGE_FILENAME, fileName);
                newRowValues.put(MyOpenHelper.COL_IMAGE_NAME, name);
                newRowValues.put(MyOpenHelper.COL_IMAGE_AUTHOR, author);
                newRowValues.put(MyOpenHelper.COL_IMAGE_LINK, link);
                int imageId = (int) db.insert(MyOpenHelper.IMAGE_TABLE, null, newRowValues);
                newRowValues.clear();

                //If there are tags
                if (!tagsList.equals("")) {
                    String[] tags = tagsList.split(" ");
                    for (String tag : tags) {

                        //Check if tag exists
                        Cursor c = db.rawQuery("SELECT id, name FROM tag WHERE name = ? LIMIT 1;", new String[]{tag});
                        int tagId;
                        //If tag does not exist, insert tag into database
                        if (c.getCount() == 0) {
                            newRowValues.put(MyOpenHelper.COL_TAG_NAME, tag);
                            tagId = (int) db.insert(MyOpenHelper.TAG_TABLE, null, newRowValues);
                            newRowValues.clear();
                        } else {
                            c.moveToFirst();
                            tagId = (int) c.getLong(c.getColumnIndex("id"));
                        }
                        c.close();

                        //Insert image-tag pair into image_tag table
                        newRowValues.put(MyOpenHelper.COL_IMAGE_TAG_IMAGE_ID, imageId);
                        newRowValues.put(MyOpenHelper.COL_IMAGE_TAG_TAG_ID, tagId);
                        db.insert(MyOpenHelper.IMAGE_TAG_TABLE, null, newRowValues);
                        newRowValues.clear();
                    }
                }
            }
        }
    }
}