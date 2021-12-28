package ca.quadrexium.velonathea.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import ca.quadrexium.velonathea.pojo.Media;

public class MyOpenHelper extends SQLiteOpenHelper {

    public final static String DATABASE_NAME = "image_database";
    public final static int DATABASE_VERSION = 7;

    public final static String COL_ID = "id";
    public final static String COL_NAME = "name";

    public final static String MEDIA_TABLE = "media";
    public final static String COL_MEDIA_ID = COL_ID;
    public final static String COL_MEDIA_NAME = COL_NAME;
    public final static String COL_MEDIA_FILENAME = "file_name";
    public final static String COL_MEDIA_AUTHOR_ID = "author_id";
    public final static String COL_MEDIA_LINK = "link";

    public final static String TAG_TABLE = "tag";
    public final static String COL_TAG_ID = COL_ID;
    public final static String COL_TAG_NAME = COL_NAME;

    public final static String MEDIA_TAG_TABLE = MEDIA_TABLE + "_" + TAG_TABLE;
    public final static String COL_MEDIA_TAG_MEDIA_ID = MEDIA_TABLE + "_" + COL_ID;
    public final static String COL_MEDIA_TAG_TAG_ID = TAG_TABLE + "_" + COL_ID;

    public final static String AUTHOR_TABLE = "author";
    public final static String COL_AUTHOR_ID = COL_ID;
    public final static String COL_AUTHOR_NAME = COL_NAME;

    public final static String COL_AUTHOR_NAME_FOREIGN = AUTHOR_TABLE + "_" + COL_AUTHOR_NAME;

    private final String MEDIA_BASE_QUERY = "SELECT " + MEDIA_TABLE + "." + COL_MEDIA_ID + ", " +
            MEDIA_TABLE + "." + COL_MEDIA_FILENAME + ", " +
            MEDIA_TABLE + "." + COL_MEDIA_NAME + ", " +
            AUTHOR_TABLE + "." + COL_AUTHOR_NAME + " AS " + COL_AUTHOR_NAME_FOREIGN + " " +
            "FROM " + MEDIA_TABLE + " " +
            "JOIN " + AUTHOR_TABLE + " ON " +
            AUTHOR_TABLE + "." + COL_AUTHOR_ID + " = " + MEDIA_TABLE + "." + COL_MEDIA_AUTHOR_ID + " ";
    private final String MEDIA_GROUP_BY = "GROUP BY (" + MEDIA_TABLE + "." + COL_MEDIA_FILENAME + ") ";
    private final String TAG_JOIN = "JOIN " + MEDIA_TAG_TABLE + " ON " +
            MEDIA_TABLE + "." + COL_MEDIA_ID + " = " + MEDIA_TAG_TABLE + "." + COL_MEDIA_TAG_MEDIA_ID + " " +
            "JOIN " + TAG_TABLE + " ON " +
            MEDIA_TAG_TABLE + "." + COL_MEDIA_TAG_TAG_ID + " = " + TAG_TABLE + "." + COL_TAG_ID + " ";

    public final static String[] DROP_INDEX_QUERIES = new String[] {
            "DROP INDEX IF EXISTS " + TAG_TABLE + "_" + COL_TAG_NAME + "_index;",
            "DROP INDEX IF EXISTS " + MEDIA_TABLE + "_" + COL_MEDIA_FILENAME + "_index;",
            "DROP INDEX IF EXISTS " + MEDIA_TAG_TABLE + "_" + MEDIA_TAG_TABLE + "_id_index;",
            "DROP INDEX IF EXISTS " + AUTHOR_TABLE + "_" + COL_AUTHOR_NAME + "_index;"
    };
    public final static String[] CREATE_INDEX_QUERIES = new String[] {
            "CREATE UNIQUE INDEX " + TAG_TABLE + "_" + COL_TAG_NAME + "_index ON " + TAG_TABLE + "(" + COL_TAG_NAME + ");",
            "CREATE INDEX " + MEDIA_TABLE + "_" + COL_MEDIA_FILENAME + "_index ON " + MEDIA_TABLE + "(" + COL_MEDIA_FILENAME + ");",
            "CREATE UNIQUE INDEX " + MEDIA_TAG_TABLE + "_" + MEDIA_TAG_TABLE + "_id_index ON " + MEDIA_TAG_TABLE + "(" + COL_MEDIA_TAG_MEDIA_ID + ", " + COL_MEDIA_TAG_TAG_ID + ");",
            "CREATE UNIQUE INDEX " + AUTHOR_TABLE + "_" + COL_AUTHOR_NAME + "_index ON " + AUTHOR_TABLE + "(" + COL_AUTHOR_NAME + ");"
    };

    public MyOpenHelper(Context ctx, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(ctx, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + MEDIA_TABLE + " (" +
                COL_MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MEDIA_NAME + " VARCHAR(150), " +
                COL_MEDIA_FILENAME + " VARCHAR(150), " +
                COL_MEDIA_AUTHOR_ID + " INTEGER, " +
                COL_MEDIA_LINK + " VARCHAR(255), " +
                "FOREIGN KEY (" + COL_MEDIA_AUTHOR_ID + ") REFERENCES " + AUTHOR_TABLE + "(" + COL_ID + "));");
        db.execSQL("CREATE TABLE " + TAG_TABLE + " (" +
                COL_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TAG_NAME + " VARCHAR(150) NOT NULL UNIQUE);");
        db.execSQL("CREATE TABLE " + MEDIA_TAG_TABLE + " (" +
                COL_MEDIA_TAG_MEDIA_ID + " INTEGER NOT NULL, " +
                COL_MEDIA_TAG_TAG_ID + " INTEGER NOT NULL, " +
                "PRIMARY KEY(" + COL_MEDIA_TAG_MEDIA_ID + ", " + COL_MEDIA_TAG_TAG_ID + "), " +
                "FOREIGN KEY(" + COL_MEDIA_TAG_MEDIA_ID + ") REFERENCES " + MEDIA_TABLE + "(" + COL_MEDIA_ID + "), " +
                "FOREIGN KEY(" + COL_MEDIA_TAG_TAG_ID + ") REFERENCES " + TAG_TABLE + "(" + COL_TAG_ID + "));");
        db.execSQL("CREATE TABLE " + AUTHOR_TABLE + " (" +
                COL_AUTHOR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_AUTHOR_NAME + " VARCHAR(80));");
        for (String query : CREATE_INDEX_QUERIES) {
            db.execSQL(query);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS " + MEDIA_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + MEDIA_TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + AUTHOR_TABLE + ";");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS " + MEDIA_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + MEDIA_TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + AUTHOR_TABLE + ";");
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {

    }

    //Gets the author id from the database, or creates an author if they do not exist
    public synchronized int getAuthorId(SQLiteDatabase db, String author) {
        Cursor authorCursor = db.rawQuery("SELECT " + COL_AUTHOR_ID + " " +
                "FROM " + AUTHOR_TABLE +" " +
                "WHERE " + COL_AUTHOR_NAME + " = ? " +
                "LIMIT 1;", new String[]{ author });
        int authorId;
        //If author does not exist, insert author into database
        if (authorCursor.getCount() == 0) {
            ContentValues cv = new ContentValues();
            cv.put(COL_AUTHOR_NAME, author);
            authorId = (int) db.insert(AUTHOR_TABLE, null, cv);
        } else {
            authorCursor.moveToFirst();
            authorId = (int) authorCursor.getLong(authorCursor.getColumnIndex(COL_AUTHOR_ID));
        }
        authorCursor.close();
        return authorId;
    }

    public synchronized int getTagIdOrInsert(SQLiteDatabase db, String tag) {
        Cursor tagCursor = db.rawQuery("SELECT " + COL_TAG_ID + " " +
                "FROM " + TAG_TABLE + " " +
                "WHERE " + COL_TAG_NAME + " = ? " +
                "LIMIT 1;", new String[]{ tag } );
        int tagId;
        //If tag does not exist, insert tag into database
        if (tagCursor.getCount() == 0) {
            ContentValues cv = new ContentValues();
            cv.put(COL_TAG_NAME, tag);
            tagId = (int) db.insert(TAG_TABLE, null, cv);
        } else {
            tagCursor.moveToFirst();
            tagId = (int) tagCursor.getLong(tagCursor.getColumnIndex(COL_TAG_ID));
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
        cv.put(COL_MEDIA_FILENAME, media.getFileName());
        cv.put(COL_MEDIA_NAME, media.getName());
        cv.put(COL_MEDIA_AUTHOR_ID, authorId);
        cv.put(COL_MEDIA_LINK, media.getLink());
        int imageId = (int) db.insert(MEDIA_TABLE, null, cv);
        cv.clear();
        return imageId;
    }

    public synchronized boolean updateMedia(SQLiteDatabase db, Media oldMedia, Media newMedia) {
        if (oldMedia != newMedia) {
            ContentValues cv = new ContentValues();
            cv.put(COL_MEDIA_FILENAME, newMedia.getFileName());
            cv.put(COL_MEDIA_NAME, newMedia.getName());
            cv.put(COL_MEDIA_LINK, newMedia.getLink());
            cv.put(COL_MEDIA_AUTHOR_ID, getAuthorId(db, newMedia.getAuthor()));

            db.update(MEDIA_TABLE, cv, COL_MEDIA_ID + " = ?", new String[]{String.valueOf(newMedia.getId())});
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
                    .id(c.getInt(c.getColumnIndex(COL_MEDIA_ID)))
                    .name(c.getString(c.getColumnIndex(COL_MEDIA_NAME)))
                    .fileName(c.getString(c.getColumnIndex(COL_MEDIA_FILENAME)))
                    .author(c.getString(c.getColumnIndex(AUTHOR_TABLE + "_" + COL_AUTHOR_NAME)))
                    .build();
            mediaList.add(media);
            c.moveToNext();
        }
        c.close();
        return mediaList;
    }

    public synchronized ArrayList<Media> getMediaList(SQLiteDatabase db, TreeMap<String, ArrayList<String>> whereFilters, String[] orderBy) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        StringBuilder query = new StringBuilder(MEDIA_BASE_QUERY);
        if (whereFilters.containsKey(TAG_TABLE + "." + COL_TAG_NAME)) {
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
        query.append(MEDIA_GROUP_BY);
        if (orderBy.length != 0) {
            query.append("ORDER BY ");
            for (int i = 0; i < orderBy.length; i++) {
                query.append(orderBy[i]);
                if (i != orderBy.length-1) {
                    query.append(", ");
                }
            }
        }

        String[] stringSelectionArgs = selectionArgs.toArray(new String[0]);
        Cursor c = db.rawQuery(query.toString(), stringSelectionArgs);
        return parseMediaListFromCursor(c);
    }

    //For when a given media has some null values, eg the link, returns the media object with those
    // values set
    public synchronized Media getRemainingData(SQLiteDatabase db, Media media) {
        //TODO: Construct a query that will only get missing values
        return getMedia(db, media.getId());
    }
    
    //Gets a single media with all values initialized
    public synchronized Media getMedia(SQLiteDatabase db, int mediaId) {
        //removing the space at the end to query more columns
        String query = MEDIA_BASE_QUERY.substring(0, MEDIA_BASE_QUERY.indexOf("FROM")-1);
        query += ", " + MEDIA_TABLE + "." + COL_MEDIA_LINK + " "; //getting image link
        query += MEDIA_BASE_QUERY.substring(MEDIA_BASE_QUERY.indexOf("FROM"));
        query += "WHERE " + MEDIA_TABLE + "." + COL_MEDIA_ID + " = ? " +
                MEDIA_GROUP_BY + "LIMIT 1";
        Cursor c = db.rawQuery(query, new String[] { String.valueOf(mediaId) });
        c.moveToFirst();
        Media media = null;
        if (!c.isAfterLast()) {
             media = new Media.Builder()
                    .id(c.getInt(c.getColumnIndex(COL_MEDIA_ID)))
                    .name(c.getString(c.getColumnIndex(COL_MEDIA_NAME)))
                    .fileName(c.getString(c.getColumnIndex(COL_MEDIA_FILENAME)))
                    .author(c.getString(c.getColumnIndex(AUTHOR_TABLE + "_" + COL_AUTHOR_NAME)))
                    .link(c.getString(c.getColumnIndex(COL_MEDIA_LINK)))
                    .build();
        }
        c.close();
        return media;
    }
}
