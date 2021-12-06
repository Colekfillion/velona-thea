package com.example.velonathea;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyOpenHelper extends SQLiteOpenHelper {

    protected final static String DATABASE_NAME = "image_database";
    protected final static int DATABASE_VERSION = 2;

    protected final static String IMAGE_TABLE = "image";
    protected final static String COL_IMAGE_ID = "id";
    protected final static String COL_IMAGE_NAME = "name";
    protected final static String COL_IMAGE_FILENAME = "file_name";
    protected final static String COL_IMAGE_AUTHOR = "author";
    protected final static String COL_IMAGE_LINK = "link";

    protected final static String TAG_TABLE = "tag";
    protected final static String COL_TAG_ID = "id";
    protected final static String COL_TAG_NAME = "name";

    protected final static String IMAGE_TAG_TABLE = "image_tag";
    protected final static String COL_IMAGE_TAG_IMAGE_ID = "image_id";
    protected final static String COL_IMAGE_TAG_TAG_ID = "tag_id";

    public MyOpenHelper(Context ctx, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(ctx, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + IMAGE_TABLE + " (" +
                COL_IMAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_IMAGE_NAME + " VARCHAR(150), " +
                COL_IMAGE_FILENAME + " VARCHAR(150), " +
                COL_IMAGE_AUTHOR + " VARCHAR(50), " +
                COL_IMAGE_LINK + " VARCHAR(255));");
        db.execSQL("CREATE TABLE " + TAG_TABLE + " (" +
                COL_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TAG_NAME + " VARCHAR(150) NOT NULL UNIQUE);");
        db.execSQL("CREATE TABLE " + IMAGE_TAG_TABLE + " (" +
                COL_IMAGE_TAG_IMAGE_ID + " INTEGER NOT NULL, " +
                COL_IMAGE_TAG_TAG_ID + " INTEGER NOT NULL, " +
                "PRIMARY KEY(" + COL_IMAGE_TAG_IMAGE_ID + ", " + COL_IMAGE_TAG_TAG_ID + "), " +
                "FOREIGN KEY(" + COL_IMAGE_TAG_IMAGE_ID + ") REFERENCES " + IMAGE_TABLE + "(" + COL_IMAGE_ID + "), " +
                "FOREIGN KEY(" + COL_IMAGE_TAG_TAG_ID + ") REFERENCES " + TAG_TABLE + "(" + COL_TAG_ID + "));");
        db.execSQL("CREATE INDEX " + TAG_TABLE + "_name_index ON " + TAG_TABLE + "(" + COL_TAG_NAME + ");");
        db.execSQL("CREATE INDEX " + IMAGE_TABLE + "_filename_index ON " + IMAGE_TABLE + "(" + COL_IMAGE_FILENAME + ");");
        db.execSQL("CREATE INDEX " + IMAGE_TAG_TABLE + "_image_tag_id_index ON " + IMAGE_TAG_TABLE + "(" + COL_IMAGE_TAG_IMAGE_ID + ", " + COL_IMAGE_TAG_TAG_ID + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS image");
        db.execSQL("DROP TABLE IF EXISTS tag");
        db.execSQL("DROP TABLE IF EXISTS image_tag");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS image");
        db.execSQL("DROP TABLE IF EXISTS tag");
        db.execSQL("DROP TABLE IF EXISTS image_tag");
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {

    }
}
