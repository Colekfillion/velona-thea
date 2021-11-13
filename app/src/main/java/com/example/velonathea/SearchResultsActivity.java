package com.example.velonathea;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchResultsActivity extends AppCompatActivity {

    private ArrayList<Image> imageList = new ArrayList<>();
    private String path;
    private String searchMode;
    private String searchFor;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_search_results);

        Bundle dataToPass = getIntent().getExtras();
        searchMode = dataToPass.getString("searchMode");
        searchFor = dataToPass.getString("searchFor");

        String buildModel = android.os.Build.MODEL;
        System.out.println(buildModel);
        if (buildModel.equals("SM-A520W")) {
            path = "/storage/9C33-6BBD/.main/Download";
        } else {
            path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        }

        RecyclerView rv = findViewById(R.id.activity_sr_imagelist);
        rv.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(layoutManager);
        MyAdapter adapter = new MyAdapter();
        rv.setAdapter(adapter);

        AtomicInteger pageNum = new AtomicInteger(1);
        search(0);
        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        Button nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v -> {
            pageNum.incrementAndGet();
            adapter.notifyItemRangeRemoved(0, imageList.size());
            search(pageNum.get() * prefs.getInt("resultsPerPage", 10));
            adapter.notifyItemRangeInserted(0, imageList.size());
        });
        Button prevButton = findViewById(R.id.previousButton);
        prevButton.setOnClickListener(v -> {
            pageNum.decrementAndGet();
            adapter.notifyItemRangeRemoved(0, imageList.size());
            search(pageNum.get() * prefs.getInt("resultsPerPage", 10));
            adapter.notifyItemRangeInserted(0, imageList.size());
        });
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        @NonNull
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_sr_imagelayout, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder imageLayout, int i) {
            Image imageObj = imageList.get(i);
            imageLayout.image.setScaleType(ImageView.ScaleType.CENTER_CROP);

            //Set imageview to bitmap at path+image file name
            File f = new File(path + "/" + imageObj.getFileName());
            if (f.exists()) {
                Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
                //compress the image
                bm = resize(bm, bm.getWidth() / 4, bm.getHeight() / 4);
                int bmSize = bm.getByteCount();
                while (bmSize > (100 * 1024 * 1024)) {
                    bm = resize(bm, bm.getWidth() / 2, bm.getHeight() / 2);
                    bmSize = bm.getByteCount();
                }
                imageLayout.image.setImageBitmap(bm);
            } else {
                //null if not on screen
                imageLayout.image.setImageBitmap(null);
            }
            imageLayout.name.setText(imageObj.getName());
            imageLayout.fileName.setText(imageObj.getFileName());
            imageLayout.author.setText(imageObj.getAuthor());
            imageLayout.link.setText(imageObj.getLink());
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
                name = view.findViewById(R.id.image_name);
                fileName = view.findViewById(R.id.image_filename);
                author = view.findViewById(R.id.image_author);
                link = view.findViewById(R.id.image_link);
            }
        }
    }

    private void search(int offset) {

        imageList.clear();

        Button prevButton = findViewById(R.id.previousButton);
        if (offset != 0) {
            prevButton.setVisibility(View.VISIBLE);
        } else {
            prevButton.setVisibility(View.INVISIBLE);
        }

        MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
        SQLiteDatabase db = myOpenHelper.getReadableDatabase();
        String baseQuery = "SELECT image.id, image.name, image.file_name, image.author, image.link, tag.name AS tag_name " +
                "FROM image " +
                "JOIN image_tag ON image.id = image_tag.image_id " +
                "JOIN tag ON image_tag.tag_id = tag.id ";
        switch(searchMode){
            case "tag":
                baseQuery += "AND tag.name LIKE ? ";
                break;
            case "title":
                baseQuery += "AND image.name LIKE ? ";
                break;
            case "author":
                baseQuery += "AND image.author LIKE ? ";
                break;
        }
        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        int resultsPerPage = prefs.getInt("resultsPerPage", 10);
        baseQuery += "GROUP BY(image.file_name) LIMIT " + resultsPerPage +
                " OFFSET " + offset + ";";
        System.out.println(baseQuery);
        Cursor c = db.rawQuery(baseQuery, new String[] { searchFor });
        c.moveToFirst();

        Button nextButton = findViewById(R.id.nextButton);
        if (c.getCount() < resultsPerPage) {
            nextButton.setVisibility(View.INVISIBLE);
        } else {
            nextButton.setVisibility(View.VISIBLE);
        }
        while (!c.isAfterLast()) {
            String fileName = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_FILENAME));
            int id = (int) c.getLong(c.getColumnIndex(MyOpenHelper.COL_IMAGE_ID));
            String name = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_NAME));
            String author = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_AUTHOR));
            String link = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_LINK));
            imageList.add(new Image(id, name, fileName, author, link));
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