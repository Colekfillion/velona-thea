package ca.quadrexium.velonathea.pojo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Constants {
    public static final Set<String> IMAGE_EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(".webp", ".png", ".jpg", ".jpeg", ".bmp")));

    public static final Set<String> VIDEO_EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(".mp4", ".mkv", ".webm")));

    public static final String VIDEO = "video";
    public static final String IMAGE = "image";
    public static final String PATH = "path";
    public static final String POSITION = "position";
    public static final String MEDIA_QUERY = "mediaQuery";
    public static final String QUERY_ARGS = "selectionArgs";
    public static final String QUERY_CACHE_FILENAME = "query_cache.txt";

    public static final String MEDIA_LAST_POSITION = "lastPosition";

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_GIF = 3;

    public static final String PREFS = "preferences";
    public static final String PREFS_MEDIA_AUTHOR = "author";
    public static final String PREFS_MEDIA_TAG = "tag";
    public static final String PREFS_RANDOM_ORDER = "randomOrder";
    public static final String PREFS_SHOW_HIDDEN_FILES = "showHiddenFiles";
    public static final String PREFS_SHOW_INVALID_FILES = "showInvalidFiles";
    public static final String PREFS_CACHE_SIZE = "cacheSize";

    public static final String FRAGMENT_CHOOSE_DIR = "chooseDirFragment";
    public static final String FRAGMENT_MEDIA_DETAILS = "mediaDetailsFragment";

    public static boolean isStringEmpty(String string) {
        return string == null || string.equals("") || string.length() == 0;
    }

    public static <T, V> void mapReplace(Map<T, V> map, T key, V newValue) {
        map.remove(key);
        map.put(key, newValue);
    }

}
