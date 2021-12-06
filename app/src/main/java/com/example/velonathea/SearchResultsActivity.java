package com.example.velonathea;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchResultsActivity extends AppCompatActivity {

    private String path, searchMode, searchFor;
    private int pageNum, maxCacheSize;
    private final ArrayList<Image> imageList = new ArrayList<>();
    private final LinkedHashMap<String, Bitmap> imageCache = new LinkedHashMap<String, Bitmap>() {
        //Keep the cache size down for performance. Removes oldest entry if size is above max cache size
        @Override
        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > maxCacheSize;
        }
    };
    private RecyclerView rv;
    private MyAdapter adapter;

//    private boolean isVerified = false;
//    private final ActivityResultLauncher<Intent> mainActivity = registerForActivityResult(
//            new ActivityResultContracts.StartActivityForResult(),
//            result -> {
//                if (result.getResultCode() == Activity.RESULT_OK) {
//                    isVerified = true;
//                    //run main
//                } else {
//                    finish();
//                }
//            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_search_results);

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        Bundle dataToPass = getIntent().getExtras();

        //Setting global variables
        searchMode = dataToPass.getString("searchMode");
        searchFor = dataToPass.getString("searchFor");
        path = prefs.getString("path", Environment.DIRECTORY_PICTURES);
        maxCacheSize = prefs.getInt("resultsPerPage", 10) * 5;

        //Recyclerview configuration
        rv = findViewById(R.id.activity_sr_imagelist);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setDrawingCacheEnabled(true);
        rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(adapter = new MyAdapter());

        //Initial search
        search(0);

        //Buttons to change page
        Button nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v -> {
            pageNum += 1;
            performSearch();
        });
        Button prevButton = findViewById(R.id.previousButton);
        prevButton.setOnClickListener(v -> {
            pageNum -= 1;
            performSearch();
        });
    }

    private void performSearch() {
        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        adapter.notifyItemRangeRemoved(0, imageList.size());
        search(pageNum * prefs.getInt("resultsPerPage", 10));
        adapter.notifyItemRangeInserted(0, imageList.size());
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (!isVerified) {
//            KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
//            Intent i = km.createConfirmDeviceCredentialIntent("Velona Thea", "This app requires you to authenticate.");
//            mainActivity.launch(i);
//        } else {
//            //run main method
//        }
//    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        @NonNull
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_sr_imagelayout, viewGroup, false);
            //Show image in fullscreen on click
            view.setOnClickListener(v -> {
                int position = rv.getChildLayoutPosition(view);
                Image image = imageList.get(position);
                Bundle dataToPass = new Bundle();
                dataToPass.putString("fileName", image.getFileName());
                Intent ii = new Intent(SearchResultsActivity.this, FullImageActivity.class);
                ii.putExtras(dataToPass);
                startActivity(ii);
            });

            //Show image details on long click
            view.setOnLongClickListener(v -> {
                int position = rv.getChildLayoutPosition(view);
                Image image = imageList.get(position);
                Bundle dataToPass = new Bundle();
                dataToPass.putString("fileName", image.getFileName());
                Intent ii = new Intent(SearchResultsActivity.this, ImageDetailsActivity.class);
                ii.putExtras(dataToPass);
                startActivity(ii);
                return true;
            });
            return new ViewHolder(view);
        }

        //For creating each row of the recyclerview
        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder imageLayout, int i) {
            Image imageObj = imageList.get(i);
            imageLayout.image.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Bitmap bm;
            //Load from image cache if image has been loaded before
            if (imageCache.containsKey(imageObj.getFileName())) {
                bm = imageCache.get(imageObj.getFileName());
                imageLayout.image.setImageBitmap(bm);
            //Load image from file
            } else {
                File f = new File(path + "/" + imageObj.getFileName());
                if (f.exists()) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    bm = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                    //compress the image
                    bm = resize(bm, bm.getWidth() / 4, bm.getHeight() / 4);
                    int bmSize = bm.getByteCount();
                    while (bmSize > (100 * 1024 * 1024)) {
                        bm = resize(bm, bm.getWidth() / 2, bm.getHeight() / 2);
                        bmSize = bm.getByteCount();
                    }
                    imageLayout.image.setImageBitmap(bm);
                    imageCache.put(imageObj.getFileName(), bm);
                } else {
                    //null if file doesn't exist
                    imageLayout.image.setImageBitmap(null);
                }
            }
            imageLayout.fileName.setText(imageObj.getFileName());
        }

        @Override
        public int getItemCount() {
            return imageList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView image;
            private TextView name;
            private TextView fileName;
            private TextView author;
            private TextView link;

            //View is the parent layout, activity_sr_imagelayout
            public ViewHolder(View view) {
                super(view);
                image = view.findViewById(R.id.activity_sr_imageview);
                fileName = view.findViewById(R.id.image_filename);
            }
        }
    }

    private void search(int offset) {

        imageList.clear();

        //Setting visibility of previous button if on the first page
        Button prevButton = findViewById(R.id.previousButton);
        if (offset != 0) {
            prevButton.setVisibility(View.VISIBLE);
        } else {
            prevButton.setVisibility(View.INVISIBLE);
        }
        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        int resultsPerPage = prefs.getInt("resultsPerPage", 10);

        MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
        SQLiteDatabase db = myOpenHelper.getReadableDatabase();
        String query = "";
        switch(searchMode){
            case "tag":
                //alternate query with no change in performance
//                query = "SELECT image.id, image.file_name " +
//                        "FROM tag " +
//                        "JOIN image_tag ON image_tag.tag_id = tag.id " +
//                        "JOIN image ON image.id = image_tag.image_id " +
//                        "WHERE tag.name LIKE ? " +
//                        "GROUP BY (image.file_name) " +
//                        "LIMIT " + resultsPerPage + " " +
//                        "OFFSET " + offset + ";";

                query = "SELECT image.id, image.file_name " +
                        "FROM image " +
                        "JOIN image_tag ON image.id = image_tag.image_id " +
                        "JOIN tag ON image_tag.tag_id = tag.id " +
                        "AND tag.name LIKE ? " +
                        "GROUP BY (image.file_name) " +
                        "LIMIT " + resultsPerPage + " " +
                        "OFFSET " + offset + ";";
                break;
            case "title":
                query = "SELECT image.id, image.file_name " +
                        "FROM image " +
                        "WHERE image.name LIKE ? " +
                        "GROUP BY (image.file_name) LIMIT " + resultsPerPage + " " +
                        "OFFSET " + offset + ";";
                break;
            case "author":
                query = "SELECT image.id, image.file_name " +
                        "FROM image " +
                        "WHERE image.author LIKE ? " +
                        "GROUP BY (image.file_name) LIMIT " + resultsPerPage + " " +
                        "OFFSET " + offset + ";";
                break;
        }

        Cursor c = db.rawQuery(query, new String[] { searchFor });
        long dbStartTime = SystemClock.elapsedRealtime();
        c.moveToFirst();
        long dbTimeToExecute = SystemClock.elapsedRealtime() - dbStartTime;
        Toast.makeText(getApplicationContext(), "time to execute: " + dbTimeToExecute + "ms", Toast.LENGTH_LONG).show();

        Button nextButton = findViewById(R.id.nextButton);
        if (c.getCount() < resultsPerPage) {
            nextButton.setVisibility(View.INVISIBLE);
        } else {
            nextButton.setVisibility(View.VISIBLE);
        }

        while (!c.isAfterLast()) {
            String fileName = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_FILENAME));
            int id = (int) c.getLong(c.getColumnIndex(MyOpenHelper.COL_IMAGE_ID));
            if (new File(path + "/" + fileName).exists()) {
                imageList.add(new Image(id, fileName));
            }
            c.moveToNext();
        }
        c.close();
        db.close();
    }

    private class Image {
        private final int id;
        private String name;
        private String fileName;
        private String author;
        private String link;

        Image(int id, String name, String fileName, String author, String link) {
            this.id = id;
            this.name = name != null ? name : fileName.substring(0, fileName.lastIndexOf("."));
            this.fileName = fileName;
            this.author = author;
            this.link = link;
        }

        public Image(int id, String fileName) {
            this.id = id;
            this.fileName = fileName;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getFileName() { return fileName; }
        public String getAuthor() { return author; }
        public String getLink() { return link; }

        public void setName(String name) { this.name = name; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public void setAuthor(String author) { this.author = author; }
        public void setLink(String link) { this.link = link; }
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
}