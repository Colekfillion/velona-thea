package ca.colekfillion.velonathea.activity;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
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

import androidx.appcompat.app.AlertDialog;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.colekfillion.velonathea.R;
import ca.colekfillion.velonathea.database.MyOpenHelper;
import ca.colekfillion.velonathea.pojo.Constants;
import ca.colekfillion.velonathea.pojo.Media;

//TODO: Better notifications. Shouldn't show up after dismissal
public class DatabaseConfigActivity extends BaseActivity {

    private static boolean busy = false; //is the database being used?
    Button btnDebugDb;
    Button showInvalidFiles;
    Button btnClearDb;
    Button btnDbExport;

    @Override
    protected void initViews() {
        btnDebugDb = findViewById(R.id.activity_database_config_btn_debugdb);
        showInvalidFiles = findViewById(R.id.activity_database_config_btn_invalidfiles);
        btnClearDb = findViewById(R.id.activity_database_config_btn_cleardb);
        btnDbExport = findViewById(R.id.activity_database_config_btn_export_media);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

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

                                    mediaListAsString.append(media.getFilePath()).append("\t");
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

                        .setNegativeButton(android.R.string.cancel, (click, arg) -> {
                        })

                        .create().show();
            }
        });

        //Delete all data from the database
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

                        .setNegativeButton(R.string.no, (click, arg) -> {
                        })

                        .create().show();
            }
        });

//        showInvalidFiles.setOnClickListener(v -> {
//            if (!busy) {
//                busy = true;
//                MyOpenHelper myOpenHelper = getMyOpenHelper();
//                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
//                db.execSQL("DROP TABLE IF EXISTS temp_media_invalid;");
//                db.execSQL("CREATE TABLE temp_media_invalid (" +
//                        "media_id INTEGER NOT NULL, " +
//                        "FOREIGN KEY(media_id) REFERENCES " + MyOpenHelper.MEDIA_TABLE + "(" + MyOpenHelper.COL_MEDIA_ID + "))");
//
//                Cursor c = db.rawQuery("SELECT " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_ID + ", " +
//                        MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_PATH + " " +
//                        "FROM " + MyOpenHelper.MEDIA_TABLE, null);
//                db.beginTransaction();
//                int idIndex = c.getColumnIndex(MyOpenHelper.COL_MEDIA_ID);
//                int filePathIndex = c.getColumnIndex(MyOpenHelper.COL_MEDIA_PATH);
//                if (c.moveToFirst()) {
//                    ContentValues cv = new ContentValues();
//                    int count = 0;
//                    while (!c.isAfterLast()) {
//                        String filePath = c.getString(filePathIndex);
//                        File f = new File(filePath);
//                        if (!f.exists() || (f.exists() && f.length() == 0)) {
//                            cv.put("media_id", c.getInt(idIndex));
//                            db.insert("temp_media_invalid", null, cv);
//                            cv.clear();
//                        }
//                        count++;
//                        System.out.println(count);
//                        c.moveToNext();
//                    }
//                }
//                c.close();
//                db.setTransactionSuccessful();
//                db.endTransaction();
//                db.close();
//
//                String sql = "SELECT " +
//                        MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_PATH + " AS " +
//                            MyOpenHelper.COL_MEDIA_FILEPATH_ALIAS + ", " +
//                        MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_ID + " AS " +
//                            MyOpenHelper.COL_MEDIA_ID_ALIAS + " " +
//                        "FROM " + MyOpenHelper.MEDIA_TABLE + " " +
//                        "JOIN temp_media_invalid ON temp_media_invalid.media_id = " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_ID + " " +
//                        "ORDER BY " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_PATH + " ASC";
//
//                Bundle dataToPass = new Bundle();
//
//                dataToPass.putString(Constants.MEDIA_QUERY, sql);
//                dataToPass.putStringArray(Constants.QUERY_ARGS, null);
//
//                Intent intent = new Intent(this, SearchResultsActivity.class);
//                intent.putExtras(dataToPass);
//                startActivity(intent);
//            }
//            busy = false;
//        });

        //Show various database statistics
        //TODO: Have additional info be shown in a popup, ex. button that says "Show duplicate files"
        // Like a side-by-side? Not sure what past me was thinking about here
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
                    int numAuthorsToReturn = Math.min(20, c.getCount());
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
//                    sql = "SELECT " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_PATH + ", " +
//                            "COUNT(" + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_PATH + ") AS count " +
//                            "FROM " + MyOpenHelper.MEDIA_TABLE + " " +
//                            "GROUP BY (" + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_PATH + ") " +
//                            "HAVING count > 1";
//                    c = db.rawQuery(sql, null);
//                    if (c.moveToFirst()) {
//                        dbStats.append("Duplicate filenames: ");
//                        while (!c.isAfterLast()) {
//                            dbStats.append("\t").append(c.getString(c.getColumnIndex(MyOpenHelper.COL_MEDIA_PATH)))
//                                    .append("(").append(c.getInt(c.getColumnIndex("count"))).append(")\n");
//                            c.moveToNext();
//                        }
//                        handler.post(() -> tvDbStats.setText(dbStats.toString()));
//                    }
//                    c.close();
//                    db.close();
//                    busy = false;
//                    handler.post(() -> tvDbStats.setText(dbStats.toString()));
                });
            }
        });
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_database_config;
    }

    @Override
    protected void isVerified() {
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

    @Override
    protected String getName() {
        return "DatabaseConfigActivity";
    }
}