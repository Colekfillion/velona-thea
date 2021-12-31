package ca.quadrexium.velonathea.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Constants;
import ca.quadrexium.velonathea.pojo.Media;

public class DatabaseConfigActivity extends BaseActivity {

    private static boolean busy = false;

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
        createToolbar(R.id.activity_tb_default_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //Loads db rows from text file
        Button loadButton = findViewById(R.id.activity_database_config_btn_import_media_file);
        loadButton.setOnClickListener(v -> {
            if (!busy) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                loadRowsActivity.launch(intent);
            }
        });

        Button importUnsorted = findViewById(R.id.activity_database_config_btn_import_media);
        importUnsorted.setOnClickListener(v -> {
            if (!busy) {
                busy = true;
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
                String path = prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES);
                LoadMediaFromRoot lmfr = new LoadMediaFromRoot(this);
                lmfr.execute(path);
            }
        });

        //Deletes all data from the database
        Button deleteButton = findViewById(R.id.activity_database_config_btn_clear_database);
        deleteButton.setOnClickListener(v -> {
            if (!busy) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                alertDialogBuilder.setTitle(R.string.button_delete)

                        .setMessage("Are you sure? ")

                        .setPositiveButton("Yes", (click, arg) -> {
                            MyOpenHelper myOpenHelper = openMediaDatabase();
                            SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                            db.delete(MyOpenHelper.MEDIA_TABLE, null, null);
                            db.delete(MyOpenHelper.TAG_TABLE, null, null);
                            db.delete(MyOpenHelper.MEDIA_TAG_TABLE, null, null);
                            db.delete(MyOpenHelper.AUTHOR_TABLE, null, null);
                        })

                        .setNegativeButton("No", (click, arg) -> { })

                        .create().show();
            }
        });

        //Shows row count of each table in DB in a toast
        Button debugButton = findViewById(R.id.activity_database_config_btn_debug_database);
        debugButton.setOnClickListener(v -> {
            if (!busy) {
                busy = true;
                DatabaseDebugTask ddt = new DatabaseDebugTask(findViewById(R.id.activity_database_config_tv_dbstats));
                ddt.execute();
            }
        });
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_database_config;
    }

    private static class LoadRowsFromFile extends AsyncTask<String, Integer, String> {

        private final Uri uri;
        private final WeakReference<DatabaseConfigActivity> actRef;

        LoadRowsFromFile(DatabaseConfigActivity context, Uri uri) {
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
                            .id(-1) //temp value
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

                            int tagId = myOpenHelper.getTagIdOrInsert(db, tag);

                            //Insert image-tag pair into image_tag table
                            newRowValues.put(MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID, imageId);
                            newRowValues.put(MyOpenHelper.COL_MEDIA_TAG_TAG_ID, tagId);
                            db.insert(MyOpenHelper.MEDIA_TAG_TABLE, null, newRowValues);
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
            DatabaseConfigActivity activity = actRef.get();
            if (activity == null || activity.isFinishing()) return;
            ProgressBar pb = activity.findViewById(R.id.activity_database_config_pb_loading);
            pb.setProgress(values[0]);
        }

        protected void onPreExecute() {
            DatabaseConfigActivity activity = actRef.get();
            if (activity == null || activity.isFinishing()) return;
            ProgressBar pb = activity.findViewById(R.id.activity_database_config_pb_loading);
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String s) {
            DatabaseConfigActivity activity = actRef.get();
            if (activity == null || activity.isFinishing()) return;
            ProgressBar pb = activity.findViewById(R.id.activity_database_config_pb_loading);
            pb.setVisibility(View.INVISIBLE);
            busy = false;
        }
    }

    private static class LoadMediaFromRoot extends AsyncTask<String, Integer, Integer> {

        private final WeakReference<DatabaseConfigActivity> actRef;

        LoadMediaFromRoot(DatabaseConfigActivity context) {
            actRef = new WeakReference<>(context);
        }

        @Override
        protected Integer doInBackground(String... strings) {
            String path = strings[0];

            File rootPath = new File(path);
            File[] files = rootPath.listFiles(File::isFile);
            HashSet<String> fileNames = new HashSet<>();
            int count = 0;
            if (files != null) {
                for (File file : files) {
                    fileNames.add(file.getName());
                }
                HashSet<String> dbFileNames = new HashSet<>();
                MyOpenHelper myOpenHelper = new MyOpenHelper(actRef.get().getApplicationContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                Cursor c = db.rawQuery("SELECT " + MyOpenHelper.COL_MEDIA_FILENAME + " " +
                        "FROM " + MyOpenHelper.MEDIA_TABLE, null);
                c.moveToFirst();

                while (!c.isAfterLast()) {
                    dbFileNames.add(c.getString(c.getColumnIndex(MyOpenHelper.COL_MEDIA_FILENAME)));
                    c.moveToNext();
                }
                c.close();
                fileNames.removeAll(dbFileNames);
                db.beginTransaction();
                for (String query : MyOpenHelper.DROP_INDEX_QUERIES) {
                    db.execSQL(query);
                }
                for (String fileName : fileNames) {
                    //If media is valid type
                    String extension = fileName.substring(fileName.lastIndexOf("."));
                    if (Constants.VIDEO_EXTENSIONS.contains(extension) ||
                            Constants.IMAGE_EXTENSIONS.contains(extension) ||
                            extension.equals(".gif")) {

                        Media media = new Media.Builder()
                                .id(-1) //temp value
                                .name(fileName.substring(0, fileName.lastIndexOf(".")))
                                .fileName(fileName)
                                .author("unknown")
                                .build();
                        media.setId(myOpenHelper.insertMedia(db, media));
                        count++;
                        publishProgress((int) (((double) count / (double) fileNames.size()) * 1000));
                    }
                }
                for (String query : MyOpenHelper.CREATE_INDEX_QUERIES) {
                    db.execSQL(query);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();
            }
            publishProgress(1000);
            return count;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            DatabaseConfigActivity activity = actRef.get();
            if (activity == null || activity.isFinishing()) return;
            ProgressBar pb = activity.findViewById(R.id.activity_database_config_pb_loading);
            pb.setProgress(values[0]);
        }

        protected void onPreExecute() {
            DatabaseConfigActivity activity = actRef.get();
            if (activity == null || activity.isFinishing()) return;
            ProgressBar pb = activity.findViewById(R.id.activity_database_config_pb_loading);
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Integer count) {
            DatabaseConfigActivity activity = actRef.get();
            if (activity == null || activity.isFinishing()) return;
            ProgressBar pb = activity.findViewById(R.id.activity_database_config_pb_loading);
            pb.setVisibility(View.INVISIBLE);
            busy = false;
            Toast.makeText(activity.getApplicationContext(), "imported " + count + " files",
                    Toast.LENGTH_LONG).show();
        }
    }

    private class DatabaseDebugTask extends AsyncTask<Void, String, Integer> {
        
        TextView tvDbStats;
        StringBuilder dbStats;
        SQLiteDatabase db;

        DatabaseDebugTask(TextView tvDbStats) {
            this.tvDbStats = tvDbStats;
            dbStats = new StringBuilder();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MyOpenHelper myOpenHelper = openMediaDatabase();
            db = myOpenHelper.getReadableDatabase();
            tvDbStats.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            
            long numMedia = DatabaseUtils.queryNumEntries(db, MyOpenHelper.MEDIA_TABLE);
            long numTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.TAG_TABLE);
            long numMediaTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.MEDIA_TAG_TABLE);
            long numAuthors = DatabaseUtils.queryNumEntries(db, MyOpenHelper.AUTHOR_TABLE);

            publishProgress("Media: " + numMedia + "\n");
            publishProgress("Tags: " + numTags + "\n");
            publishProgress("Authors: " + numAuthors + "\n");
            publishProgress("Media-tag relationships: " + numMediaTags + "\n");

            String sql = "SELECT COUNT(*) AS count FROM (" +
                    "SELECT " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_ID + " " +
                    "FROM " + MyOpenHelper.MEDIA_TABLE + " " +
                    "INTERSECT " +
                    "SELECT " + MyOpenHelper.MEDIA_TAG_TABLE + "." + MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID + " " +
                    "FROM " + MyOpenHelper.MEDIA_TAG_TABLE +
                    ") A";

            long numMediaWithTags = 0;
            Cursor c = db.rawQuery(sql, null);
            if (c.moveToFirst()) {
                numMediaWithTags = c.getLong(c.getColumnIndex("count"));
            }
            c.close();

            publishProgress("Media with tags: " + numMediaWithTags + "\n");
            publishProgress("Media without tags: " + (numMedia - numMediaWithTags) + "\n");

            sql = "SELECT " + MyOpenHelper.AUTHOR_TABLE + "." + MyOpenHelper.COL_AUTHOR_NAME + ", " +
                    "COUNT(" + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_AUTHOR_ID + ") AS count " +
                    "FROM " + MyOpenHelper.MEDIA_TABLE + " " +
                    "JOIN " + MyOpenHelper.AUTHOR_TABLE + " ON " +
                    MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_AUTHOR_ID + " = " +
                    MyOpenHelper.AUTHOR_TABLE + "." + MyOpenHelper.COL_AUTHOR_ID + " " +
                    "GROUP BY " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_AUTHOR_ID + " " +
                    "ORDER BY count DESC";


            c = db.rawQuery(sql, null);
            int numAuthorsToReturn = Math.min(3, c.getCount());
            if (c.moveToFirst()) {
                int nameColIndex = c.getColumnIndex(MyOpenHelper.COL_AUTHOR_NAME);
                int countColIndex = c.getColumnIndex("count");
                publishProgress("Most common author(s): \n");
                while (!c.isAfterLast() && numAuthorsToReturn != 0) {
                    publishProgress("\t" + c.getString(nameColIndex) + " (" + c.getLong(countColIndex) + " media)\n");
                    --numAuthorsToReturn;
                    c.moveToNext();
                }
            }
            c.close();
            return 1;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            dbStats.append(values[0]);
            tvDbStats.setText(dbStats);
        }
        
        @Override
        protected void onPostExecute(Integer i) {
            tvDbStats.setText(dbStats);
            db.close();
            busy = false;
        }
    }

    //No options menu
    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        return true;
    }

    //Back button in toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}