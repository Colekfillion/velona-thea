package ca.quadrexium.velonathea.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ca.quadrexium.velonathea.pojo.Media;

public class MyOpenHelper extends SQLiteOpenHelper {

    public final static String DATABASE_NAME = "image_database";
    public final static int DATABASE_VERSION = 5;

    public final static String COL_ID = "id";
    public final static String COL_NAME = "name";

    public final static String IMAGE_TABLE = "image";
    public final static String COL_IMAGE_FILENAME = "file_name";
    public final static String COL_IMAGE_AUTHOR_ID = "author_id";
    public final static String COL_IMAGE_LINK = "link";

    public final static String TAG_TABLE = "tag";

    public final static String IMAGE_TAG_TABLE = IMAGE_TABLE + "_" + TAG_TABLE;
    public final static String COL_IMAGE_TAG_IMAGE_ID = IMAGE_TABLE + "_" + COL_ID;
    public final static String COL_IMAGE_TAG_TAG_ID = TAG_TABLE + "_" + COL_ID;

    private final String IMAGE_BASE_QUERY = "SELECT image.id, image.file_name, image.name, author.name AS author_name " +
            "FROM image " +
            "JOIN author ON author.id = image.author_id ";
    private final String IMAGE_GROUP_BY = "GROUP BY (image.file_name) ";
    private final String TAG_JOIN = "JOIN image_tag ON image.id = image_tag.image_id " +
            "JOIN tag ON image_tag.tag_id = tag.id ";

    public final static String AUTHOR_TABLE = "author";

    public final static String[] DROP_INDEX_QUERIES = new String[] {
            "DROP INDEX IF EXISTS " + MyOpenHelper.TAG_TABLE + "_name_index;",
            "DROP INDEX IF EXISTS " + MyOpenHelper.IMAGE_TABLE + "_filename_index;",
            "DROP INDEX IF EXISTS " + MyOpenHelper.IMAGE_TAG_TABLE + "_image_tag_id_index;",
            "DROP INDEX IF EXISTS " + MyOpenHelper.AUTHOR_TABLE + "_name_index;"
    };
    public final static String[] CREATE_INDEX_QUERIES = new String[] {
            "CREATE UNIQUE INDEX " + TAG_TABLE + "_name_index ON " + TAG_TABLE + "(" + COL_NAME + ");",
            "CREATE INDEX " + IMAGE_TABLE + "_filename_index ON " + IMAGE_TABLE + "(" + COL_IMAGE_FILENAME + ");",
            "CREATE UNIQUE INDEX " + IMAGE_TAG_TABLE + "_image_tag_id_index ON " + IMAGE_TAG_TABLE + "(" + COL_IMAGE_TAG_IMAGE_ID + ", " + COL_IMAGE_TAG_TAG_ID + ");",
            "CREATE UNIQUE INDEX " + AUTHOR_TABLE + "_name_index ON " + AUTHOR_TABLE + "(" + COL_NAME + ");"
    };

    public MyOpenHelper(Context ctx, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(ctx, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + IMAGE_TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " VARCHAR(150), " +
                COL_IMAGE_FILENAME + " VARCHAR(150), " +
                COL_IMAGE_AUTHOR_ID + " INTEGER, " +
                COL_IMAGE_LINK + " VARCHAR(255), " +
                "FOREIGN KEY (" + COL_IMAGE_AUTHOR_ID + ") REFERENCES " + AUTHOR_TABLE + "(" + COL_ID + "));");
        db.execSQL("CREATE TABLE " + TAG_TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " VARCHAR(150) NOT NULL UNIQUE);");
        db.execSQL("CREATE TABLE " + IMAGE_TAG_TABLE + " (" +
                COL_IMAGE_TAG_IMAGE_ID + " INTEGER NOT NULL, " +
                COL_IMAGE_TAG_TAG_ID + " INTEGER NOT NULL, " +
                "PRIMARY KEY(" + COL_IMAGE_TAG_IMAGE_ID + ", " + COL_IMAGE_TAG_TAG_ID + "), " +
                "FOREIGN KEY(" + COL_IMAGE_TAG_IMAGE_ID + ") REFERENCES " + IMAGE_TABLE + "(" + COL_ID + "), " +
                "FOREIGN KEY(" + COL_IMAGE_TAG_TAG_ID + ") REFERENCES " + TAG_TABLE + "(" + COL_ID + "));");
        db.execSQL("CREATE TABLE " + AUTHOR_TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " VARCHAR(80));");
        for (String query : CREATE_INDEX_QUERIES) {
            db.execSQL(query);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS " + IMAGE_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + IMAGE_TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + AUTHOR_TABLE + ";");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS " + IMAGE_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + IMAGE_TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + AUTHOR_TABLE + ";");
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {

    }

    //Gets the author id from the database, or creates an author if they do not exist
    public synchronized int getAuthorId(SQLiteDatabase db, String author) {
        Cursor authorCursor = db.rawQuery("SELECT id FROM author WHERE name = ? LIMIT 1;", new String[]{ author });
        int authorId;
        //If author does not exist, insert author into database
        if (authorCursor.getCount() == 0) {
            ContentValues cv = new ContentValues();
            cv.put(MyOpenHelper.COL_NAME, author);
            authorId = (int) db.insert(MyOpenHelper.AUTHOR_TABLE, null, cv);
        } else {
            authorCursor.moveToFirst();
            authorId = (int) authorCursor.getLong(authorCursor.getColumnIndex(MyOpenHelper.COL_ID));
        }
        authorCursor.close();
        return authorId;
    }

    public synchronized int getTagIdOrInsert(SQLiteDatabase db, String tag) {
        Cursor tagCursor = db.rawQuery("SELECT id FROM tag WHERE name = ? LIMIT 1;", new String[]{ tag } );
        int tagId;
        //If tag does not exist, insert tag into database
        if (tagCursor.getCount() == 0) {
            ContentValues cv = new ContentValues();
            cv.put(MyOpenHelper.COL_NAME, tag);
            tagId = (int) db.insert(MyOpenHelper.TAG_TABLE, null, cv);
        } else {
            tagCursor.moveToFirst();
            tagId = (int) tagCursor.getLong(tagCursor.getColumnIndex(COL_ID));
        }
        tagCursor.close();
        return tagId;
    }

//    public synchronized int getTagId(SQLiteDatabase db, String tag) {
//        Cursor tagCursor = db.rawQuery("SELECT id FROM tag WHERE name = ? LIMIT 1;", new String[]{ tag } );
//        int tagId;
//        if (tagCursor.getCount() == 0) {
//            tagId = -1;
//        } else {
//            tagCursor.moveToFirst();
//            tagId = (int) tagCursor.getLong(tagCursor.getColumnIndex(COL_ID));
//        }
//        tagCursor.close();
//        return tagId;
//    }

    public synchronized int insertMedia(SQLiteDatabase db, Media media) {
        int authorId = getAuthorId(db, media.getAuthor());

        //Insert new row into database
        ContentValues cv = new ContentValues();
        cv.put(MyOpenHelper.COL_IMAGE_FILENAME, media.getFileName());
        cv.put(MyOpenHelper.COL_NAME, media.getName());
        cv.put(MyOpenHelper.COL_IMAGE_AUTHOR_ID, authorId);
        cv.put(MyOpenHelper.COL_IMAGE_LINK, media.getLink());
        int imageId = (int) db.insert(MyOpenHelper.IMAGE_TABLE, null, cv);
        cv.clear();
        return imageId;
    }

    public synchronized boolean updateMedia(SQLiteDatabase db, Media oldMedia, Media newMedia) {
        if (oldMedia != newMedia) {
            ContentValues cv = new ContentValues();
            cv.put(MyOpenHelper.COL_IMAGE_FILENAME, newMedia.getFileName());
            cv.put(MyOpenHelper.COL_NAME, newMedia.getName());
            cv.put(MyOpenHelper.COL_IMAGE_LINK, newMedia.getLink());
            cv.put(MyOpenHelper.COL_IMAGE_AUTHOR_ID, getAuthorId(db, newMedia.getAuthor()));

            db.update(MyOpenHelper.IMAGE_TABLE, cv, "id = ?", new String[]{String.valueOf(newMedia.getId())});
            return true;
        }
        return false;
    }

    //Creates a media list from a given cursor
    private synchronized ArrayList<Media> parseMediaListFromCursor(Cursor c) {
        ArrayList<Media> mediaList = new ArrayList<>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            Media media = new Media.Builder()
                    .id((int) c.getInt(c.getColumnIndex(MyOpenHelper.COL_ID)))
                    .name(c.getString(c.getColumnIndex(MyOpenHelper.COL_NAME)))
                    .fileName(c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_FILENAME)))
                    .author(c.getString(c.getColumnIndex(MyOpenHelper.AUTHOR_TABLE + "_" + MyOpenHelper.COL_NAME)))
                    .build();
            mediaList.add(media);
            c.moveToNext();
        }
        c.close();
        return mediaList;
    }

    public synchronized ArrayList<Media> getMediaList(SQLiteDatabase db, TreeMap<String, ArrayList<String>> whereFilters, String[] orderBy) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        StringBuilder query = new StringBuilder(IMAGE_BASE_QUERY);
        if (whereFilters.containsKey(MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_NAME)) {
            query.append(TAG_JOIN);
        }
        //Creates the where clause
        //whereFilters values can be an arraylist with one or more values. If there is only one:
        // "WHERE (column LIKE value) AND (column2 LIKE value2)"
        // If there are multiple:
        // "WHERE (column LIKE valueA OR column LIKE valueB) AND (column2 LIKE value2)
        if (whereFilters.size() != 0) {
            query.append("WHERE ");
            for (Map.Entry<String, ArrayList<String>> entry : whereFilters.entrySet()) {
                //For filtering when the column value may match multiple values (OR clause)
                query.append("(");
                for (int i = 0; i < entry.getValue().size(); i++) {
                    query.append(entry.getKey()).append(" LIKE ? ");
                    selectionArgs.add("%" + entry.getValue().get(i) + "%");
                    if (entry.getValue().size() > 1 && i != entry.getValue().size()-1) {
                        query.append("OR ");
                    }
                }
                query.append(") ");
                //For filtering other columns, append AND (AND clause)
                if (!entry.equals(whereFilters.lastEntry())) {
                    query.append("AND ");
                }
            }
        }
        query.append(IMAGE_GROUP_BY);
        if (orderBy.length != 0) {
            query.append("ORDER BY ");
            for (int i = 0; i < orderBy.length; i++) {
                query.append("?");
                selectionArgs.add(orderBy[i]);
                if (i != orderBy.length-1) {
                    query.append(", ");
                }
            }
        }

        String[] stringSelectionArgs = selectionArgs.toArray(new String[0]);
        Cursor c = db.rawQuery(query.toString(), stringSelectionArgs);
        return parseMediaListFromCursor(c);
    }
}
