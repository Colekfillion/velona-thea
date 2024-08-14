package ca.colekfillion.velonathea.activity;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ca.colekfillion.velonathea.pojo.Constants;
import ca.colekfillion.velonathea.pojo.Media;

/**
 * Special implementation of BaseActivity that will create fragments that need to access
 * the query cache.
 */
public abstract class CacheDependentActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Reads the contents of a file as a String, for use in fragments
     *
     * @param uri the file to read
     * @return the file contents as a string
     */
    public static String readStringFromUri(Uri uri, Activity parentActivity) {
        String text = "";
        try {
            InputStream in = parentActivity.getContentResolver().openInputStream(uri);
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }
            text = total.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }

    /**
     * Loads a media list from the query cache, if it exists.
     *
     * @param cacheFileLocation the parent folder of the query cache
     * @return an arraylist of media with id and filename set
     */
    protected ArrayList<Media> loadMediaFromCache(String cacheFileLocation) {
        ArrayList<Media> mediaList = new ArrayList<>();
        File queryCache = new File(cacheFileLocation, Constants.QUERY_CACHE_FILENAME);
        String content = readStringFromUri(Uri.fromFile(queryCache), this);

        if (!content.equals("")) {
            content = content.substring(content.indexOf("\n") + 1); //remove query

            String[] rows = content.split("\n");
            for (String row : rows) {
                String[] rowValues = row.split("\t");
                Media media = new Media.Builder()
                        .id(Integer.parseInt(rowValues[0]))
                        .filePath(rowValues[1])
                        .build();
                mediaList.add(media);
            }
        }
        return mediaList;
    }
}