package ca.quadrexium.velonathea.database;

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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ca.quadrexium.velonathea.pojo.Constants;
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

    public final static String COL_MEDIA_TAGS_GROUPED_ALIAS = "tags";
    public final static String COL_AUTHOR_NAME_FOREIGN = AUTHOR_TABLE + "_" + COL_AUTHOR_NAME;

    public final static String COL_MEDIA_ID_ALIAS = MEDIA_TABLE + "_" + COL_MEDIA_ID;
    public final static String COL_MEDIA_NAME_ALIAS = MEDIA_TABLE + "_" + COL_NAME;
    public final static String COL_MEDIA_FILENAME_ALIAS = MEDIA_TABLE + "_" + COL_MEDIA_FILENAME;
    public final static String COL_MEDIA_LINK_ALIAS = MEDIA_TABLE + "_" + COL_MEDIA_LINK;

    public final static String COL_TAG_ID_ALIAS = TAG_TABLE + "_" + COL_TAG_ID;
    public final static String COL_TAG_NAME_ALIAS = TAG_TABLE + "_" + COL_TAG_NAME;

    public final static String COL_AUTHOR_NAME_ALIAS = AUTHOR_TABLE + "_" + COL_AUTHOR_NAME;

    public final static String COL_MEDIA_TAGS_GROUPED = "group_concat(" + TAG_TABLE + "." + COL_TAG_NAME + ", \" \")";

    private final static Map<String, String> columns = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put(COL_MEDIA_ID_ALIAS, MEDIA_TABLE + "." + COL_MEDIA_ID);
        put(COL_MEDIA_NAME_ALIAS, MEDIA_TABLE + "." + COL_MEDIA_NAME);
        put(COL_MEDIA_FILENAME_ALIAS, MEDIA_TABLE + "." + COL_MEDIA_FILENAME);
        put(COL_MEDIA_LINK_ALIAS, MEDIA_TABLE + "." + COL_MEDIA_LINK);

        put(COL_AUTHOR_NAME_ALIAS, AUTHOR_TABLE + "." + COL_AUTHOR_NAME);

        put(COL_TAG_NAME_ALIAS, TAG_TABLE + "." + COL_TAG_NAME);

        put(COL_MEDIA_TAGS_GROUPED_ALIAS, "group_concat(" + TAG_TABLE + "." + COL_TAG_NAME + ", \" \")");
    }});

    private final String AUTHOR_JOIN = "JOIN " + AUTHOR_TABLE + " ON " +
            AUTHOR_TABLE + "." + COL_AUTHOR_ID + " = " + MEDIA_TABLE + "." + COL_MEDIA_AUTHOR_ID + " ";

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
        if (!oldMedia.equals(newMedia)) {
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
        Media media;
        while (!c.isAfterLast()) {
            media = parseMediaFromCursor(c);
            if (media != null) {
                mediaList.add(media);
            }
        }
        return mediaList;
    }

    public Media parseMediaFromCursor(Cursor c) {
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
                        String[] tags = c.getString(c.getColumnIndex(COL_MEDIA_TAGS_GROUPED_ALIAS)).split(" ");
                        mediaBuilder.tags(new ArrayList<>(Arrays.asList(tags)));
                }
            }
            return mediaBuilder.build();
        }
        return null;
    }

    //Use the code from this view to select image tags.
//    db.execSQL("CREATE VIEW v_image_tags AS " +
//                "SELECT image.*, group_concat(tag.name, \" \") AS tags FROM image " +
//                "JOIN image_tag ON image.id = image_tag.image_id " +
//                "JOIN tag ON tag.id = image_tag.tag_id " +
//                "GROUP BY image.file_name;");

    //For when a given media has some null values, eg the link, returns the media object with those
    // values set
    public synchronized Media getRemainingData(SQLiteDatabase db, Media media) throws Exception {
        Set<String> selectedColumns = new LinkedHashSet<>();
        if (Constants.isStringEmpty(media.getName())) {
            selectedColumns.add(COL_MEDIA_NAME_ALIAS);
        }
        if (Constants.isStringEmpty(media.getLink())) {
            selectedColumns.add(COL_MEDIA_LINK_ALIAS);
        }
        if (Constants.isStringEmpty(media.getAuthor())) {
            selectedColumns.add(COL_AUTHOR_NAME_ALIAS);
        }
        if (selectedColumns.size() == 0) {
            return media;
        }
        TreeMap<String, String[]> whereFilters = new TreeMap<>();
        whereFilters.put(MEDIA_TABLE + "." + COL_MEDIA_ID, new String[] { String.valueOf( media.getId() )});
        Media newMedia = mediaQuery(db, selectedColumns, whereFilters, null, 1).get(0);
        return mergeMedia(media, newMedia);
    }

    public Media mergeMedia(Media oldMedia, Media newMedia) {
        if (Constants.isStringEmpty(oldMedia.getName())/* || !newMedia.getName().equals(oldMedia.getName())*/) {
            oldMedia.setName(newMedia.getName());
        }
        if (Constants.isStringEmpty(oldMedia.getAuthor())) {
            oldMedia.setAuthor(newMedia.getAuthor());
        }
        if (Constants.isStringEmpty(oldMedia.getLink())) {
            oldMedia.setLink(newMedia.getLink());
        }
        return oldMedia;
    }

//    public synchronized boolean writeMediaToFile(SQLiteDatabase db, String fileName, boolean withIds) {
//        ArrayList<Media> mediaList = getMediaList(db, new TreeMap<>(), new String[] { MEDIA_TABLE + "." + COL_MEDIA_FILENAME + " ASC"});
//        StringBuilder mediaListAsString = new StringBuilder();
//        for (Media media : mediaList) {
//            if (withIds) {
//                mediaListAsString.append(media.getId()).append("\t");
//            }
//            mediaListAsString.append(media.getFileName()).append("\t");
//            mediaListAsString.append(media.getName()).append("\t");
//            mediaListAsString.append(media.getAuthor()).append("\t");
//            mediaListAsString.append(media.getLink()).append("\t");
//            mediaListAsString.append(media.getTagsAsString()).append("\t");
//            mediaListAsString.append("\n");
//        }
//        try {
//            FileWriter myWriter = new FileWriter(fileName);
//            myWriter.write(mediaListAsString.toString());
//            myWriter.close();
//            return true;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    public ArrayList<Media> mediaQuery(SQLiteDatabase db, Set<String> selectedColumns,
                                        TreeMap<String, String[]> whereFilters,
                                        String[] orderBy, long limit) throws Exception {
        ArrayList<String> selectionArgs = new ArrayList<>();
        selectedColumns.add(COL_MEDIA_ID_ALIAS);
        selectedColumns.add(COL_MEDIA_FILENAME_ALIAS);
        StringBuilder query = new StringBuilder("SELECT ");
        String[] selectedColumnsAsArray = new String[selectedColumns.size()];
        selectedColumnsAsArray = selectedColumns.toArray(selectedColumnsAsArray);
        String lastColumn = selectedColumnsAsArray[(selectedColumnsAsArray.length-1)];
        HashSet<String> joins = new HashSet<>();
        for (String columnAlias : selectedColumns) {
            String column = columns.get(columnAlias);
            if (column == null) {
                throw new Exception("Must use aliases to select columns");
            } else {
                int startOfTableName = column.contains("(") ? column.indexOf("(")+1 : 0;
                String tableName = column.substring(startOfTableName, column.indexOf("."));
                switch (tableName) {
                    case Constants.PREFS_MEDIA_AUTHOR:
                        joins.add(AUTHOR_JOIN);
                        break;
                    case Constants.PREFS_MEDIA_TAG:
                        joins.add(TAG_JOIN);
                        break;
                }
            }
            query.append(column).append(" AS ").append(columnAlias);
            if (!columnAlias.equals(lastColumn)) {
                query.append(",");
            }
            query.append(" ");
        }
        query.append("FROM " + MEDIA_TABLE + " ");
        for (String join : joins) {
            query.append(join);
        }
        //Creates the where clause
        //whereFilters values can be an arraylist with one or more values. If there is only one:
        // "WHERE (column LIKE value) AND (column2 LIKE value2)"
        // If there are multiple:
        // "WHERE (column LIKE valueA OR column LIKE valueB) AND (column2 LIKE value2)
        if (whereFilters.size() != 0) {
            query.append("WHERE ");
            for (Map.Entry<String, String[]> entry : whereFilters.entrySet()) {
                //For filtering when the column value may match multiple values (OR clause)
                query.append("(");
                for (int i = 0; i < entry.getValue().length; i++) {
                    query.append(entry.getKey()).append(" LIKE ?");
                    selectionArgs.add("%" + entry.getValue()[i] + "%");
                    if (entry.getValue().length > 1 && i != entry.getValue().length-1) {
                        query.append(" OR ");
                    }
                }
                query.append(") ");
                //For filtering other columns, append AND (AND clause)
                if (!entry.equals(whereFilters.lastEntry())) {
                    query.append("AND ");
                }
            }
        }
        query.append("GROUP BY (").append(columns.get(COL_MEDIA_FILENAME_ALIAS)).append(") ");
        if (orderBy != null && orderBy.length != 0) {
            query.append("ORDER BY ");
            for (int i = 0; i < orderBy.length; i++) {
                query.append(orderBy[i]);
                if (i != orderBy.length-1) {
                    query.append(", ");
                }
            }
            query.append(" ");
        }
        if (limit != 0) {
            query.append("LIMIT ").append(limit);
        }

        String[] stringSelectionArgs = selectionArgs.toArray(new String[0]);
        //System.out.println(query.toString());
        Cursor c = db.rawQuery(query.toString(), stringSelectionArgs);
        ArrayList<Media> mediaList = parseMediaListFromCursor(c);
        c.close();
        return mediaList;
    }
}
