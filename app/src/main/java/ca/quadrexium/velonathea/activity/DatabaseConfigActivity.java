package ca.quadrexium.velonathea.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Constants;
import ca.quadrexium.velonathea.pojo.Media;

public class DatabaseConfigActivity extends BaseActivity {

    private static boolean busy = false;

    //Loads rows from selected text file
    private final ActivityResultLauncher<Intent> loadRowsActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        busy = true;
                        Uri uri = data.getData();

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Handler handler = new Handler(Looper.getMainLooper());

                        executor.execute(() -> {
                            String content = "";
                            ProgressBar pb = findViewById(R.id.activity_database_config_pb_loading);
                            handler.post(() -> pb.setVisibility(View.VISIBLE));
                            try {
                                InputStream in = this.getContentResolver().openInputStream(uri);
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
                                MyOpenHelper myOpenHelper = openMediaDatabase();
                                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                                db.beginTransaction();
                                //Dropping indexes for performance
                                for (String query : MyOpenHelper.DROP_INDEX_QUERIES) {
                                    db.execSQL(query);
                                }
                                ContentValues newRowValues = new ContentValues();

                                String[] rows = content.split("\n");
                                int numRows = rows.length;
                                for (int i = 0; i < rows.length; i++) {
                                    String[] rowValues = rows[i].split("\t");
                                    Media media = new Media.Builder()
                                            .id(-1) //temp value
                                            .fileName(rowValues[0])
                                            .name(rowValues[1])
                                            .author(Constants.isStringEmpty(rowValues[2]) ? "unknown" : rowValues[2])
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
                                    int finalI = i;
                                    handler.post(() -> pb.setProgress((int) (((double) finalI / (double) numRows) * 1000)));
                                }
                                //Recreating the indexes
                                for (String query : MyOpenHelper.CREATE_INDEX_QUERIES) {
                                    db.execSQL(query);
                                }
                                db.setTransactionSuccessful();
                                db.endTransaction();
                                handler.post(() -> {
                                    busy = false;
                                    pb.setVisibility(View.GONE);
                                    Toast.makeText(this, "Loaded " + numRows + " media", Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //Loads media from text file
        Button btnDbImportFile = findViewById(R.id.activity_database_config_btn_dbimportfile);
        btnDbImportFile.setOnClickListener(v -> {
            if (!busy) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/plain");
                loadRowsActivity.launch(intent);
            }
        });

        //Import media from root directory
        Button btnDbImportRaw = findViewById(R.id.activity_database_config_btn_dbimportraw);
        btnDbImportRaw.setOnClickListener(v -> {
            if (!busy) {
                busy = true;
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
                String path = prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    ProgressBar pb = this.findViewById(R.id.activity_database_config_pb_loading);
                    handler.post(() -> pb.setVisibility(View.VISIBLE));

                    //Getting file names in rootDir
                    File rootDir = new File(path);
                    String[] fileNamesArray = rootDir.list((dir, name) -> {
                        int extensionIndex = name.lastIndexOf(".");
                        if (extensionIndex == -1) { return false; }
                        String extension = name.substring(extensionIndex);
                        return Constants.VIDEO_EXTENSIONS.contains(extension) ||
                                Constants.IMAGE_EXTENSIONS.contains(extension) ||
                                extension.equals(".gif");
                    });

                    Set<String> fileNames;
                    int count = 0;
                    if (fileNamesArray != null) {
                        //Getting all unique file names in rootDir
                        fileNames = new HashSet<>(Arrays.asList(fileNamesArray));
                        MyOpenHelper myOpenHelper = openMediaDatabase();
                        SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                        Cursor c = db.rawQuery("SELECT " + MyOpenHelper.COL_MEDIA_FILENAME + " " +
                                "FROM " + MyOpenHelper.MEDIA_TABLE, null);
                        c.moveToFirst();
                        int columnIndex = c.getColumnIndex(MyOpenHelper.COL_MEDIA_FILENAME);
                        while (!c.isAfterLast()) {
                            fileNames.remove(c.getString(columnIndex));
                            c.moveToNext();
                        }
                        c.close();

                        //Loading the media into memory
                        Set<Media> mediaList = new HashSet<>();
                        for (String fileName : fileNames) {
                            mediaList.add(new Media.Builder()
                                    .id(-1) //temp value
                                    .name(fileName.substring(0, fileName.lastIndexOf(".")))
                                    .fileName(fileName)
                                    .author("unknown")
                                    .build());
                            //Progress
                            count++;
                            int finalCount = count;
                            handler.post(() -> pb.setProgress((int) (((double) finalCount / (double) fileNames.size()) * 500)));
                        }

                        //Inserting mediaList media into database
                        db.beginTransaction();
                        for (String query : MyOpenHelper.DROP_INDEX_QUERIES) {
                            db.execSQL(query);
                        }
                        count = 0;
                        for (Media media : mediaList) {
                            //Check that the media name is the filename without the extension
                            String fileName = media.getFileName();
                            if (!fileName.substring(0, fileName.lastIndexOf(".")).equals(media.getName())) {
                                Toast.makeText(this, "Error: " + fileName + " != " + media.getName(), Toast.LENGTH_LONG).show();
                            }
                            if (!media.getAuthor().equals("unknown")) {
                                Toast.makeText(this, "Error: " + media.getAuthor() + " != unknown", Toast.LENGTH_LONG).show();
                            }

                            //Insert
                            myOpenHelper.insertMedia(db, media);
                            //Progress
                            count++;
                            int finalCount = count;
                            handler.post(() -> pb.setProgress(((int) (((double) finalCount / (double) fileNames.size()) * 500)+500)));
                        }
                        for (String query : MyOpenHelper.CREATE_INDEX_QUERIES) {
                            db.execSQL(query);
                        }
                        db.setTransactionSuccessful();
                        db.endTransaction();
                        db.close();
                    }
                    int finalCount1 = count;
                    handler.post(() -> {
                        busy = false;
                        pb.setVisibility(View.GONE);
                        Toast.makeText(this, "Loaded " + finalCount1 + " media", Toast.LENGTH_SHORT).show();
                    });
                });
            }
        });

        //Delete all data from the database
        Button btnClearDb = findViewById(R.id.activity_database_config_btn_cleardb);
        btnClearDb.setOnClickListener(v -> {
            if (!busy) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                alertDialogBuilder.setTitle(R.string.button_delete)

                        .setMessage(R.string.are_you_sure)

                        .setPositiveButton(R.string.yes, (click, arg) -> {
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            Handler handler = new Handler(Looper.getMainLooper());

                            executor.execute(() -> {
                                ProgressBar pb = this.findViewById(R.id.activity_database_config_pb_loading);
                                handler.post(() -> pb.setVisibility(View.VISIBLE));
                                busy = true;
                                MyOpenHelper myOpenHelper = openMediaDatabase();
                                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                                db.delete(MyOpenHelper.MEDIA_TABLE, null, null);
                                handler.post(() -> pb.setProgress(250));
                                db.delete(MyOpenHelper.TAG_TABLE, null, null);
                                handler.post(() -> pb.setProgress(500));
                                db.delete(MyOpenHelper.MEDIA_TAG_TABLE, null, null);
                                handler.post(() -> pb.setProgress(750));
                                db.delete(MyOpenHelper.AUTHOR_TABLE, null, null);
                                handler.post(() -> {
                                    handler.post(() -> pb.setProgress(1000));
                                    busy = false;
                                    pb.setVisibility(View.GONE);
                                    Toast.makeText(this, "Cleared database", Toast.LENGTH_SHORT).show();
                                });
                                busy = false;
                            });
                        })

                        .setNegativeButton(R.string.no, (click, arg) -> { })

                        .create().show();
            }
        });

        //Show various database statistics
        //TODO: Have additional info be shown in a popup, ex. button that says "Show duplicate files"
        Button btnDebugDb = findViewById(R.id.activity_database_config_btn_debugdb);
        btnDebugDb.setOnClickListener(v -> {
            if (!busy) {
                busy = true;
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    TextView tvDbStats = findViewById(R.id.activity_database_config_tv_dbstats);
                    tvDbStats.setMovementMethod(new ScrollingMovementMethod());
                    handler.post(() -> tvDbStats.setVisibility(View.VISIBLE));

                    StringBuilder dbStats = new StringBuilder();
                    MyOpenHelper myOpenHelper = openMediaDatabase();
                    SQLiteDatabase db = myOpenHelper.getReadableDatabase();

                    long numMedia = DatabaseUtils.queryNumEntries(db, MyOpenHelper.MEDIA_TABLE);
                    long numTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.TAG_TABLE);
                    long numMediaTags = DatabaseUtils.queryNumEntries(db, MyOpenHelper.MEDIA_TAG_TABLE);
                    long numAuthors = DatabaseUtils.queryNumEntries(db, MyOpenHelper.AUTHOR_TABLE);

                    dbStats.append("Media: ").append(numMedia).append("\n");
                    dbStats.append("Tags: ").append(numTags).append("\n");
                    dbStats.append("Authors: ").append(numAuthors).append("\n");
                    dbStats.append("Media-tag relationships: ").append(numMediaTags).append("\n");
                    handler.post(() -> tvDbStats.setText(dbStats.toString()));

                    //Media with/without tags
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

                    dbStats.append("Media with tags: ").append(numMediaWithTags).append("\n");
                    dbStats.append("Media without tags: ").append(numMedia - numMediaWithTags).append("\n");
                    handler.post(() -> tvDbStats.setText(dbStats.toString()));

                    //Most common tags
                    sql = "SELECT " + MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_NAME + ", " +
                            "COUNT(" + MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_NAME + ") AS count " +
                            "FROM " + MyOpenHelper.TAG_TABLE + " " +
                            "JOIN " + MyOpenHelper.MEDIA_TAG_TABLE + " ON " +
                            MyOpenHelper.MEDIA_TAG_TABLE + "." + MyOpenHelper.COL_MEDIA_TAG_TAG_ID + " = " +
                            MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_ID + " " +
                            "GROUP BY " + MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_NAME + " " +
                            "ORDER BY count DESC";

                    c = db.rawQuery(sql, null);
                    if (c.moveToFirst()) {
                        int numTagsToReturn = Math.min(10, c.getCount());
                        int nameColIndex = c.getColumnIndex(MyOpenHelper.COL_TAG_NAME);
                        int countColIndex = c.getColumnIndex("count");
                        dbStats.append("Most common tag(s): \n");
                        while (!c.isAfterLast() && numTagsToReturn != 0) {
                            dbStats.append("\t").append(c.getString(nameColIndex)).append(" (").append(c.getLong(countColIndex)).append(")\n");
                            --numTagsToReturn;
                            c.moveToNext();
                        }
                        handler.post(() -> tvDbStats.setText(dbStats.toString()));
                    }
                    c.close();

                    //Most common authors
                    sql = "SELECT " + MyOpenHelper.AUTHOR_TABLE + "." + MyOpenHelper.COL_AUTHOR_NAME + ", " +
                            "COUNT(" + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_AUTHOR_ID + ") AS count " +
                            "FROM " + MyOpenHelper.MEDIA_TABLE + " " +
                            "JOIN " + MyOpenHelper.AUTHOR_TABLE + " ON " +
                            MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_AUTHOR_ID + " = " +
                            MyOpenHelper.AUTHOR_TABLE + "." + MyOpenHelper.COL_AUTHOR_ID + " " +
                            "GROUP BY " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_AUTHOR_ID + " " +
                            "ORDER BY count DESC";

                    c = db.rawQuery(sql, null);
                    int numAuthorsToReturn = Math.min(5, c.getCount());
                    if (c.moveToFirst()) {
                        int nameColIndex = c.getColumnIndex(MyOpenHelper.COL_AUTHOR_NAME);
                        int countColIndex = c.getColumnIndex("count");
                        dbStats.append("Most common author(s): \n");
                        while (!c.isAfterLast() && numAuthorsToReturn != 0) {
                            dbStats.append("\t").append(c.getString(nameColIndex)).append(" (").append(c.getLong(countColIndex)).append(" media)\n");
                            --numAuthorsToReturn;
                            c.moveToNext();
                        }
                        handler.post(() -> tvDbStats.setText(dbStats.toString()));
                    }
                    c.close();

                    //Duplicate filenames
                    sql = "SELECT " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME + ", " +
                            "COUNT(" + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME + ") AS count " +
                            "FROM " + MyOpenHelper.MEDIA_TABLE + " " +
                            "GROUP BY (" + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME + ") " +
                            "HAVING count > 1";
                    c = db.rawQuery(sql, null);
                    if (c.moveToFirst()) {
                        dbStats.append("Duplicate filenames: ");
                        while (!c.isAfterLast()) {
                            dbStats.append("\t").append(c.getString(c.getColumnIndex(MyOpenHelper.COL_MEDIA_FILENAME)))
                                    .append("(").append(c.getInt(c.getColumnIndex("count"))).append(")\n");
                            c.moveToNext();
                        }
                        handler.post(() -> tvDbStats.setText(dbStats.toString()));
                    }
                    c.close();
                    db.close();
                    busy = false;
                    handler.post(() -> tvDbStats.setText(dbStats.toString()));
                });
            }
        });
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_database_config; }

    @Override
    protected void isVerified() { }

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