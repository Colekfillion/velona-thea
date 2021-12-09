package ca.quadrexium.velonathea.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SwitchCompat;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Media;

public class ConfigActivity extends BaseActivity {

    private static boolean busy = false;

    private final ActivityResultLauncher<Intent> chooseDirActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putString("path", data.getStringExtra("path"));
                        edit.apply();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> loadRowsActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        busy = true;
                        Uri uri = data.getData();
                        LoadRowsFromFile lrff = new LoadRowsFromFile(this, uri);
                        lrff.execute();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Loads db rows from text file
        Button loadButton = findViewById(R.id.activity_config_loadbutton);
        loadButton.setOnClickListener(v -> {
            if (!busy) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                loadRowsActivity.launch(intent);
            }
        });

        //Deletes all data from the database
        Button deleteButton = findViewById(R.id.activity_config_deletebutton);
        deleteButton.setOnClickListener(v -> {
            if (!busy) {
                MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                db.delete(MyOpenHelper.IMAGE_TABLE, null, null);
                db.delete(MyOpenHelper.TAG_TABLE, null, null);
                db.delete(MyOpenHelper.IMAGE_TAG_TABLE, null, null);
                db.delete(MyOpenHelper.AUTHOR_TABLE, null, null);
            }
        });

        //Shows row count of each table in DB in a toast
        Button debugButton = findViewById(R.id.activity_config_debugbutton);
        debugButton.setOnClickListener(v -> {
            if (!busy) {
                MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                SQLiteDatabase db = myOpenHelper.getReadableDatabase();

                long numImages = DatabaseUtils.queryNumEntries(db, MyOpenHelper.IMAGE_TABLE);
                long numTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.TAG_TABLE);
                long numImageTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.IMAGE_TAG_TABLE);
                long numAuthors = DatabaseUtils.queryNumEntries(db, MyOpenHelper.AUTHOR_TABLE);
                Toast.makeText(getApplicationContext(), "image:" + numImages +
                                                            ".tag:" + numTags +
                                                            ".imagetags:" + numImageTags +
                                                            ".author:" + numAuthors,
                        Toast.LENGTH_LONG).show();
            }
        });

        //Launches the chooseDirActivity
        Button chooseDirButton = findViewById(R.id.activity_config_choosedirbutton);
        chooseDirButton.setOnClickListener(v -> {
            Intent i = new Intent(this, ChooseDirActivity.class);
            chooseDirActivity.launch(i);
        });

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);

        EditText imageCacheSize = findViewById(R.id.activity_config_maxcachesize);
        imageCacheSize.setText(String.valueOf(prefs.getInt("maxCacheSize", 20)));

        SwitchCompat showHiddenFiles = findViewById(R.id.show_hidden_files_switch);
        showHiddenFiles.setChecked(prefs.getBoolean("showHiddenFiles", false));
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_config;
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
                db.beginTransaction();
                //Dropping indexes for performance
                for (String query : MyOpenHelper.DROP_INDEX_QUERIES) {
                    db.execSQL(query);
                }
                ContentValues newRowValues = new ContentValues();

                String[] rows = content.split("\n");
                int numRows = rows.length;
                for (int i=0; i<rows.length; i++) {
                    String[] rowValues = rows[i].split("\t");
                    Media media = new Media.Builder()
                            .id(-1)
                            .fileName(rowValues[0])
                            .name(rowValues[1])
                            .author(rowValues[2])
                            .build();
                    try {
                        media.setLink(rowValues[3]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        //no link to source, do nothing
                    }
                    String tagsList = "";
                    try {
                        tagsList = rowValues[4];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        //no tags, do nothing
                    }

                    int imageId = myOpenHelper.insertMedia(db, media);

                    //If there are tags
                    if (!tagsList.equals("")) {

                        String[] tagsArray = tagsList.split(" ");
                        Set<String> tags = new HashSet<>(Arrays.asList(tagsArray));
                        for (String tag : tags) {

                            int tagId = myOpenHelper.getTagId(db, tag);

                            //Insert image-tag pair into image_tag table
                            newRowValues.put(MyOpenHelper.COL_IMAGE_TAG_IMAGE_ID, imageId);
                            newRowValues.put(MyOpenHelper.COL_IMAGE_TAG_TAG_ID, tagId);
                            db.insert(MyOpenHelper.IMAGE_TAG_TABLE, null, newRowValues);
                            newRowValues.clear();
                        }
                    }
                    publishProgress((int) (((double) i / (double) numRows) * 1000));
                }
                //Recreating the indexes
                for (String query : MyOpenHelper.CREATE_INDEX_QUERIES) {
                    db.execSQL(query);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
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
            busy = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        EditText imageCacheSize = (EditText)findViewById(R.id.activity_config_maxcachesize);
        edit.putInt("maxCacheSize", Integer.parseInt(imageCacheSize.getText().toString()));

        SwitchCompat showHiddenFiles = findViewById(R.id.show_hidden_files_switch);
        edit.putBoolean("showHiddenFiles", showHiddenFiles.isChecked());
        edit.apply();
    }
}