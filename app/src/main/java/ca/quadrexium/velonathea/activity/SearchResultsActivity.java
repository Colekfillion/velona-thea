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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

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

    ActivityResultLauncher<Intent> fullImageActivity = registerForActivityResult(
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
        createToolbar(R.id.activity_main_toolbar);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        Bundle data = getIntent().getExtras();

        //Setting global variables
        path = prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES);
        maxCacheSize = prefs.getInt(Constants.PREFS_CACHE_SIZE, 20);
        boolean showHiddenFiles = prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false);

        //Local variables
        String nameFilter = data.getString(Constants.PREFS_MEDIA_NAME);
        String author = data.getString(Constants.PREFS_MEDIA_AUTHOR);
        String tag = data.getString(Constants.PREFS_MEDIA_TAG);
        String mediaType = data.getString(Constants.PREFS_MEDIA_TYPE);
        boolean randomOrder = prefs.getBoolean(Constants.PREFS_RANDOM_ORDER, false);

        //Recyclerview configuration
        rv = findViewById(R.id.activity_search_results_recyclerview_media);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setDrawingCacheEnabled(true);
        rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(adapter = new MyAdapter());

        MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
        SQLiteDatabase db = myOpenHelper.getReadableDatabase();
        ArrayList<String> orderBy = new ArrayList<>();
        if (randomOrder) {
            orderBy.add("RANDOM()");
        }
        if (showHiddenFiles || !path.contains(".")) {
            TreeMap<String, ArrayList<String>> whereFilters = new TreeMap<>();
            if (!nameFilter.equals("")) {
                whereFilters.put(MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_NAME, new ArrayList<>(
                        Arrays.asList(nameFilter)));
            }
            if (!author.equals("")) {
                whereFilters.put(MyOpenHelper.COL_AUTHOR_NAME_FOREIGN, new ArrayList<>(
                        Arrays.asList(author)));
            }
            if (!tag.equals("")) {
                whereFilters.put(MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_NAME, new ArrayList<>(
                        Arrays.asList(tag)));
            }
            if (mediaType != null) {
                if (mediaType.equals(Constants.IMAGE)) {
                    whereFilters.put(MyOpenHelper.COL_MEDIA_FILENAME, Constants.IMAGE_EXTENSIONS);
                } else if (mediaType.equals(Constants.VIDEO)) {
                    ArrayList<String> videoExtensions = new ArrayList<>(Constants.VIDEO_EXTENSIONS);
                    videoExtensions.add(".gif"); //gifs are considered videos except for viewing
                    whereFilters.put(MyOpenHelper.COL_MEDIA_FILENAME, videoExtensions);
                }
            }
            mediaList.addAll(myOpenHelper.getMediaList(db, whereFilters, orderBy.toArray(new String[0])));
            adapter.notifyItemRangeInserted(0, mediaList.size());
        }
        Toast.makeText(getApplicationContext(), mediaList.size() + " results", Toast.LENGTH_SHORT).show();
        db.close();
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

                //Limiting media passed to FullMediaActivity to stay within bundle transaction limit
                ArrayList<String> fileNames = new ArrayList<>();
                int maxFileNames = 1000;
                int minPosition = Math.max(0, position-(maxFileNames/5));
                int maxPosition = (int) Math.min(mediaList.size(), position+(Math.round(maxFileNames*0.8)));
                for (int ii = minPosition; ii < maxPosition; ii++) {
                    fileNames.add(mediaList.get(ii).getFileName());
                }

                dataToPass.putStringArrayList("fileNames", fileNames);
                dataToPass.putInt(Constants.POSITION, position-minPosition);
                Intent ii = new Intent(SearchResultsActivity.this, FullMediaActivity.class);
                ii.putExtras(dataToPass);
                startActivity(ii);
            });

            //Show image details on long click
            view.setOnLongClickListener(v -> {
                int position = rv.getChildLayoutPosition(view);
                Media media = mediaList.get(position);
                Bundle dataToPass = new Bundle();
                dataToPass.putParcelable(Constants.MEDIA, media);
                dataToPass.putInt(Constants.POSITION, position);
                Intent ii = new Intent(SearchResultsActivity.this, MediaDetailsActivity.class);
                ii.putExtras(dataToPass);
                fullImageActivity.launch(ii);
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
            imageLayout.fileName.setText(media.getFileName());
            imageLayout.name.setText(media.getName());
            imageLayout.author.setText(media.getAuthor());

            //Load from image cache if image has been loaded before
            if (imageCache.containsKey(fileName)) {
                imageLayout.image.setImageBitmap(imageCache.get(fileName));
            //Load image from file
            } else {
                imageLayout.image.setImageBitmap(null);
                new ImageLoaderTask(imageLayout).execute(fileName);
            }
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

    private class ImageLoaderTask extends AsyncTask<String, Bitmap, Bitmap> {

        private final MyAdapter.ViewHolder view;
        private String fileName;
        private float screenDensity;

        public ImageLoaderTask(MyAdapter.ViewHolder view) {
            this.view = view;
        }

        public boolean validate() {
            if (!fileName.equals(view.fileName.getText().toString())) {
                cancel(true);
                return true;
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
            screenDensity = displayMetrics.density;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bm = null;
            fileName = strings[0];
            if (validate()) { return bm; } //get used to this
            File f = new File(path + "/" + fileName);
            if (f.exists()) {
                //Image and gif (gifs can be loaded like a bitmap)
                String extension = fileName.substring(fileName.lastIndexOf("."));
                if (Constants.IMAGE_EXTENSIONS.contains(extension) ||
                        extension.equals(".gif")) {
                    if (validate()) { return bm; }

                    //Just decoding the dimensions of the target image
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                    if (validate()) { return bm; }
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
                    if (validate()) { return bm; }
                    bm = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                    imageCache.put(fileName, bm);
                //Video
                } else if (Constants.VIDEO_EXTENSIONS.contains(extension)) {
                    if (validate()) { return bm; }
                    //thumbnails can be created easier for videos
                    bm = ThumbnailUtils.createVideoThumbnail(path + "/" + fileName, MediaStore.Video.Thumbnails.MINI_KIND);
                    imageCache.put(fileName, bm);
                }
            }
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bm) {
            if (view.fileName.getText().toString().equals(fileName)) {
                view.image.setImageBitmap(bm);
            } else {
                view.image.setImageBitmap(null);
            }
        }
    }
}