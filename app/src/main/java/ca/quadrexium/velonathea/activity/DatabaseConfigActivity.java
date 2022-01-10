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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
import ca.quadrexium.velonathea.pojo.Notification;

//TODO: Better notifications. Shouldn't show up after dismissal
//TODO: Create app logo for notifications.
public class DatabaseConfigActivity extends BaseActivity {

    private static boolean busy = false;
    private final int LOAD_FILE_NOTIFICATION_ID = 12;
    private final int LOAD_MEDIA_ROOT_NOTIFICATION_ID = 64;

    private Notification notification;

    //Loads rows from selected text file
    private final ActivityResultLauncher<Intent> loadRowsActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        busy = true;
                        Uri uri = data.getData();

                        ProgressBar pb = findViewById(R.id.activity_database_config_pb_loading);
                        TextView tvLoading = findViewById(R.id.activity_database_config_tv_loading);

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Handler handler = new Handler(Looper.getMainLooper());

                        executor.execute(() -> {
                            //Read the file as a string
                            handler.post(() -> {
                                pb.setVisibility(View.VISIBLE);
                                tvLoading.setVisibility(View.VISIBLE);
                                notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_1,
                                        LOAD_FILE_NOTIFICATION_ID)
                                        .title(getString(R.string.loading_media))
                                        .content(getString(R.string.preparing))
                                        .smallIcon(R.drawable.null_image).build();
                            });

                            String text = readStringFromUri(uri);
                            if (!text.equals("")) {
                                //Prepare for inserting rows into db
                                MyOpenHelper myOpenHelper = getMyOpenHelper();
                                SQLiteDatabase db = myOpenHelper.getWritableDatabase();

                                db.beginTransaction();
                                //Dropping indexes for performance
                                for (String query : MyOpenHelper.DROP_INDEX_QUERIES) {
                                    db.execSQL(query);
                                }

                                //TODO: Check filenames in database.. probably do this after path migration
                                String[] rows = text.split("\n");

                                int numRows = rows.length;
                                for (int i = 0; i < rows.length; i++) {
                                    String[] rowValues = rows[i].split("\t");
                                    Media.Builder mediaBuilder = new Media.Builder()
                                            .id(-1) //temp value
                                            .fileName(rowValues[0])
                                            .name(rowValues[1])
                                            .author(Constants.isStringEmpty(rowValues[2]) ? "unknown" : rowValues[2]);
                                    try {
                                        mediaBuilder.link(rowValues[3]);
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        //no link to source, do nothing
                                    }
                                    try {
                                        mediaBuilder.tags(new HashSet<>(Arrays.asList(rowValues[4].split(" "))));
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        //no tags, do nothing
                                    }

                                    Media media = mediaBuilder.build();

                                    myOpenHelper.insertMedia(db, media);
                                    int finalI = i;
                                    handler.post(() -> {
                                        int percentProgress = (int) (((double) finalI / (double) numRows) * 100);
                                        String content = finalI + "/" + numRows +
                                                " (" + percentProgress + "%)";
                                        if (!isVisible) {
                                            notification.setContent(content);
                                            notification.setProgress(percentProgress*10);
                                            notification.show();
                                        } else {
                                            pb.setProgress(percentProgress*10);
                                            tvLoading.setText(String.format("%s, %s", getString(R.string.loading_media), content));
                                        }
                                    });
                                }
                                //Recreating the indexes
                                for (String query : MyOpenHelper.CREATE_INDEX_QUERIES) {
                                    db.execSQL(query);
                                }
                                db.setTransactionSuccessful();
                                db.endTransaction();
                                busy = false;
                                handler.post(() -> {
                                    if (!isVisible) {
                                        notification.setTitle(getString(R.string.loading_complete));
                                        notification.setContent(numRows + "/" + numRows);
                                        notification.setProgress(0);
                                        notification.show();
                                    }
                                    pb.setProgress(0);
                                    pb.setVisibility(View.GONE);
                                    tvLoading.setText(String.format("%s, %s",
                                            getString(R.string.loading_complete),
                                            numRows + "/" + numRows));
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

                ProgressBar pb = findViewById(R.id.activity_database_config_pb_loading);
                TextView tvLoading = findViewById(R.id.activity_database_config_tv_loading);
                tvLoading.setVisibility(View.VISIBLE);
                pb.setVisibility(View.VISIBLE);

                notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_1,
                        LOAD_MEDIA_ROOT_NOTIFICATION_ID)
                        .title(getString(R.string.loading_media))
                        .content(getString(R.string.preparing))
                        .smallIcon(R.drawable.null_image).build();

                tvLoading.setText(String.format("%s, %s", getString(R.string.loading_media), getString(R.string.preparing)));
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    //Get all valid media files in root directory
                    File root = new File(path);
                    String[] filesInRoot = root.list((dir, name) -> {
                        int startingIndex = name.lastIndexOf(".");
                        if (startingIndex == -1) { return false; }
                        String extension = name.substring(startingIndex);
                        return Constants.IMAGE_EXTENSIONS.contains(extension) ||
                            Constants.VIDEO_EXTENSIONS.contains(extension) ||
                                extension.equals(".gif");
                    });
                    if (filesInRoot == null) {
                        return; //no files
                    }
                    Set<String> fileNames = new HashSet<>(Arrays.asList(filesInRoot));
                    int fileNamesLength = fileNames.size();

                    //Filter filenames that are already in the database
                    MyOpenHelper myOpenHelper = getMyOpenHelper();
                    SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                    Cursor c = db.rawQuery("SELECT " + MyOpenHelper.MEDIA_TABLE + "." +
                            MyOpenHelper.COL_MEDIA_FILENAME + " FROM " + MyOpenHelper.MEDIA_TABLE, null);
                    c.moveToFirst();
                    int fileNameIndex = c.getColumnIndex(MyOpenHelper.COL_MEDIA_FILENAME);

                    while (!c.isAfterLast()) {
                        boolean wasRemoved = fileNames.remove(c.getString(fileNameIndex));
                        if (wasRemoved) {
                            fileNamesLength--;
                        }
                        c.moveToNext();
                    }
                    c.close();

                    int count = 0;
                    //Insert media
                    db.beginTransaction();
                    for (String fileName : fileNames) {
                        String name = fileName.substring(0, fileName.lastIndexOf("."));
                        Media media = new Media.Builder()
                                .id(-1)
                                .fileName(fileName)
                                .name(name)
                                .author("unknown")
                                .build();
                        //To diagnose a one-time unreproducible bug where the name was not the
                        // filename without the extension
                        if (!fileName.contains(media.getName())) {
                            handler.post(() -> {
                                Toast.makeText(this, "Assertion error: " +
                                        fileName.substring(0, fileName.lastIndexOf("."))
                                        + " != " + name, Toast.LENGTH_LONG).show();
                                finish();
                            });
                            return;
                        }
                        int mediaId = myOpenHelper.insertMedia(db, media);
                        if (mediaId == -1) { //insertion error
                            handler.post(() -> {
                                Toast.makeText(this, "Error inserting " +
                                                "media into database",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            });
                            return;
                        }
                        count++;
                        int finalCount = count;
                        double percent = ((double) finalCount / (double) fileNamesLength) * 1000;
                        String notificationContent = count + "/" + fileNamesLength;
                        String tvLoadingText = getString(R.string.loading_media) + ", " + count + "/" + fileNamesLength;
                        if (count % 4 == 0) { //give the ui thread some time to process
                            handler.post(() -> {
                                tvLoading.setText(tvLoadingText);
                                pb.setProgress((int) percent);
                                if (!isVisible) {
                                    notification.setContent(notificationContent);
                                    notification.setProgress((int) percent);
                                    notification.show();
                                }
                            });
                        }
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    db.close();
                    busy = false;
                    String content = count + "/" + count;
                    String tvLoadingText = getString(R.string.loading_complete) + ", " + content;
                    handler.post(() -> {
                        tvLoading.setText(tvLoadingText);
                        pb.setVisibility(View.GONE);
                        pb.setProgress(0);
                        if (!isVisible) {
                            notification.setTitle(tvLoadingText);
                            notification.setContent(content);
                            notification.setProgress(0);
                            notification.show();
                        }
                    });
                });
            }
        });

        Button btnDbExport = findViewById(R.id.activity_database_config_btn_export_media);
        btnDbExport.setOnClickListener(v -> {
            if (!busy) {
                EditText edit = new EditText(this);
                String defaultName = "mediaList";
                edit.setHint("Filename (without extension)");
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                alertDialogBuilder.setTitle("Export media")

                        .setView(edit)

                        .setPositiveButton(android.R.string.ok, (click, arg) -> {

                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            Handler handler = new Handler(Looper.getMainLooper());

                            executor.execute(() -> {
                                busy = true;
                                String fileName = Constants.isStringEmpty(edit.getText().toString()) ? defaultName : edit.getText().toString();
                                fileName += ".txt";
                                MyOpenHelper myOpenHelper = getMyOpenHelper();
                                SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                                Cursor c = db.rawQuery(MyOpenHelper.getAllMediaQuery(), null);
                                ArrayList<Media> mediaList = myOpenHelper.parseMediaListFromCursor(c);
                                c.close();

                                int total = mediaList.size();
                                ProgressBar pb = this.findViewById(R.id.activity_database_config_pb_loading);
                                handler.post(() -> pb.setVisibility(View.VISIBLE));

                                int count = 0;
                                StringBuilder mediaListAsString = new StringBuilder();
                                for (Media media : mediaList) {

                                    String name = media.getName() != null ? media.getName() : "";
                                    String author = media.getAuthor() != null ? media.getAuthor() : "";
                                    String link = media.getLink() != null ? media.getLink() : "";

                                    mediaListAsString.append(media.getFileName()).append("\t");
                                    mediaListAsString.append(name).append("\t");
                                    mediaListAsString.append(author).append("\t");
                                    mediaListAsString.append(link).append("\t");
                                    mediaListAsString.append(media.getTagsAsString()).append("\t");
                                    mediaListAsString.append("\n");
                                    count++;
                                    int finalCount = count;
                                    handler.post(() -> pb.setProgress((int) ((double) finalCount / (double) total) * 1000));
                                }
                                try {
                                    FileWriter myWriter = new FileWriter(this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + fileName);
                                    myWriter.write(mediaListAsString.toString());
                                    myWriter.close();
                                    handler.post(() -> Toast.makeText(this, R.string.success, Toast.LENGTH_LONG).show());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    handler.post(() -> Toast.makeText(this, R.string.fail, Toast.LENGTH_LONG).show());
                                }
                                handler.post(() -> {
                                    pb.setVisibility(View.GONE);
                                    pb.setProgress(0);
                                });
                                busy = false;
                            });
                        })

                        .setNegativeButton(android.R.string.cancel, (click, arg) -> { })

                        .create().show();
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
                                MyOpenHelper myOpenHelper = getMyOpenHelper();
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
                                    Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
                                });
                                busy = false;
                            });
                        })

                        .setNegativeButton(R.string.no, (click, arg) -> { })

                        .create().show();
            }
        });

        Button showInvalidFiles = findViewById(R.id.activity_database_config_btn_invalidfiles);
        showInvalidFiles.setOnClickListener(v -> {
            if (!busy) {
                busy = true;
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
                String path = prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES);

                MyOpenHelper myOpenHelper = getMyOpenHelper();
                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                db.execSQL("DROP TABLE IF EXISTS temp_media_invalid;");
                db.execSQL("CREATE TABLE temp_media_invalid (" +
                        "media_id INTEGER NOT NULL, " +
                        "FOREIGN KEY(media_id) REFERENCES " + MyOpenHelper.MEDIA_TABLE + "(" + MyOpenHelper.COL_MEDIA_ID + "))");

                Cursor c = db.rawQuery("SELECT " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_ID + ", " +
                        MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME + " " +
                        "FROM " + MyOpenHelper.MEDIA_TABLE, null);
                db.beginTransaction();
                int idIndex = c.getColumnIndex(MyOpenHelper.COL_MEDIA_ID);
                int fileNameIndex = c.getColumnIndex(MyOpenHelper.COL_MEDIA_FILENAME);
                if (c.moveToFirst()) {
                    ContentValues cv = new ContentValues();
                    int count = 0;
                    while (!c.isAfterLast()) {
                        String fileName = c.getString(fileNameIndex);
                        File f = new File(path, fileName);
                        if (!f.exists() || (f.exists() && f.length() == 0)) {
                            cv.put("media_id", c.getInt(idIndex));
                            db.insert("temp_media_invalid", null, cv);
                            cv.clear();
                        }
                        count++;
                        System.out.println(count);
                        c.moveToNext();
                    }
                }
                c.close();
                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();

                String sql = "SELECT " +
                        MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME + " AS " +
                            MyOpenHelper.COL_MEDIA_FILENAME_ALIAS + ", " +
                        MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_ID + " AS " +
                            MyOpenHelper.COL_MEDIA_ID_ALIAS + " " +
                        "FROM " + MyOpenHelper.MEDIA_TABLE + " " +
                        "JOIN temp_media_invalid ON temp_media_invalid.media_id = " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_ID + " " +
                        "ORDER BY " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME + " ASC";

                Bundle dataToPass = new Bundle();

                dataToPass.putString(Constants.MEDIA_QUERY, sql);
                dataToPass.putStringArray(Constants.QUERY_ARGS, null);

                Intent intent = new Intent(this, SearchResultsActivity.class);
                intent.putExtras(dataToPass);
                startActivity(intent);
            }
            busy = false;
        });

        //Show various database statistics
        //TODO: Have additional info be shown in a popup, ex. button that says "Show duplicate files"
        // Low priority
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
                    MyOpenHelper myOpenHelper = getMyOpenHelper();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (notification != null) {
            notification.dismiss();
        }
    }
}