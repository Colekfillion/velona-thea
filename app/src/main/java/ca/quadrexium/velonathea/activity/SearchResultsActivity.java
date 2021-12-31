package ca.quadrexium.velonathea.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Constants;
import ca.quadrexium.velonathea.pojo.Media;

public class SearchResultsActivity extends BaseActivity {

    private String path;
    private int maxCacheSize;
    private final ArrayList<Media> mediaList = new ArrayList<>();
    private final LinkedHashMap<String, Bitmap> imageCache = new LinkedHashMap<String, Bitmap>() {
        //Keep the cache size down for performance. Removes oldest entry if size is above max cache size
        @Override
        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > maxCacheSize;
        }
    };
    private RecyclerView rv;
    private MyAdapter adapter;
    float screenDensity;
    int lastUpdatedPosition = 0;

    ActivityResultLauncher<Intent> mediaDetailsActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        int position = data.getIntExtra(Constants.PREFS_UPDATED_MEDIA_POSITION, -1);
                        if (position == -1) {
                            throw new IllegalStateException("Media position cannot be -1");
                        }
                        mediaList.set(position, data.getParcelableExtra(Constants.MEDIA));
                        adapter.notifyItemChanged(position);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);

        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        screenDensity = displayMetrics.density;

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        Bundle data = getIntent().getExtras();

        //Setting global variables
        path = prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES);
        maxCacheSize = prefs.getInt(Constants.PREFS_CACHE_SIZE, 20);
        boolean showHiddenFiles = prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false);

        //Local variables
        String fileName = data.getString(Constants.PREFS_MEDIA_FILENAME);
        String nameFilter = data.getString(Constants.PREFS_MEDIA_NAME);
        String author = data.getString(Constants.PREFS_MEDIA_AUTHOR);
        String tag = data.getString(Constants.PREFS_MEDIA_TAG);
        String mediaType = data.getString(Constants.PREFS_MEDIA_TYPE);
        boolean randomOrder = prefs.getBoolean(Constants.PREFS_RANDOM_ORDER, false);
        boolean showInvalidFiles = prefs.getBoolean(Constants.PREFS_SHOW_INVALID_FILES, true);

        //Recyclerview configuration
        rv = findViewById(R.id.activity_search_results_recyclerview_media);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setDrawingCacheEnabled(true);
        rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(adapter = new MyAdapter());

        //Loading media into recyclerview
        if (showHiddenFiles || !path.contains(".")) {
            String queryCacheLocation = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath();
            if (doesQueryCacheExist(queryCacheLocation)) {
                mediaList.addAll(loadMediaFromCache(queryCacheLocation));
                Toast.makeText(this, "Loaded from cache", Toast.LENGTH_LONG).show();
            } else {
                MyOpenHelper myOpenHelper = openMediaDatabase();
                SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                Set<String> selectedColumns = new LinkedHashSet<>();
                ArrayList<String> orderBy = new ArrayList<>();
                if (randomOrder) {
                    orderBy.add("RANDOM()");
                }

                TreeMap<String, String[]> whereFilters = new TreeMap<>();
                if (!fileName.equals("")) {
                    whereFilters.put(MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME, new String[] { fileName });
                }
                if (!nameFilter.equals("")) {
                    selectedColumns.add(MyOpenHelper.COL_MEDIA_NAME_ALIAS);
                    whereFilters.put(MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_NAME, new String[] { nameFilter });
                }
                if (!author.equals("")) {
                    selectedColumns.add(MyOpenHelper.COL_AUTHOR_NAME_ALIAS);
                    whereFilters.put(MyOpenHelper.AUTHOR_TABLE + "." + MyOpenHelper.COL_AUTHOR_NAME, new String[] { author });
                }
                if (!tag.equals("")) {
                    selectedColumns.add(MyOpenHelper.COL_MEDIA_TAGS_GROUPED_ALIAS);
                    //selectedColumns.add(MyOpenHelper.COL_TAG_NAME_ALIAS);
                    whereFilters.put(MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_NAME, new String[] { tag });
                }
                if (mediaType != null) {
                    if (mediaType.equals(Constants.IMAGE)) {
                        whereFilters.put(MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME, Constants.IMAGE_EXTENSIONS.toArray(new String[0]));
                    } else if (mediaType.equals(Constants.VIDEO)) {
                        Set<String> videoExtensions = new HashSet<>(Constants.VIDEO_EXTENSIONS);
                        videoExtensions.add(".gif"); //gifs are considered videos except for viewing
                        whereFilters.put(MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME, videoExtensions.toArray(new String[0]));
                    }
                }
                if (!nameFilter.equals("") || !fileName.equals("")) {
                    String naturalSortColumn = MyOpenHelper.MEDIA_TABLE + ".";
                    naturalSortColumn += fileName.length() >= nameFilter.length() ? MyOpenHelper.COL_MEDIA_FILENAME : MyOpenHelper.COL_MEDIA_NAME;
                    orderBy.add("LENGTH(" + naturalSortColumn + ")");
                }
                try {
                    mediaList.addAll(myOpenHelper.mediaQuery(db, selectedColumns, whereFilters, orderBy.toArray(new String[0]), 0));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int i = 0;
                if (!showInvalidFiles) {
                    ArrayList<Media> mediaToRemove = new ArrayList<>();
                    for (Media media : mediaList) {
                        File f = new File(path + "/" + media.getFileName());
                        if (!f.exists() || (f.exists() && f.length() == 0)) {
                            mediaToRemove.add(media);
                        }
                        i++;
                        System.out.println(i);
                    }
                    mediaList.removeAll(mediaToRemove);
                }

                //Saving the mediaList as a tab-delimited text file
                //TODO: Async this
                StringBuilder mediaListAsString = new StringBuilder();
                for (Media media : mediaList) {
                    mediaListAsString.append(media.getId()).append("\t");
                    mediaListAsString.append(media.getFileName()).append("\t");
                    mediaListAsString.append("\n");
                }
                try {
                    FileWriter myWriter = new FileWriter(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath() + "/" + Constants.QUERY_CACHE_FILENAME);
                    myWriter.write(mediaListAsString.toString());
                    myWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                db.close();
            }
            adapter.notifyItemRangeInserted(0, mediaList.size());
            Toast.makeText(getApplicationContext(), mediaList.size() + " results", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_search_results; }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        @NonNull
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                    R.layout.row_search_results_recyclerview_media, viewGroup, false);
            //Show image in fullscreen on click
            view.setOnClickListener(v -> {
                int position = rv.getChildLayoutPosition(view);

                Bundle dataToPass = new Bundle();
                dataToPass.putInt(Constants.POSITION, position);

                Intent intent = new Intent(SearchResultsActivity.this, FullMediaActivity.class);
                intent.putExtras(dataToPass);
                startActivity(intent);
            });

            //Show image details on long click
            view.setOnLongClickListener(v -> {
                int position = rv.getChildLayoutPosition(view);
                Media media = mediaList.get(position);

                Bundle dataToPass = new Bundle();
                dataToPass.putParcelable(Constants.MEDIA, media);
                dataToPass.putInt(Constants.POSITION, position);

                Intent intent = new Intent(SearchResultsActivity.this, MediaDetailsActivity.class);
                intent.putExtras(dataToPass);
                mediaDetailsActivity.launch(intent);
                return true;
            });
            return new ViewHolder(view);
        }

        //For creating each row of the recyclerview
        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder imageLayout, int i) {
            Media media = mediaList.get(i);
            String fileName = media.getFileName();
            imageLayout.image.setScaleType(ImageView.ScaleType.CENTER_CROP);

            //Setting layout views
            imageLayout.fileName.setText(fileName);
            imageLayout.name.setText(media.getName());
            imageLayout.author.setText(media.getAuthor());
        }

        int imagesLoading = 0;

        @Override
        public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
            super.onViewAttachedToWindow(holder);

            String fileName = holder.fileName.getText().toString();
            if (imageCache.containsKey(fileName)) {
                holder.image.setImageBitmap(imageCache.get(fileName));
                //Load image from file
            } else {
                holder.image.setImageBitmap(null);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    Bitmap bm = null;
                    do {
                        imagesLoading++;
                        System.out.println(imagesLoading);
                        File f = new File(path + "/" + fileName);
                        if (f.exists()) {
                            //Image and gif (gifs can be loaded like a bitmap)
                            String extension = fileName.substring(fileName.lastIndexOf("."));
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
                                bm = ThumbnailUtils.createVideoThumbnail(path + "/" + fileName, MediaStore.Video.Thumbnails.MINI_KIND);
                                imageCache.put(fileName, bm);
                            }
                        }
                        break;
                    } while (true);
                    Bitmap finalBm = bm;
                    handler.post(() -> {
                        if (holder.fileName.getText().toString().equals(fileName)) {
                            holder.image.setImageBitmap(finalBm);
                        }
                        --imagesLoading;
                    });
                });
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
        }

        @Override
        public int getItemCount() {
            return mediaList.size();
        }

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

    public static ArrayList<Media> loadMediaFromCache(String cacheFileLocation) {
        ArrayList<Media> mediaList = new ArrayList<>();
        File queryCache = new File(cacheFileLocation + "/" + Constants.QUERY_CACHE_FILENAME);
        String content = "";
        try {
            BufferedReader r = new BufferedReader(new FileReader(queryCache));
            StringBuilder total = new StringBuilder();
            String line;

            while ((line = r.readLine()) != null) {
                total.append(line);
                total.append('\n');
            }
            r.close();
            content = total.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!content.equals("")) {

            String[] rows = content.split("\n");
            for (String row : rows) {
                String[] rowValues = row.split("\t");
                Media media = new Media.Builder()
                        .id(Integer.parseInt(rowValues[0]))
                        .fileName(rowValues[1])
                        .build();
                mediaList.add(media);
            }
        }
        return mediaList;
    }

    public static boolean doesQueryCacheExist(String cacheFileLocation) {
        File queryCache = new File(cacheFileLocation + "/" + Constants.QUERY_CACHE_FILENAME);
        return queryCache.exists();
    }

    boolean cancelDataLoading = false;
    @Override
    protected void onResume() {
        super.onResume();

        //Loading image data
        if (mediaList.size() > lastUpdatedPosition) {
            cancelDataLoading = false;
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {

                MyOpenHelper myOpenHelper = openMediaDatabase();
                SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                db.beginTransaction();
                for (int i = lastUpdatedPosition; i < mediaList.size(); i++) {
                    Media oldMedia = mediaList.get(i);
                    Media newMedia = null;
                    try {
                        newMedia = myOpenHelper.getRemainingData(db, oldMedia);
//                        if (newMedia.equals(oldMedia)) {
//                            continue;
//                        }
//                        System.out.println(oldMedia.toString());
//                        System.out.println(newMedia.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mediaList.set(i, newMedia);
                    int finalI = i;
                    handler.post(() -> adapter.notifyItemChanged(finalI));
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

    @Override
    protected void onPause() {
        super.onPause();
        cancelDataLoading = true;
    }
}