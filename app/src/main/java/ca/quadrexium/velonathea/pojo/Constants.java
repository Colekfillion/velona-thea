package ca.quadrexium.velonathea.pojo;

import java.util.ArrayList;

public class Constants {
    public static final ArrayList<String> IMAGE_EXTENSIONS = new ArrayList<String>() {{
        add(".webp");
        add(".png");
        add(".jpg");
        add(".jpeg");
        add(".bmp");
    }};

    public static final ArrayList<String> VIDEO_EXTENSIONS = new ArrayList<String>() {{
        add(".mp4");
        add(".mkv");
        add(".webm");
    }};
    public static final String MEDIA = "media";
    public static final String VIDEO = "video";
    public static final String IMAGE = "image";
    public static final String PATH = "path";
    public static final String POSITION = "position";
    public static final String QUERY_CACHE_FILENAME = "query_cache.txt";

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_GIF = 3;

    public static final String PREFS = "preferences";
    public static final String PREFS_MEDIA_NAME = "name";
    public static final String PREFS_MEDIA_FILENAME = "fileName";
    public static final String PREFS_MEDIA_AUTHOR = "author";
    public static final String PREFS_MEDIA_TAG = "tag";
    public static final String PREFS_MEDIA_TYPE = "mediaType";
    public static final String PREFS_RANDOM_ORDER = "randomOrder";
    public static final String PREFS_SHOW_HIDDEN_FILES = "showHiddenFiles";
    public static final String PREFS_SHOW_INVALID_FILES = "showValidFiles";
    public static final String PREFS_CACHE_SIZE = "cacheSize";
    public static final String PREFS_UPDATED_MEDIA_POSITION = "positionToUpdate";

}
