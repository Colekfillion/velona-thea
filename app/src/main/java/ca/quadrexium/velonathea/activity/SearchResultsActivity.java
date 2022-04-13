package ca.quadrexium.velonathea.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.fragment.MediaDetailsFragment;
import ca.quadrexium.velonathea.pojo.Constants;
import ca.quadrexium.velonathea.pojo.Media;

public class SearchResultsActivity extends BaseActivity {

    private int cacheSize;
    private final ArrayList<Media> mediaList = new ArrayList<>();
    private final Map<String, Bitmap> imageCache = new LinkedHashMap<String, Bitmap>() {
        //Keep the cache size down for performance. Removes oldest entry if size is above max cache size
        @Override
        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > cacheSize;
        }
    };
    private RecyclerView rv;
    private MyAdapter rvAdapter;
    private float screenDensity;
    private int lastUpdatedPosition = 0;
    private final int MAX_IMAGES_LOADING = 25;
    private ExecutorService executorService = Executors.newFixedThreadPool(MAX_IMAGES_LOADING);
    private final ActivityResultLauncher<Intent> fullMediaActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Bundle dataPassed = data.getExtras();
                        rv.scrollToPosition(dataPassed.getInt(Constants.MEDIA_LAST_POSITION));
                    }
                }
            });

    @Override
    protected void isVerified() { }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);

        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        screenDensity = displayMetrics.density;

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        Bundle data = getIntent().getExtras();

        //Setting global variables
        cacheSize = prefs.getInt(Constants.PREFS_CACHE_SIZE, 150);

        //Local variables
        boolean showHiddenFiles = prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false);
        String query = data.getString(Constants.MEDIA_QUERY);
        String[] selectionArgs = data.getStringArray(Constants.QUERY_ARGS);

        //Recyclerview configuration
        rv = findViewById(R.id.activity_search_results_recyclerview_media);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setDrawingCacheEnabled(true);
        rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(rvAdapter = new MyAdapter());

        //Loading media into recyclerview
        //Try to load from the query cache
        String queryCacheLocation = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath();
        String cachedQuery = getCachedQuery(queryCacheLocation);
        StringBuilder currentQuery = new StringBuilder();
        currentQuery.append(query);
        if (selectionArgs != null) {
            for (String arg : selectionArgs) {
                currentQuery.append(arg).append("\t");
            }
        }
        if (cachedQuery == null || !cachedQuery.equals(currentQuery.toString())) {
            File queryCache = new File(queryCacheLocation + "/" + Constants.QUERY_CACHE_FILENAME);
            boolean wasDeleted = queryCache.delete();
        }
        if (cachedQuery != null && cachedQuery.equals(currentQuery.toString())) {
            mediaList.addAll(loadMediaFromCache(queryCacheLocation));
            Toast.makeText(this, String.format(getString(R.string.loaded_x_media_cache), mediaList.size()), Toast.LENGTH_LONG).show();
            rvAdapter.notifyItemRangeInserted(0, mediaList.size());
        //Execute input query if query cache is invalid or nonexistent
        } else {
            MyOpenHelper myOpenHelper = getMyOpenHelper();
            SQLiteDatabase db = myOpenHelper.getReadableDatabase();
            Cursor c = db.rawQuery(query, selectionArgs);
            mediaList.addAll(myOpenHelper.parseMediaListFromCursor(c));

            //Removes media from results if they are in a hidden directory
            //TODO: Add where clause to query, 'if (!showHiddenFiles), file path not like "/."'
            ArrayList<Media> mediaToRemove = new ArrayList<>();
            for (Media media : mediaList) {
                if (!showHiddenFiles && media.getFilePath().contains("/.")) {
                    mediaToRemove.add(media);
                }
            }
            mediaList.removeAll(mediaToRemove);
            Toast.makeText(this, mediaList.size() + " results", Toast.LENGTH_LONG).show();
            c.close();
            db.close();
            rvAdapter.notifyItemRangeInserted(0, mediaList.size());

            //Saving the mediaList as a tab-delimited text file
            StringBuilder mediaListAsString = new StringBuilder();
            mediaListAsString.append(query);
            if (selectionArgs != null) {
                for (String arg : selectionArgs) {
                    mediaListAsString.append(arg).append("\t");
                }
            }
            mediaListAsString.append("\n");
            for (Media media : mediaList) {
                mediaListAsString.append(media.getId()).append("\t");
                mediaListAsString.append(media.getFilePath()).append("\t");
                mediaListAsString.append("\n");
            }
            try {
                FileWriter myWriter = new FileWriter(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath() + "/" + Constants.QUERY_CACHE_FILENAME);
                myWriter.write(mediaListAsString.toString());
                myWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_search_results; }

    /**
     * Recyclerview adapter for showing media.
     */
    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        @NonNull
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                    R.layout.row_search_results_recyclerview_media, viewGroup, false);
            //Show image in fullscreen on click
            view.setOnClickListener(v -> {
                int position = rv.getChildLayoutPosition(view);
                cancelDataLoading = true;

                Bundle dataToPass = new Bundle();
                dataToPass.putInt(Constants.POSITION, position);

                Intent intent = new Intent(SearchResultsActivity.this, FullMediaActivity.class);
                intent.putExtras(dataToPass);
                fullMediaActivity.launch(intent);
            });

            //Show image details on long click
            view.setOnLongClickListener(v -> {
                int position = rv.getChildLayoutPosition(view);
                Media media = mediaList.get(position);

                cancelDataLoading = true;
                FragmentManager fm = getSupportFragmentManager();
                MediaDetailsFragment mediaDetailsFragment = new MediaDetailsFragment(position, media);
                mediaDetailsFragment.show(fm, Constants.FRAGMENT_MEDIA_DETAILS);

                return true;
            });
            return new ViewHolder(view);
        }

        //For creating each row of the recyclerview
        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder imageLayout, int i) {
            Media media = mediaList.get(i);
            if (media == null) {
                finish();
            }
            String fileName = media.getFileName();
            imageLayout.image.setScaleType(ImageView.ScaleType.CENTER_CROP);

            //Setting layout views
            imageLayout.fileName.setText(fileName);
            imageLayout.name.setText(media.getName());
            imageLayout.author.setText(media.getAuthor());
        }

        @Override
        public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            Media visibleMedia = mediaList.get(holder.getAdapterPosition());

            String filePath = visibleMedia.getFilePath();
            String fileName = visibleMedia.getFileName();

            if (imageCache.containsKey(filePath)) {
                holder.image.setImageBitmap(imageCache.get(filePath));
                //Load image from file
            } else {
                holder.image.setImageBitmap(null);

                Handler handler = new Handler(Looper.getMainLooper());

                executorService.execute(() -> {
                    Bitmap bm = null;
                    //True if this task was queued for execution but user has left activity
                    if (!cancelDataLoading) {
                        while (true) {
                            File f = new File(filePath);
                            if (f.exists()) {
                                String extension = fileName.substring(fileName.lastIndexOf("."));
                                //Image and gif (gifs can be loaded like a bitmap)
                                if (Constants.IMAGE_EXTENSIONS.contains(extension) ||
                                        extension.equals(".gif")) {

                                    //Just decoding the dimensions of the target image
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inJustDecodeBounds = true;
                                    BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                                    if (!fileName.equals(holder.fileName.getText().toString())) {
                                        break;
                                    }
                                    int height = Math.round(options.outHeight / screenDensity); //height as dp

                                    int sampleSize = 1; //how much smaller the image should be loaded
                                    if (height != 80) {
                                        //if the height is 160dp, image will be loaded half as big
                                        sampleSize = Math.round(height / 80f);
                                    }

                                    //Loading the image with subsampling to save memory (smaller version of image)
                                    options.inJustDecodeBounds = false;
                                    options.inSampleSize = sampleSize;
                                    options.inPreferredConfig = Bitmap.Config.RGB_565; //less colours
                                    bm = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                                    imageCache.put(fileName, bm);
                                    //Video
                                } else if (Constants.VIDEO_EXTENSIONS.contains(extension)) {
                                    //thumbnails can be created easier for videos
                                    bm = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.MINI_KIND);
                                    imageCache.put(filePath, bm);
                                }
                            }
                            break;
                        }
                    }
                    Bitmap finalBm = bm;
                    handler.post(() -> {
                        if (holder.fileName.getText().toString().equals(fileName)) {
                            holder.image.setImageBitmap(finalBm);
                        }
                    });
                });
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
        }

        @Override
        public int getItemCount() { return mediaList.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView image;
            private final TextView name;
            private final TextView fileName;
            private final TextView author;

            //View is the parent layout, activity_sr_imagelayout
            public ViewHolder(View view) {
                super(view);
                image = view.findViewById(R.id.activity_search_results_imgv);
                fileName = view.findViewById(R.id.activty_search_results_tv_filename);
                name = view.findViewById(R.id.activty_search_results_tv_name);
                author = view.findViewById(R.id.activty_search_results_tv_author);
            }
        }
    }

    /**
     * Gets the cached query stored in the query cache if it exists, and null if it doesn't.
     * @param cacheFileLocation the parent folder of the query cache
     * @return the cached query and its selection args as a string
     */
    public static String getCachedQuery(String cacheFileLocation) {
        File queryCache = new File(cacheFileLocation + "/" + Constants.QUERY_CACHE_FILENAME);
        if (!queryCache.exists()) {
            return null;
        }
        String cachedQuery = "";
        try {
            BufferedReader r = new BufferedReader(new FileReader(queryCache));
            cachedQuery = r.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cachedQuery;
    }

    boolean cancelDataLoading = false;
    @Override
    protected void onResume() {
        super.onResume();
        startLoadingMedia();
    }

    public void startLoadingMedia() {
        cancelDataLoading = false;

        //Recreating the executor
        if (executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newFixedThreadPool(MAX_IMAGES_LOADING);
        }

        //Loading image data
        if (mediaList.size() > lastUpdatedPosition) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {

                MyOpenHelper myOpenHelper = getMyOpenHelper();
                SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                db.beginTransaction();
                for (int i = lastUpdatedPosition; i < mediaList.size(); i++) {
                    Media oldMedia = mediaList.get(i);
                    Media newMedia = myOpenHelper.depthMediaQuery(db, oldMedia.getId());
                    if (newMedia == null) { //if database has been cleared
                        handler.post(this::finish);
                    }
                    mediaList.set(i, newMedia);
                    int finalI = i;
                    handler.post(() -> rvAdapter.notifyItemChanged(finalI));
                    if (cancelDataLoading) {
                        lastUpdatedPosition = i;
                        break;
                    }
                }
                db.setTransactionSuccessful();
                db.endTransaction();
            });
        }
    }

    public void stopLoadingMedia() {
        cancelDataLoading = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLoadingMedia();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    /**
     * Called from MediaDetailsFragment to update the SearchResultsActivity dataset
     * @param i the location of the media to update in mediaList
     * @param media the updated media
     */
    public void mediaChanged(int i, Media media) {
        mediaList.set(i, media);
        rvAdapter.notifyItemChanged(i);
        startLoadingMedia();
    }

    /**
     * Method to calculate the size of all the bitmaps from imageCache.
     * @return the total size of cached bitmaps in megabytes
     */
    private int sizeOfCachedBitmaps() {
        int size = 0;
        for (Bitmap bm : imageCache.values()) {
            size += bm.getAllocationByteCount();
        }
        return size/1000000;
    }

    public void cancelDataLoading(boolean cancel) {
        cancelDataLoading = cancel;
    }

    @Override
    protected String getName() {
        return "SearchResultsActivity";
    }
}