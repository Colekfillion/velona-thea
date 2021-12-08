package ca.quadrexium.velonathea;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyOpenHelper extends SQLiteOpenHelper {

    protected final static String DATABASE_NAME = "image_database";
    protected final static int DATABASE_VERSION = 3;

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

    protected final static String[] DROP_INDEX_QUERIES = new String[] {
            "DROP INDEX IF EXISTS " + MyOpenHelper.TAG_TABLE + "_name_index;",
            "DROP INDEX IF EXISTS " + MyOpenHelper.IMAGE_TABLE + "_filename_index;",
            "DROP INDEX IF EXISTS " + MyOpenHelper.IMAGE_TAG_TABLE + "_image_tag_id_index;"
    };
    protected final static String[] CREATE_INDEX_QUERIES = new String[] {
            "CREATE INDEX " + TAG_TABLE + "_name_index ON " + TAG_TABLE + "(" + COL_TAG_NAME + ");",
            "CREATE INDEX " + IMAGE_TABLE + "_filename_index ON " + IMAGE_TABLE + "(" + COL_IMAGE_FILENAME + ");",
            "CREATE INDEX " + IMAGE_TAG_TABLE + "_image_tag_id_index ON " + IMAGE_TAG_TABLE + "(" + COL_IMAGE_TAG_IMAGE_ID + ", " + COL_IMAGE_TAG_TAG_ID + ");"
    };

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
        for (String query : CREATE_INDEX_QUERIES) {
            db.execSQL(query);
        }

        //View that should have increased performance but it doesn't
//        db.execSQL("CREATE VIEW v_image_tags AS " +
//                "SELECT image.*, group_concat(tag.name, \" \") AS tags FROM image " +
//                "JOIN image_tag ON image.id = image_tag.image_id " +
//                "JOIN tag ON tag.id = image_tag.tag_id " +
//                "GROUP BY image.file_name;");
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
