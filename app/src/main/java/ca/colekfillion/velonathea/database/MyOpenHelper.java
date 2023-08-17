package ca.colekfillion.velonathea.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.colekfillion.velonathea.pojo.Media;

public class MyOpenHelper extends SQLiteOpenHelper {

    public final static String DATABASE_NAME = "image_database";
    public final static int DATABASE_VERSION = 11;

    public final static String COL_ID = "id";
    public final static String COL_NAME = "name";

    public final static String MEDIA_TABLE = "media";
    public final static String COL_MEDIA_ID = COL_ID;
    public final static String COL_MEDIA_NAME = COL_NAME;
    public final static String COL_MEDIA_FILENAME = "filename";
    public final static String COL_MEDIA_AUTHOR_ID = "author_id";
    public final static String COL_MEDIA_LINK = "link";
    public final static String COL_MEDIA_INDEXED = "is_indexed";
    public final static String COL_MEDIA_FILEPATH_ID = "filepath_id";

    public final static String FILEPATH_TABLE = "filepath";
    public final static String COL_FILEPATH_ID = COL_ID;
    public final static String COL_FILEPATH_NAME = COL_NAME;

    public final static String TAG_TABLE = "tag";
    public final static String COL_TAG_ID = COL_ID;
    public final static String COL_TAG_NAME = COL_NAME;

    public final static String MEDIA_TAG_TABLE = MEDIA_TABLE + "_" + TAG_TABLE;
    public final static String COL_MEDIA_TAG_MEDIA_ID = MEDIA_TABLE + "_" + COL_ID;
    public final static String COL_MEDIA_TAG_TAG_ID = TAG_TABLE + "_" + COL_ID;

    public final static String AUTHOR_TABLE = "author";
    public final static String COL_AUTHOR_ID = COL_ID;
    public final static String COL_AUTHOR_NAME = COL_NAME;

    public final static String COL_MEDIA_TAGS_GROUPED_ALIAS = "tags";

    public final static String COL_MEDIA_ID_ALIAS = MEDIA_TABLE + "_" + COL_MEDIA_ID;
    public final static String COL_MEDIA_NAME_ALIAS = MEDIA_TABLE + "_" + COL_NAME;
    public final static String COL_MEDIA_FILENAME_ALIAS = MEDIA_TABLE + "_" + COL_MEDIA_FILENAME;
    public final static String COL_MEDIA_LINK_ALIAS = MEDIA_TABLE + "_" + COL_MEDIA_LINK;

    public final static String COL_FILEPATH_NAME_ALIAS = FILEPATH_TABLE + "_" + COL_FILEPATH_NAME;

    public final static String COL_TAG_NAME_ALIAS = TAG_TABLE + "_" + COL_TAG_NAME;
    public final static String COL_MEDIA_TAG_ID_ALIAS = MEDIA_TABLE + "_" + TAG_TABLE + "_" + COL_TAG_ID;

    public final static String COL_AUTHOR_NAME_ALIAS = AUTHOR_TABLE + "_" + COL_AUTHOR_NAME;

    public final static Map<String, String> columns = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put(COL_MEDIA_ID_ALIAS,
                MEDIA_TABLE + "." + COL_MEDIA_ID);
        put(COL_MEDIA_NAME_ALIAS,
                MEDIA_TABLE + "." + COL_MEDIA_NAME);
        put(COL_MEDIA_FILENAME_ALIAS,
                MEDIA_TABLE + "." + COL_MEDIA_FILENAME);
        put(COL_MEDIA_LINK_ALIAS,
                MEDIA_TABLE + "." + COL_MEDIA_LINK);

        put(COL_FILEPATH_NAME_ALIAS,
                FILEPATH_TABLE + "." + COL_FILEPATH_NAME);

        put(COL_AUTHOR_NAME_ALIAS,
                AUTHOR_TABLE + "." + COL_AUTHOR_NAME);

        put(COL_TAG_NAME_ALIAS,
                TAG_TABLE + "." + COL_TAG_NAME);

        put(COL_MEDIA_TAGS_GROUPED_ALIAS,
                "group_concat(" + TAG_TABLE + "." + COL_TAG_NAME + ", \" \")");

        put(COL_MEDIA_TAG_ID_ALIAS,
                MEDIA_TAG_TABLE + "." + COL_MEDIA_TAG_TAG_ID);
    }});

    public final static String AUTHOR_JOIN = "JOIN " + AUTHOR_TABLE + " ON " +
            AUTHOR_TABLE + "." + COL_AUTHOR_ID + " = " + MEDIA_TABLE + "." + COL_MEDIA_AUTHOR_ID + " ";

    private final static String FILEPATH_JOIN = "JOIN " + FILEPATH_TABLE + " ON " +
            FILEPATH_TABLE + "." + COL_FILEPATH_ID + " = " + MEDIA_TABLE + "." + COL_MEDIA_FILEPATH_ID + " ";

    private final static String TAG_JOIN_ALL = "LEFT OUTER JOIN " + MEDIA_TAG_TABLE + " ON " +
            MEDIA_TABLE + "." + COL_MEDIA_ID + " = " + MEDIA_TAG_TABLE + "." + COL_MEDIA_TAG_MEDIA_ID + " " +
            "LEFT OUTER JOIN " + TAG_TABLE + " ON " +
            MEDIA_TAG_TABLE + "." + COL_MEDIA_TAG_TAG_ID + " = " + TAG_TABLE + "." + COL_TAG_ID + " ";

    private final static String TAG_JOIN = "LEFT JOIN " + MEDIA_TAG_TABLE + " ON " +
            MEDIA_TABLE + "." + COL_MEDIA_ID + " = " + MEDIA_TAG_TABLE + "." + COL_MEDIA_TAG_MEDIA_ID + " ";

    public final static String[] DROP_INDEX_QUERIES = new String[]{
            "DROP INDEX IF EXISTS " + TAG_TABLE + "_" + COL_TAG_NAME + "_index;",
            "DROP INDEX IF EXISTS " + MEDIA_TABLE + "_" + COL_MEDIA_FILENAME + "_index;",
            "DROP INDEX IF EXISTS " + MEDIA_TAG_TABLE + "_" + MEDIA_TAG_TABLE + "_id_index;",
            "DROP INDEX IF EXISTS " + AUTHOR_TABLE + "_" + COL_AUTHOR_NAME + "_index;"
    };
    public final static String[] CREATE_INDEX_QUERIES = new String[]{
            "CREATE UNIQUE INDEX " + TAG_TABLE + "_" + COL_TAG_NAME + "_index ON " + TAG_TABLE + "(" + COL_TAG_NAME + ");",
            "CREATE INDEX " + MEDIA_TABLE + "_" + COL_MEDIA_FILENAME + "_index ON " + MEDIA_TABLE + "(" + COL_MEDIA_FILENAME + ");",
            "CREATE UNIQUE INDEX " + MEDIA_TAG_TABLE + "_" + MEDIA_TAG_TABLE + "_id_index ON " + MEDIA_TAG_TABLE + "(" + COL_MEDIA_TAG_MEDIA_ID + ", " + COL_MEDIA_TAG_TAG_ID + ");",
            "CREATE UNIQUE INDEX " + AUTHOR_TABLE + "_" + COL_AUTHOR_NAME + "_index ON " + AUTHOR_TABLE + "(" + COL_AUTHOR_NAME + ");"
    };

    public final static String BASE_QUERY = "SELECT " + MEDIA_TABLE + "." + COL_MEDIA_ID + " AS " + COL_MEDIA_ID_ALIAS + ", " +
            MEDIA_TABLE + "." + COL_MEDIA_FILENAME + " AS " + COL_MEDIA_FILENAME_ALIAS + ", " +
            FILEPATH_TABLE + "." + COL_FILEPATH_NAME + " AS " + COL_FILEPATH_NAME_ALIAS + " " +
            "FROM " + MEDIA_TABLE + " ";

    public MyOpenHelper(Context ctx, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(ctx, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + MEDIA_TABLE + " (" +
                COL_MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MEDIA_NAME + " VARCHAR(255), " +
                COL_MEDIA_FILENAME + " TEXT, " +
                COL_MEDIA_FILEPATH_ID + " INTEGER, " +
                COL_MEDIA_AUTHOR_ID + " INTEGER, " +
                COL_MEDIA_LINK + " VARCHAR(255), " +
                COL_MEDIA_INDEXED + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY (" + COL_MEDIA_AUTHOR_ID + ") REFERENCES " + AUTHOR_TABLE + "(" + COL_ID + "), " +
                "FOREIGN KEY (" + COL_MEDIA_FILEPATH_ID + ") REFERENCES " + FILEPATH_TABLE + "(" + COL_ID + "));");
        db.execSQL("CREATE TABLE " + TAG_TABLE + " (" +
                COL_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TAG_NAME + " VARCHAR(100) NOT NULL UNIQUE);");
        db.execSQL("CREATE TABLE " + MEDIA_TAG_TABLE + " (" +
                COL_MEDIA_TAG_MEDIA_ID + " INTEGER NOT NULL, " +
                COL_MEDIA_TAG_TAG_ID + " INTEGER NOT NULL, " +
                "PRIMARY KEY(" + COL_MEDIA_TAG_MEDIA_ID + ", " + COL_MEDIA_TAG_TAG_ID + "), " +
                "FOREIGN KEY(" + COL_MEDIA_TAG_MEDIA_ID + ") REFERENCES " + MEDIA_TABLE + "(" + COL_MEDIA_ID + "), " +
                "FOREIGN KEY(" + COL_MEDIA_TAG_TAG_ID + ") REFERENCES " + TAG_TABLE + "(" + COL_TAG_ID + "));");
        db.execSQL("CREATE TABLE " + AUTHOR_TABLE + " (" +
                COL_AUTHOR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_AUTHOR_NAME + " VARCHAR(100));");
        db.execSQL("CREATE TABLE " + FILEPATH_TABLE + " (" +
                COL_FILEPATH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FILEPATH_NAME + " TEXT NOT NULL);");
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
        db.execSQL("DROP TABLE IF EXISTS " + FILEPATH_TABLE + ";");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS " + MEDIA_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + MEDIA_TAG_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + AUTHOR_TABLE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + FILEPATH_TABLE + ";");
        onCreate(db);
    }

    /**
     * Returns a query used to select all media all media.
     *
     * @return the query
     */
    public static String getAllMediaQuery() {
        String query = "SELECT " +
                MEDIA_TABLE + "." + COL_MEDIA_ID + " AS " + COL_MEDIA_ID_ALIAS + ", " +
                MEDIA_TABLE + "." + COL_MEDIA_FILENAME + " AS " + COL_MEDIA_FILENAME_ALIAS + ", " +
                FILEPATH_TABLE + "." + COL_FILEPATH_NAME + " AS " + COL_FILEPATH_NAME_ALIAS + ", " +
                MEDIA_TABLE + "." + COL_MEDIA_NAME + " AS " + COL_MEDIA_NAME_ALIAS + ", " +
                AUTHOR_TABLE + "." + COL_AUTHOR_NAME + " AS " + COL_AUTHOR_NAME_ALIAS + ", " +
                MEDIA_TABLE + "." + COL_MEDIA_LINK + " AS " + COL_MEDIA_LINK_ALIAS + ", " +
                columns.get(COL_MEDIA_TAGS_GROUPED_ALIAS) + " AS " + COL_MEDIA_TAGS_GROUPED_ALIAS + " ";

        query += "FROM " + MEDIA_TABLE + " ";

        query += FILEPATH_JOIN;
        query += AUTHOR_JOIN;
        query += TAG_JOIN_ALL;

        query += "GROUP BY (" + columns.get(COL_MEDIA_FILENAME_ALIAS) + ") ";

        query += "ORDER BY " + MEDIA_TABLE + "." + COL_MEDIA_FILENAME + " ASC";

        return query;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
    }

    /**
     * Gets all authors in the database.
     *
     * @param db a readable SQLite database
     * @return a hashset of authors
     */
    public synchronized Set<String> getAuthorSet(SQLiteDatabase db) {
        Set<String> authorList = new HashSet<>();
        Cursor c = db.rawQuery("SELECT " +
                MyOpenHelper.AUTHOR_TABLE + "." + MyOpenHelper.COL_AUTHOR_NAME + " " +
                "FROM " + MyOpenHelper.AUTHOR_TABLE, null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                authorList.add(c.getString(0));
                c.moveToNext();
            }
        }
        c.close();
        return authorList;
    }

    /**
     * Gets the provided author's id from the database or inserts if the author does not exist.
     *
     * @param db     a writable SQLite database
     * @param author the author's name
     * @return the id of the author
     */
    public synchronized int getAuthorIdOrInsert(SQLiteDatabase db, String author) {
        Cursor authorCursor = db.rawQuery("SELECT " + COL_AUTHOR_ID + " " +
                "FROM " + AUTHOR_TABLE + " " +
                "WHERE " + COL_AUTHOR_NAME + " LIKE ? " +
                "LIMIT 1;", new String[]{author});
        int authorId;
        //If author does not exist, insert author into database
        if (authorCursor.getCount() == 0) {
            authorCursor.close();
            ContentValues cv = new ContentValues();
            cv.put(COL_AUTHOR_NAME, author);
            authorId = (int) db.insert(AUTHOR_TABLE, null, cv);
        } else {
            authorCursor.moveToFirst();
            authorId = (int) authorCursor.getLong(authorCursor.getColumnIndex(COL_AUTHOR_ID));
            authorCursor.close();
        }
        return authorId;
    }

    public synchronized int getAuthorId(SQLiteDatabase db, String author) {
        Cursor authorCursor = db.rawQuery("SELECT " + COL_AUTHOR_ID + " " +
                "FROM " + AUTHOR_TABLE + " " +
                "WHERE " + COL_AUTHOR_NAME + " LIKE ? " +
                "LIMIT 1;", new String[]{author});
        int authorId;
        if (authorCursor.getCount() == 0) {
            authorId = -1;
        } else {
            authorCursor.moveToFirst();
            authorId = (int) authorCursor.getLong(authorCursor.getColumnIndex(COL_AUTHOR_ID));
        }
        authorCursor.close();
        return authorId;
    }

    /**
     * Gets the provided tag's id from the database, or inserts it if it does not exist.
     *
     * @param db  a writable SQLite database
     * @param tag the tag name
     * @return the id of the tag
     */
    public synchronized int getTagIdOrInsert(SQLiteDatabase db, String tag) {
        Cursor tagCursor = db.rawQuery("SELECT " + COL_TAG_ID + " " +
                "FROM " + TAG_TABLE + " " +
                "WHERE " + COL_TAG_NAME + " = ? " +
                "LIMIT 1;", new String[]{tag});
        int tagId;
        //If tag does not exist, insert tag into database
        if (tagCursor.getCount() == 0) {
            tagCursor.close();
            ContentValues cv = new ContentValues();
            cv.put(COL_TAG_NAME, tag);
            tagId = (int) db.insert(TAG_TABLE, null, cv);
        } else {
            tagCursor.moveToFirst();
            tagId = (int) tagCursor.getLong(tagCursor.getColumnIndex(COL_TAG_ID));
            tagCursor.close();
        }
        return tagId;
    }

    /**
     * Gets the provided tag's id from the database, as well as any similar tags.
     *
     * @param db  a readable SQLite database
     * @param tag the tag name
     * @return a set containing the ids of the similar tags
     */
    public synchronized Set<String> getSimilarTagIds(SQLiteDatabase db, String tag) {
        Cursor tagCursor = db.rawQuery("SELECT " + COL_TAG_ID + " " +
                        "FROM " + TAG_TABLE + " " +
                        "WHERE " + COL_TAG_NAME + " LIKE ?;",
                new String[]{"%" + tag + "%"});
        Set<String> tagIds = new HashSet<>();
        //If tag does not exist, insert tag into database
        if (tagCursor.getCount() != 0) {
            while (tagCursor.moveToNext()) {
                tagIds.add(String.valueOf(tagCursor.getLong(tagCursor.getColumnIndex(COL_TAG_ID))));
            }
        }
        tagCursor.close();
        return tagIds;
    }

    /**
     * Gets the provided tag's id from the database.
     *
     * @param db  a readable SQLite database
     * @param tag the tag name
     * @return the tag id or -1 if it does not exist
     */
    public synchronized int getTagId(SQLiteDatabase db, String tag) {
        Cursor tagCursor = db.rawQuery("SELECT " + COL_TAG_ID + " " +
                "FROM " + TAG_TABLE + " " +
                "WHERE " + COL_TAG_NAME + " = ? " +
                "LIMIT 1;", new String[]{tag});
        int tagId;
        if (tagCursor.getCount() == 0) {
            tagId = -1;
        } else {
            tagCursor.moveToFirst();
            tagId = (int) tagCursor.getLong(tagCursor.getColumnIndex(COL_TAG_ID));
        }
        tagCursor.close();
        return tagId;
    }

    /**
     * Inserts the provided media into the database and returns its id.
     *
     * @param db    a writable SQLite database
     * @param media the media to insert
     * @return the id of the inserted media, or -1 if an error occurred
     */
    public synchronized int insertMedia(SQLiteDatabase db, Media media) {
        int authorId = getAuthorIdOrInsert(db, media.getAuthor());

        ContentValues cv = new ContentValues();
        long filePathId = getFilepathIdOrInsert(db, media.getFilePath());

        cv.put(COL_MEDIA_FILEPATH_ID, filePathId);
        cv.put(COL_MEDIA_NAME, media.getName());
        cv.put(COL_MEDIA_FILENAME, media.getFilePath().substring(media.getFilePath().lastIndexOf("/") + 1));
        cv.put(COL_MEDIA_AUTHOR_ID, authorId);
        cv.put(COL_MEDIA_LINK, media.getLink());
        int imageId = (int) db.insert(MEDIA_TABLE, null, cv);
        cv.clear();

        if (media.getTags() != null) {
            for (String tag : media.getTags()) {
                int tagId = getTagIdOrInsert(db, tag);
                cv.put(COL_MEDIA_TAG_MEDIA_ID, imageId);
                cv.put(COL_MEDIA_TAG_TAG_ID, tagId);
                db.insert(MEDIA_TAG_TABLE, null, cv);
                cv.clear();
            }
        }
        return imageId;
    }

    public synchronized int getFilepathIdOrInsert(SQLiteDatabase db, String filePath) {
        int filePathId;
        Cursor c = db.rawQuery("SELECT " + COL_FILEPATH_ID + " " +
                        "FROM " + FILEPATH_TABLE + " " +
                        "WHERE " + FILEPATH_TABLE + "." + COL_FILEPATH_NAME + " = ?;",
                new String[]{filePath.substring(0, filePath.lastIndexOf("/") + 1)});
        if (c.getCount() <= 0) {
            ContentValues cv = new ContentValues();
            cv.put(COL_FILEPATH_NAME, filePath.substring(0, filePath.lastIndexOf("/") + 1));
            filePathId = (int) db.insert(FILEPATH_TABLE, null, cv);
        } else {
            c.moveToFirst();
            filePathId = (int) c.getLong(c.getColumnIndex(COL_FILEPATH_ID));
        }
        c.close();
        return filePathId;
    }

//    public synchronized ArrayList<Media> getMediaList(SQLiteDatabase db) {
//        ArrayList<Media> mediaList = new ArrayList<>();
//        Cursor c = db.rawQuery("SELECT ")
//    }

    /**
     * @param db    a writable SQLite database
     * @param media the media object with new values
     * @return true if the media changed in the database, false if an error occurred
     */
    public synchronized boolean updateMedia(SQLiteDatabase db, Media media) {
        String[] mediaId = new String[]{String.valueOf(media.getId())};
        ContentValues cv = new ContentValues();
        cv.put(COL_MEDIA_FILEPATH_ID, getFilepathIdOrInsert(db, media.getFilePath()));
        cv.put(COL_MEDIA_NAME, media.getName());
        cv.put(COL_MEDIA_FILENAME, media.getFilePath().substring(media.getFilePath().lastIndexOf("/") + 1));
        cv.put(COL_MEDIA_LINK, media.getLink());
        cv.put(COL_MEDIA_INDEXED, 1);
        cv.put(COL_MEDIA_AUTHOR_ID, getAuthorIdOrInsert(db, media.getAuthor()));
        boolean success = db.update(MEDIA_TABLE, cv, COL_MEDIA_ID + " = ?", mediaId) == 1;
        cv.clear();
        if (!success) {
            return false;
        }

        //Delete all existing media tags
        db.delete(MEDIA_TAG_TABLE, COL_MEDIA_TAG_MEDIA_ID + " = ?", mediaId);
        if (media.getTags() != null) {
            for (String tag : media.getTags()) {
                int tagId = getTagIdOrInsert(db, tag);
                cv.put(COL_MEDIA_TAG_MEDIA_ID, media.getId());
                cv.put(COL_MEDIA_TAG_TAG_ID, tagId);
                success = db.insert(MEDIA_TAG_TABLE, null, cv) != -1;
                cv.clear();
                if (!success) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Wrapper for parseMediaFromCursor for a list of media.
     *
     * @param c a cursor for the media table
     * @return a arraylist of media created from all rows in the cursor
     * @see #parseMediaFromCursor(Cursor)
     */
    public synchronized ArrayList<Media> parseMediaListFromCursor(Cursor c) {
        ArrayList<Media> mediaList = new ArrayList<>();
        Media.Builder media;
        while (!c.isAfterLast()) {
            media = parseMediaFromCursor(c);
            if (media != null) { //making sure the cursor is not after last
                mediaList.add(media.build());
            }
        }
        return mediaList;
    }

    /**
     * Parses a media object from the next row in the provided cursor. Dynamically sets the
     * media object's values based on the columns selected in the cursor.
     *
     * @param c a cursor for the media table
     * @return a media builder, or null if the cursor is after last row
     */
    public Media.Builder parseMediaFromCursor(Cursor c) {
        String[] columns = c.getColumnNames();
        if (c.moveToNext() && !c.isAfterLast()) {
            Media.Builder mediaBuilder = new Media.Builder();
            for (String column : columns) {
                switch (column) {
                    case COL_MEDIA_ID_ALIAS:
                        mediaBuilder.id(c.getInt(c.getColumnIndex(COL_MEDIA_ID_ALIAS)));
                        break;
                    case COL_MEDIA_NAME_ALIAS:
                        mediaBuilder.name(c.getString(c.getColumnIndex(COL_MEDIA_NAME_ALIAS)));
                        break;
                    case COL_FILEPATH_NAME_ALIAS:
                        mediaBuilder.filePath(c.getString(c.getColumnIndex(COL_FILEPATH_NAME_ALIAS)));
                        break;
                    case COL_MEDIA_FILENAME_ALIAS:
                        mediaBuilder.fileName(c.getString(c.getColumnIndex(COL_MEDIA_FILENAME_ALIAS)));
                        break;
                    case COL_AUTHOR_NAME_ALIAS:
                        mediaBuilder.author(c.getString(c.getColumnIndex(COL_AUTHOR_NAME_ALIAS)));
                        break;
                    case COL_MEDIA_LINK_ALIAS:
                        mediaBuilder.link(c.getString(c.getColumnIndex(COL_MEDIA_LINK_ALIAS)));
                        break;
                    case COL_MEDIA_TAGS_GROUPED_ALIAS:
                        String[] tags;
                        String tagString = c.getString(c.getColumnIndex(COL_MEDIA_TAGS_GROUPED_ALIAS));
                        if (tagString != null) {
                            tags = tagString.split(" ");
                            mediaBuilder.tags(new HashSet<>(Arrays.asList(tags)));
                        }
                        break;
                }
            }
            //System.out.println(media.toString());
            return mediaBuilder;
        }
        return null; //if cursor is after last
    }

    /**
     * To get additional data for one media. Takes the media id, returns a completed Media object.
     *
     * @param db a readable SQLite database
     * @param id the id of the media
     * @return the media with all values from the database
     */
    public synchronized Media depthMediaQuery(SQLiteDatabase db, long id) {

        String query = "SELECT " +
                MEDIA_TABLE + "." + COL_MEDIA_FILENAME + " AS " + COL_MEDIA_FILENAME_ALIAS + ", " +
                FILEPATH_TABLE + "." + COL_FILEPATH_NAME + " AS " + COL_FILEPATH_NAME_ALIAS + ", " +
                MEDIA_TABLE + "." + COL_MEDIA_NAME + " AS " + COL_MEDIA_NAME_ALIAS + ", " +
                AUTHOR_TABLE + "." + COL_AUTHOR_NAME + " AS " + COL_AUTHOR_NAME_ALIAS + ", " +
                MEDIA_TABLE + "." + COL_MEDIA_LINK + " AS " + COL_MEDIA_LINK_ALIAS + ", " +
                columns.get(COL_MEDIA_TAGS_GROUPED_ALIAS) + " AS " + COL_MEDIA_TAGS_GROUPED_ALIAS + " ";

        query += "FROM " + MEDIA_TABLE + " ";

        query += FILEPATH_JOIN;
        query += AUTHOR_JOIN;
        query += TAG_JOIN_ALL;

        query += "WHERE " + MEDIA_TABLE + "." + COL_MEDIA_ID + " = ? ";

        query += "GROUP BY (" + columns.get(COL_MEDIA_FILENAME_ALIAS) + ") ";

        query += "LIMIT 1";

        //System.out.println(query);

        Cursor c = db.rawQuery(query, new String[]{String.valueOf(id)});
        Media.Builder media = parseMediaFromCursor(c);
        c.close();
        if (media == null) {
            return null;
        }
        media.id((int) id);
        return media.build();
    }

    public synchronized Set<String> getMediaPaths(SQLiteDatabase db) {
        String query = "SELECT " +
                MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME + " AS " + MyOpenHelper.COL_MEDIA_FILENAME_ALIAS + ", " +
                MyOpenHelper.FILEPATH_TABLE + "." + MyOpenHelper.COL_FILEPATH_NAME + " AS " + MyOpenHelper.COL_FILEPATH_NAME_ALIAS + " ";

        query += "FROM " + MyOpenHelper.MEDIA_TABLE + " ";

        query += FILEPATH_JOIN;

        Cursor c = db.rawQuery(query, new String[]{});
        Set<String> allMediaPaths = new HashSet<>();
        while (c.moveToNext()) {
            allMediaPaths.add(c.getString(c.getColumnIndex(COL_FILEPATH_NAME_ALIAS)) + c.getString(c.getColumnIndex(COL_MEDIA_FILENAME_ALIAS)));
        }
        c.close();
        return allMediaPaths;
    }
}
