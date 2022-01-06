package ca.quadrexium.velonathea.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    public final static String COL_MEDIA_ID_ALIAS = MEDIA_TABLE + "_" + COL_MEDIA_ID;
    public final static String COL_MEDIA_NAME_ALIAS = MEDIA_TABLE + "_" + COL_NAME;
    public final static String COL_MEDIA_FILENAME_ALIAS = MEDIA_TABLE + "_" + COL_MEDIA_FILENAME;
    public final static String COL_MEDIA_LINK_ALIAS = MEDIA_TABLE + "_" + COL_MEDIA_LINK;

    public final static String COL_TAG_NAME_ALIAS = TAG_TABLE + "_" + COL_TAG_NAME;

    public final static String COL_AUTHOR_NAME_ALIAS = AUTHOR_TABLE + "_" + COL_AUTHOR_NAME;

    private final static Map<String, String> columns = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put(COL_MEDIA_ID_ALIAS,
                MEDIA_TABLE + "." + COL_MEDIA_ID);
        put(COL_MEDIA_NAME_ALIAS,
                MEDIA_TABLE + "." + COL_MEDIA_NAME);
        put(COL_MEDIA_FILENAME_ALIAS,
                MEDIA_TABLE + "." + COL_MEDIA_FILENAME);
        put(COL_MEDIA_LINK_ALIAS,
                MEDIA_TABLE + "." + COL_MEDIA_LINK);

        put(COL_AUTHOR_NAME_ALIAS,
                AUTHOR_TABLE + "." + COL_AUTHOR_NAME);

        put(COL_TAG_NAME_ALIAS,
                TAG_TABLE + "." + COL_TAG_NAME);

        put(COL_MEDIA_TAGS_GROUPED_ALIAS,
                "group_concat(" + TAG_TABLE + "." + COL_TAG_NAME + ", \" \")");
    }});

    private final String AUTHOR_JOIN = "JOIN " + AUTHOR_TABLE + " ON " +
            AUTHOR_TABLE + "." + COL_AUTHOR_ID + " = " + MEDIA_TABLE + "." + COL_MEDIA_AUTHOR_ID + " ";

    private final String TAG_JOIN = "LEFT OUTER JOIN " + MEDIA_TAG_TABLE + " ON " +
            MEDIA_TABLE + "." + COL_MEDIA_ID + " = " + MEDIA_TAG_TABLE + "." + COL_MEDIA_TAG_MEDIA_ID + " " +
            "LEFT OUTER JOIN " + TAG_TABLE + " ON " +
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
                COL_MEDIA_NAME + " VARCHAR(255), " +
                COL_MEDIA_FILENAME + " VARCHAR(255), " +
                COL_MEDIA_AUTHOR_ID + " INTEGER, " +
                COL_MEDIA_LINK + " VARCHAR(255), " +
                "FOREIGN KEY (" + COL_MEDIA_AUTHOR_ID + ") REFERENCES " + AUTHOR_TABLE + "(" + COL_ID + "));");
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
    public void onOpen(SQLiteDatabase db) { }

    /**
     * Gets the provided author's id from the database or inserts if the author does not exist.
     * @param db a writable SQLite database
     * @param author the author's name
     * @return the id of the author
     */
    public synchronized int getAuthorIdOrInsert(SQLiteDatabase db, String author) {
        Cursor authorCursor = db.rawQuery("SELECT " + COL_AUTHOR_ID + " " +
                "FROM " + AUTHOR_TABLE +" " +
                "WHERE " + COL_AUTHOR_NAME + " LIKE ? " +
                "LIMIT 1;", new String[]{ author });
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

    /**
     * Gets the provided tag's id from the database, or inserts it if it does not exist.
     * @param db a writable SQLite database
     * @param tag the tag name
     * @return the id of the tag
     */
    public synchronized int getTagIdOrInsert(SQLiteDatabase db, String tag) {
        Cursor tagCursor = db.rawQuery("SELECT " + COL_TAG_ID + " " +
                "FROM " + TAG_TABLE + " " +
                "WHERE " + COL_TAG_NAME + " = ? " +
                "LIMIT 1;", new String[]{ tag } );
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
     * Inserts the provided media into the database and returns its id.
     * @param db a writable SQLite database
     * @param media the media to insert
     * @return the id of the inserted media, or -1 if an error occured
     */
    public synchronized int insertMedia(SQLiteDatabase db, Media media) {
        int authorId = getAuthorIdOrInsert(db, media.getAuthor());

        ContentValues cv = new ContentValues();
        cv.put(COL_MEDIA_FILENAME, media.getFileName());
        cv.put(COL_MEDIA_NAME, media.getName());
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

    /**
     * @param db a writable SQLite database
     * @param media the media object with new values
     * @return true if the media changed in the database, false if an error occured
     */
    public synchronized boolean updateMedia(SQLiteDatabase db, Media media) {
        String[] mediaId = new String[] {String.valueOf(media.getId())};
        ContentValues cv = new ContentValues();
        cv.put(COL_MEDIA_FILENAME, media.getFileName());
        cv.put(COL_MEDIA_NAME, media.getName());
        cv.put(COL_MEDIA_LINK, media.getLink());
        cv.put(COL_MEDIA_AUTHOR_ID, getAuthorIdOrInsert(db, media.getAuthor()));
        boolean success = db.update(MEDIA_TABLE, cv, COL_MEDIA_ID + " = ?", mediaId) == 1;
        cv.clear();
        if (!success) { return false; }

        //Delete all existing media tags
        db.delete(MEDIA_TAG_TABLE, COL_MEDIA_TAG_MEDIA_ID + " = ?", mediaId);
        if (media.getTags() != null) {
            for (String tag : media.getTags()) {
                int tagId = getTagIdOrInsert(db, tag);
                cv.put(COL_MEDIA_TAG_MEDIA_ID, media.getId());
                cv.put(COL_MEDIA_TAG_TAG_ID, tagId);
                success = db.insert(MEDIA_TAG_TABLE, null, cv) != -1;
                cv.clear();
                if (!success) { return false; }
            }
        }
        return true;
    }

    /**
     * @param c a cursor for the media table
     * @return a arraylist of media created from all rows in the cursor
     */
    public synchronized ArrayList<Media> parseMediaListFromCursor(Cursor c) {
        ArrayList<Media> mediaList = new ArrayList<>();
        Media media;
        while (!c.isAfterLast()) {
            media = parseMediaFromCursor(c);
            if (media != null) { //making sure the cursor is not after last
                mediaList.add(media);
            }
        }
        return mediaList;
    }

    /**
     * Parses a media object from the next row in the provided cursor. Dynamically sets the
     *  media object's values based on the columns selected in the cursor.
     * @param c a cursor for the media table
     * @return a media object, or null if the cursor is after last row
     */
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
                        String[] tags;
                        String tagString = c.getString(c.getColumnIndex(COL_MEDIA_TAGS_GROUPED_ALIAS));
                        if (tagString != null) {
                            tags = tagString.split(" ");
                            mediaBuilder.tags(new HashSet<>(Arrays.asList(tags)));
                        }
                        break;
                }
            }
            Media media = mediaBuilder.build();
            //System.out.println(media.toString());
            return media;
        }
        return null; //if cursor is after last
    }

    /**
     * Method that takes a media object with unset values and returns the same media with
     *  those values set
     * @param db a readable SQLite database
     * @param media the media to get values for
     * @return the media with set values
     */
    public synchronized Media getRemainingData(SQLiteDatabase db, Media media) {
        //Find out what data needs to be selected
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
        if (media.getTags() == null || media.getTags().size() == 0) {
            selectedColumns.add(COL_MEDIA_TAGS_GROUPED_ALIAS);
        }

        //Make sure there is data to query
        if (selectedColumns.size() == 0) {
            return media;
        }

        //Select the exact same media
        Map<String, String[]> whereFilters = new HashMap<>();
        whereFilters.put(MEDIA_TABLE + "." + COL_MEDIA_ID, new String[]{String.valueOf(media.getId())});

        Media newMedia = mediaQuery(db, selectedColumns, whereFilters, null, 1).get(0);
        if (newMedia.equals(media)) {
            return newMedia;
        }
        return mergeMedia(media, newMedia);
    }

    /**
     * Merges two media together so that any unset values in oldMedia are set to their
     *  equivalent set values in newMedia.
     * @param oldMedia a media with unset values
     * @param newMedia a media with the unset values from oldMedia set
     * @return oldMedia with the unset values set to the ones from newMedia
     */
    public Media mergeMedia(Media oldMedia, Media newMedia) {
        if (Constants.isStringEmpty(oldMedia.getName())/* || !newMedia.getName().equals(oldMedia.getName())*/) {
            if (!Constants.isStringEmpty(newMedia.getName())) {
                oldMedia.setName(newMedia.getName());
            }
        }
        if (Constants.isStringEmpty(oldMedia.getAuthor())) {
            if (!Constants.isStringEmpty(newMedia.getAuthor())) {
                oldMedia.setAuthor(newMedia.getAuthor());
            }
        }
        if (Constants.isStringEmpty(oldMedia.getLink())) {
            if (!Constants.isStringEmpty(newMedia.getLink())) {
                oldMedia.setLink(newMedia.getLink());
            }
        }
        if (oldMedia.getTags() == null || oldMedia.getTags().size() == 0) {
            if (newMedia.getTags() != null && newMedia.getTags().size() > 0) {
                oldMedia.setTags(newMedia.getTags());
            }
        }
        return oldMedia;
    }

    /**
     * Writes all media to a text-delimited file.
     * @param db a readable SQLite database
     * @param filePath the path to write the file to
     * @return true if written, else false
     */
    public boolean writeMediaToFile(SQLiteDatabase db, String filePath) {
        Set<String> selectedColumns = new HashSet<>();
        selectedColumns.add(COL_MEDIA_FILENAME_ALIAS);
        selectedColumns.add(COL_MEDIA_NAME_ALIAS);
        selectedColumns.add(COL_AUTHOR_NAME_ALIAS);
        selectedColumns.add(COL_MEDIA_LINK_ALIAS);
        selectedColumns.add(COL_MEDIA_TAGS_GROUPED_ALIAS);
        ArrayList<Media> mediaList = mediaQuery(db, selectedColumns, new HashMap<>(),
                new String[] { MEDIA_TABLE + "." + COL_MEDIA_FILENAME + " ASC"}, 0);

        StringBuilder mediaListAsString = new StringBuilder();
        for (Media media : mediaList) {
            String name = media.getName() != null ? media.getName() : "";
            String author = media.getAuthor() != null ? media.getAuthor() : "";
            String link = media.getLink() != null ? media.getLink() : "";
            //String tags = media.getTagsAsString() != null ? media.getTagsAsString() : "";

            mediaListAsString.append(media.getFileName()).append("\t");
            mediaListAsString.append(name).append("\t");
            mediaListAsString.append(author).append("\t");
            mediaListAsString.append(link).append("\t");
            mediaListAsString.append(media.getTagsAsString()).append("\t");
            mediaListAsString.append("\n");
        }
        try {
            FileWriter myWriter = new FileWriter(filePath);
            myWriter.write(mediaListAsString.toString());
            myWriter.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Wrapper method to execute a query from mediaQueryBuilder and parse an arraylist from it.
     * @param db an SQLite readable database
     * @param selectedColumns the columns to return. must be a defined alias
     * @param whereFilters conditions for the media to be returned
     * @param orderBy the order of the rows
     * @param limit how many media to return
     * @return a mediaList
     */
    public ArrayList<Media> mediaQuery(SQLiteDatabase db, Set<String> selectedColumns,
                                        Map<String, String[]> whereFilters,
                                        String[] orderBy, long limit) {

        Pair<String, String[]> pair = mediaQueryBuilder(selectedColumns, whereFilters, orderBy, limit);
        String query = pair.first;
        String[] selectionArgs = pair.second;

        Cursor c = db.rawQuery(query, selectionArgs);
        ArrayList<Media> mediaList = parseMediaListFromCursor(c);
        c.close();
        return mediaList;
    }

    /**
     * Method that can build queries at runtime to select media from the database
     * @param selectedColumns the columns to return. must be a defined alias
     * @param whereFilters conditions for the media to be returned
     * @param orderBy the order of the rows
     * @param limit how many media to return
     * @return a pair containing the query string and a string array of the selection args
     */
    public Pair<String, String[]> mediaQueryBuilder(Set<String> selectedColumns,
                                                    Map<String, String[]> whereFilters,
                                                    String[] orderBy, long limit) {

        Set<String> selectionArgs = new HashSet<>();

        //Always select the id and filename
        selectedColumns.add(COL_MEDIA_ID_ALIAS);
        selectedColumns.add(COL_MEDIA_FILENAME_ALIAS);

        StringBuilder query = new StringBuilder();

        //SELECT
        query.append("SELECT ");
        Set<String> selection = new HashSet<>();
        Set<String> joins = new HashSet<>();
        Set<String> columnAliases = new HashSet<>();
        columnAliases.addAll(whereFilters.keySet());
        columnAliases.addAll(selectedColumns);
        for (String columnAlias : columnAliases) {
            String columnToSelect = columns.get(columnAlias);
            //If the column is not an alias, then it is a columnToSelect. Try to find the
            // column alias using this columnToSelect as a value in the columns set
            if (columnToSelect == null && columns.containsValue(columnAlias)) {
                String trueColumnToSelect = columnAlias;
                columnAlias = null;
                for (Map.Entry<String, String> entry : columns.entrySet()) {
                    if (entry.getValue().equals(trueColumnToSelect)) {
                        columnAlias = entry.getKey();
                        break;
                    }
                }
                columnToSelect = trueColumnToSelect;
            }
            if (columnToSelect == null) {
                throw new IllegalArgumentException("Must use a defined alias");
            }
            //Remove the column name and any functions from columnToSelect
            int startOfTableName = columnToSelect.contains("(") ? columnToSelect.indexOf("(")+1 : 0;
            String tableName = columnToSelect.substring(startOfTableName, columnToSelect.indexOf("."));
            switch (tableName) {
                case Constants.PREFS_MEDIA_AUTHOR:
                    joins.add(AUTHOR_JOIN);
                    break;
                case Constants.PREFS_MEDIA_TAG:
                    joins.add(TAG_JOIN);
                    break;
            }
            selection.add(columnToSelect + " AS " + columnAlias + ", ");
        }
        for (String selectColumn : selection) {
            query.append(selectColumn);
        }
        query.replace(query.lastIndexOf(","), query.length(), " "); //removing comma

        //FROM
        query.append("FROM " + MEDIA_TABLE + " ");

        //JOIN
        for (String join : joins) {
            query.append(join);
        }

        //WHERE
        if (whereFilters.size() != 0) {
            query.append("WHERE ");
            for (Map.Entry<String, String[]> entry : whereFilters.entrySet()) {
                //For filtering when the column value may match multiple values (OR clause)
                query.append("(");
                for (String value : entry.getValue()) {
                    query.append(entry.getKey()).append(" LIKE ?");
                    selectionArgs.add("%" + value + "%");
                    query.append(" OR ");
                }
                query.setLength(query.length()-(" OR ").length()+1);
                //For filtering other columns, append AND (AND clause)
                query.append(") AND ");
            }
            query.setLength(query.length()-(") AND ").length()+1); //remove trailing AND
        }

        //GROUP BY
        query.append("GROUP BY (").append(columns.get(COL_MEDIA_FILENAME_ALIAS)).append(") ");

        //ORDER BY
        if (orderBy != null && orderBy.length != 0) {
            query.append("ORDER BY ");
            for (String orderColumn : orderBy) {
                query.append(orderColumn).append(", ");
            }
            query.replace(query.lastIndexOf(","), query.length(), ""); //removing comma
        }

        //LIMIT
        if (limit != 0) {
            query.append("LIMIT ").append(limit);
        }

        return new Pair<>(query.toString(), selectionArgs.toArray(new String[0]));
    }

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
}
