package com.example.velonathea;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

public class ConfigActivity extends AppCompatActivity {

    private static LoadRowsFromFile lrff;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        //Request read permissions if not granted
        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            },1);
        }

        ActivityResultLauncher<Intent> loadRowsActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            lrff = new LoadRowsFromFile(this, uri);
                            lrff.execute();
                        }
                    }
                });

        //Loads db rows from text file
        Button loadButton = findViewById(R.id.activity_config_loadbutton);
        loadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            loadRowsActivity.launch(intent);
        });

        //Shows row count of each table in DB in a toast
        Button debugButton = findViewById(R.id.activity_config_debugbutton);
        debugButton.setOnClickListener(v -> {
            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getWritableDatabase();

            long numImages = DatabaseUtils.queryNumEntries(db, MyOpenHelper.IMAGE_TABLE);
            long numTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.TAG_TABLE);
            long numImageTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.IMAGE_TAG_TABLE);
            Toast.makeText(getApplicationContext(), "image:" + numImages + ".tag:" + numTags + ".imagetags:" + numImageTags, Toast.LENGTH_LONG).show();
        });

        Button deleteButton = findViewById(R.id.activity_config_deletebutton);
        deleteButton.setOnClickListener(v -> {
            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getWritableDatabase();
            db.delete("image_tag", null, null);
            db.delete("image", null, null);
            db.delete("tag", null, null);
        });
    }

    private static class LoadRowsFromFile extends AsyncTask<String, Integer, String> {

        private final Uri uri;
        private final WeakReference<ConfigActivity> actRef;

        LoadRowsFromFile(ConfigActivity context, Uri uri) {
            actRef = new WeakReference<>(context);
            this.uri = uri;
        }

        @Override
        protected String doInBackground(String... strings) {
            String content = "";
            try {
                InputStream in = actRef.get().getContentResolver().openInputStream(uri);
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

            if (!content.equals("")) {
                //Prepare for inserting rows into db
                MyOpenHelper myOpenHelper = new MyOpenHelper(actRef.get().getApplicationContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                ContentValues newRowValues = new ContentValues();

                String[] rows = content.split("\n");
                int numRows = rows.length;
                for (int i=0; i<rows.length; i++) {
                    String[] rowValues = rows[i].split("\t");
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
                    publishProgress((int) (((double) i / (double) numRows) * 1000));
                }
            }
            return "Done";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            ConfigActivity activity = actRef.get();
            if (activity == null || activity.isFinishing()) return;
            ProgressBar pb = activity.findViewById(R.id.activity_config_progressbar);
            pb.setProgress(values[0]);
        }

        protected void onPreExecute() {
            ConfigActivity activity = actRef.get();
            if (activity == null || activity.isFinishing()) return;
            ProgressBar pb = activity.findViewById(R.id.activity_config_progressbar);
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String s) {
            ConfigActivity activity = actRef.get();
            if (activity == null || activity.isFinishing()) return;
            ProgressBar pb = activity.findViewById(R.id.activity_config_progressbar);
            pb.setVisibility(View.INVISIBLE);
        }
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
}