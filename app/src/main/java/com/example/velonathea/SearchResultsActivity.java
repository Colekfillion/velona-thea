package com.example.velonathea;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

public class SearchResultsActivity extends AppCompatActivity {

    public ArrayList<Image> imageList = new ArrayList<>();
    public ListAdapter adapter;
    public ListView imageListView;
    public String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        Bundle dataToPass = getIntent().getExtras();
        String searchMode = dataToPass.getString("searchMode");
        String searchFor = dataToPass.getString("searchFor");

//        path = getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS)[1].getAbsolutePath().substring(0, path.lastIndexOf("/"));
//        path = path.substring(0, path.lastIndexOf("/")).substring(0, path.lastIndexOf("/"));
//        path = path.substring(0, path.lastIndexOf("/")).substring(0, path.lastIndexOf("/"));
//        path += "/.main/Download";
//        System.out.println(path);
        path = "/storage/9C33-6BBD/.main/Download";

        imageListView = findViewById(R.id.activity_sr_imagelist);
        imageListView.setAdapter(adapter = new ListAdapter());

        MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
        SQLiteDatabase db = myOpenHelper.getWritableDatabase();
        String baseQuery = "SELECT image.id, image.name, image.file_name, image.author, image.link, tag.name AS tag_name " +
                "FROM image " +
                "JOIN image_tag ON image.id = image_tag.image_id " +
                "JOIN tag ON image_tag.tag_id = tag.id ";
        switch(searchMode){
            case "tag":
                baseQuery += "AND tag.name LIKE ?" +
                        "GROUP BY(image.file_name) LIMIT 10;";
                break;
            case "title":
                baseQuery += "AND image.name LIKE ?" +
                        "GROUP BY(image.file_name) LIMIT 10;";
                break;
            case "author":
                baseQuery += "AND image.author LIKE ?" +
                        "GROUP BY(image.file_name) LIMIT 10;";
                break;
            default:
                baseQuery += "GROUP BY(image.file_name) LIMIT 10;";
                break;
        }
        Cursor c = db.rawQuery(baseQuery, new String[] { searchFor });
        c.moveToFirst();
        while (!c.isAfterLast()) {
            String fileName = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_FILENAME));
            int id = (int) c.getLong(c.getColumnIndex(MyOpenHelper.COL_IMAGE_ID));
            String name = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_NAME));
            String author = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_AUTHOR));
            String link = c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_LINK));
            imageList.add(new Image(id, name, fileName, author, link));
            c.moveToNext();
        }
        adapter.notifyDataSetChanged();
        c.close();

        imageListView.setOnItemClickListener((list, item, position, id) -> {
            //TODO: Design a new activity for viewing pictures
            Image image = imageList.get(position);
        });

//        imageListView.setOnItemLongClickListener((parent, view, position, id) -> {
//            Image image = imageList.get(position);
//
//            return true;
//        });
    }

    private class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() { return imageList.size(); }

        @Override
        public Object getItem(int i) { return imageList.get(i); }

        @Override
        public long getItemId(int i) {
            Image image = ((Image) getItem(i));
            //return imageList.indexOf(image);
            return image.getId();
        }

        @Override
        public View getView(int i, View old, ViewGroup parent) {
            View newView = old;
            LayoutInflater inflater = getLayoutInflater();
            Image imageObj = ((Image) getItem(i));

            if (old == null) {
                newView = inflater.inflate(R.layout.activity_sr_imagelayout, parent, false);
            }
            //Setting image
            ImageView image = newView.findViewById(R.id.activity_sr_imageview);
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
                image.setImageBitmap(bm);
            } else {
                //null if not on screen
                image.setImageBitmap(null);
            }
            ((TextView) newView.findViewById(R.id.image_name)).setText(imageObj.getName());
            ((TextView) newView.findViewById(R.id.image_filename)).setText(imageObj.getFileName());
            ((TextView) newView.findViewById(R.id.image_author)).setText(imageObj.getAuthor());

            return newView;
        }
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
            return image;
        } else {
            return image;
        }
    }
}