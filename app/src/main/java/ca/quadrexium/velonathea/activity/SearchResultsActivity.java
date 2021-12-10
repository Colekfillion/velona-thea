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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

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
    //private int numAsyncThreads = 0;

    ActivityResultLauncher<Intent> fullImageActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        int position = data.getIntExtra("position", -1);
                        System.out.println("position: " + position);
                        if (position == -1) {
                            throw new IllegalStateException("Media position cannot be -1");
                        }
                        mediaList.set(position, data.getParcelableExtra("media"));
                        adapter.notifyItemChanged(position);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_main_toolbar);

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        Bundle dataToPass = getIntent().getExtras();

        //Setting global variables
        path = prefs.getString("path", Environment.DIRECTORY_PICTURES);
        maxCacheSize = prefs.getInt("maxCacheSize", 20);

        //Local variables
        String searchMode = dataToPass.getString("searchMode");
        String searchFor = "%" + dataToPass.getString("searchFor") + "%"; //% padding for string matching
        boolean randomOrder = prefs.getBoolean("randomOrder", false);

        //Recyclerview configuration
        rv = findViewById(R.id.activity_search_results_recyclerview_media);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setDrawingCacheEnabled(true);
        rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(adapter = new MyAdapter());

        if (!searchMode.equals("unsorted")) {
            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getReadableDatabase();
            String query = "";
            Cursor c;
            switch(searchMode){
                case "tag":
                    query = "SELECT image.id, image.file_name, image.name, author.name AS author_name " +
                            "FROM image " +
                            "JOIN image_tag ON image.id = image_tag.image_id " +
                            "JOIN author ON author.id = image.author_id " +
                            "JOIN tag ON image_tag.tag_id = tag.id " +
                            "AND tag.name LIKE ? ";
                    break;
                case "title":
                    query = "SELECT image.id, image.file_name, image.name, author.name AS author_name " +
                            "FROM image " +
                            "JOIN author ON author.id = image.author_id " +
                            "WHERE image.name LIKE ? ";
                    break;
                case "author":
                    //Query to check if that exact author exists
                    c = db.rawQuery("SELECT name " +
                            "FROM author " +
                            "WHERE name = ? " +
                            "LIMIT 1;", new String[] { searchFor });
                    //If exact author doesn't exist, use LIKE clause
                    if (c.getCount() == 0) {
                        query = "SELECT image.id, image.file_name, image.name, author.name AS author_name " +
                                "FROM image " +
                                "JOIN author ON author.id = image.author_id " +
                                "WHERE author_name LIKE ? ";
                    } else {
                        query = "SELECT image.id, image.file_name, image.name, author.name AS author_name " +
                                "FROM image " +
                                "JOIN author ON author.id = image.author_id " +
                                "WHERE author_name = ? ";
                    }
                    break;
            }
            query += "GROUP BY (image.file_name) ";
            if (randomOrder) {
                query += "ORDER BY RANDOM();";
            } else {
                query += ";";
            }
            c = db.rawQuery(query, new String[] { searchFor });
            c.moveToFirst();

            while (!c.isAfterLast()) {
                Media media = new Media.Builder()
                        .id((int) c.getLong(c.getColumnIndex(MyOpenHelper.COL_ID)))
                        .name(c.getString(c.getColumnIndex(MyOpenHelper.COL_NAME)))
                        .fileName(c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_FILENAME)))
                        .author(c.getString(c.getColumnIndex(MyOpenHelper.AUTHOR_TABLE + "_" + MyOpenHelper.COL_NAME)))
                        .build();
                mediaList.add(media);
                c.moveToNext();
            }
            c.close();
            db.close();
        } else {
            File rootPath = new File(path);
            File[] files = rootPath.listFiles(File::isFile);
            HashSet<String> fileNames = new HashSet<>();
            for (File file : files) {
                fileNames.add(file.getName());
            }
            HashSet<String> dbFileNames = new HashSet<>();
            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getWritableDatabase();
            Cursor c = db.rawQuery("SELECT file_name FROM image;", null);
            c.moveToFirst();

            while (!c.isAfterLast()) {
                dbFileNames.add(c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_FILENAME)));
                c.moveToNext();
            }
            c.close();
            fileNames.removeAll(dbFileNames);
            db.beginTransaction();
            for (String fileName : fileNames) {
                if (Constants.VIDEO_EXTENSIONS.contains(fileName.substring(fileName.lastIndexOf("."))) ||
                        Constants.IMAGE_EXTENSIONS.contains(fileName.substring(fileName.lastIndexOf(".")))) {

                    Media media = new Media.Builder()
                            .id(-1)
                            .name(fileName.substring(0, fileName.lastIndexOf(".")))
                            .fileName(fileName)
                            .author("unknown")
                            .build();
                    media.setId(myOpenHelper.insertMedia(db, media));
                    mediaList.add(media);
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        }
        adapter.notifyItemRangeInserted(0, mediaList.size());
        Toast.makeText(getApplicationContext(), mediaList.size() + " results", Toast.LENGTH_SHORT).show();
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
                ArrayList<String> fileNames = new ArrayList<>();
                int maxFileNames = 1000;
                int minPosition = Math.max(0, position-(maxFileNames/5));
                int maxPosition = (int) Math.min(mediaList.size(), position+(Math.round(maxFileNames*0.8)));
                for (int ii = minPosition; ii < maxPosition; ii++) {
                    fileNames.add(mediaList.get(ii).getFileName());
                }
                dataToPass.putStringArrayList("fileNames", fileNames);
                dataToPass.putInt("position", position);
                Intent ii = new Intent(SearchResultsActivity.this, FullMediaActivity.class);
                ii.putExtras(dataToPass);
                startActivity(ii);
            });

            //Show image details on long click
            view.setOnLongClickListener(v -> {
                int position = rv.getChildLayoutPosition(view);
                Media media = mediaList.get(position);
                Bundle dataToPass = new Bundle();
                dataToPass.putParcelable("media", media);
                System.out.println("position: " + position);
                dataToPass.putInt("position", position);
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
                //numAsyncThreads++;
                //System.out.println(numAsyncThreads);
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

    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
        }
        return image;
    }

    private class ImageLoaderTask extends AsyncTask<String, Bitmap, Bitmap> {

        private final MyAdapter.ViewHolder view;
        private String fileName;

        public ImageLoaderTask(MyAdapter.ViewHolder view) {
            this.view = view;
        }

        public boolean validate() {
            if (!fileName.equals(view.fileName.getText().toString())) {
                //numAsyncThreads--;
                //System.out.println(numAsyncThreads);
                cancel(true);
                return true;
            }
            return false;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bm = null;
            fileName = strings[0];
            if (validate()) { return bm; }
            File f = new File(path + "/" + fileName);
            if (f.exists()) {
                //Image
                if (Constants.IMAGE_EXTENSIONS.contains(fileName.substring(fileName.lastIndexOf("."))) ||
                        fileName.substring(fileName.lastIndexOf(".")).equals(".gif")) {
                    if (validate()) { return bm; }

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    bm = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                    bm = resize(bm, bm.getWidth() / 4, bm.getHeight() / 4);

                    int bmSize = bm.getByteCount(); //we compress the image
                    while (bmSize > (100 * 1024 * 1024)) {
                        if (validate()) { return bm; }

                        bm = resize(bm, bm.getWidth() / 2, bm.getHeight() / 2);
                        bmSize = bm.getByteCount();
                    }
                    imageCache.put(fileName, bm);
                //Video
                } else if (Constants.VIDEO_EXTENSIONS.contains(fileName.substring(fileName.lastIndexOf(".")))) {
                    if (validate()) { return bm; }
                    //thumbnails can be created easier for videos
                    bm = ThumbnailUtils.createVideoThumbnail(path + "/" + fileName, MediaStore.Video.Thumbnails.MICRO_KIND);
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
            //numAsyncThreads--;
            //System.out.println(numAsyncThreads);
        }
    }
}