package com.example.velonathea;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
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
        Cursor c;
        switch(searchMode){
            case "tag":
//                c = db.rawQuery("SELECT " + MyOpenHelper.IMAGE_TABLE + "." + MyOpenHelper.COL_IMAGE_NAME + ", " +
//                                MyOpenHelper.IMAGE_TABLE + "." + MyOpenHelper.COL_IMAGE_FILENAME + ", " +
//                                MyOpenHelper.IMAGE_TABLE + "." + MyOpenHelper.COL_IMAGE_AUTHOR + ", " +
//                                MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_NAME +
//                                    " AS " + MyOpenHelper.TAG_TABLE + "_" + MyOpenHelper.COL_TAG_NAME + " " +
//                                "FROM " + MyOpenHelper.IMAGE_TABLE + " " +
//                                "JOIN " + MyOpenHelper.IMAGE_TAG_TABLE + " ON " +
//                                    MyOpenHelper.IMAGE_TABLE + "." + MyOpenHelper.COL_IMAGE_ID + " = " + MyOpenHelper.IMAGE_TAG_TABLE + "." + MyOpenHelper.COL_IMAGE_TAG_IMAGE_ID + " " +
//                                "JOIN " + MyOpenHelper.TAG_TABLE + " ON " +
//                                    MyOpenHelper.IMAGE_TAG_TABLE + "." + MyOpenHelper.COL_IMAGE_TAG_TAG_ID + " = " + MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_ID + " " +
//                                "AND " + MyOpenHelper.TAG_TABLE + " LIKE \"" + searchFor + "\";", null);
                c = db.rawQuery("SELECT image.id, image.name, image.file_name, image.author, image.link, tag.name AS tag_name " +
                        "FROM image " +
                        "JOIN image_tag ON image.id = image_tag.image_id " +
                        "JOIN tag ON image_tag.tag_id = tag.id " +
                        "AND tag.name LIKE ?" +
                        "GROUP BY(image.file_name) LIMIT 10;", new String[] { searchFor });
                break;
            case "title":
                c = db.rawQuery("SELECT image.id, image.name, image.file_name, image.author, image.link, tag.name AS tag_name " +
                        "FROM image " +
                        "JOIN image_tag ON image.id = image_tag.image_id " +
                        "JOIN tag ON image_tag.tag_id = tag.id " +
                        "AND image.name LIKE ?" +
                        "GROUP BY(image.file_name) LIMIT 10;", new String[] { searchFor });
                break;
            case "author":
                c = db.rawQuery("SELECT image.id, image.name, image.file_name, image.author, image.link, tag.name AS tag_name " +
                        "FROM image " +
                        "JOIN image_tag ON image.id = image_tag.image_id " +
                        "JOIN tag ON image_tag.tag_id = tag.id " +
                        "AND image.author LIKE ?" +
                        "GROUP BY(image.file_name) LIMIT 10;", new String[] { searchFor });
                break;
            default:
                c = db.rawQuery("SELECT image.id, image.name, image.file_name, image.author, image.link, tag.name AS tag_name " +
                        "FROM image " +
                        "JOIN image_tag ON image.id = image_tag.image_id " +
                        "JOIN tag ON image_tag.tag_id = tag.id " +
                        "GROUP BY(image.file_name) LIMIT 10;", null);
                break;
        }
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
            Image image = imageList.get(position);
            Toast.makeText(this, image.getName(), Toast.LENGTH_SHORT).show();
        });

        imageListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Image image = imageList.get(position);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            alertDialogBuilder.setTitle(image.getName())

                    .setMessage(image.getFileName() + "\n" + image.getAuthor() + "\n" + image.getLink())

                    .setNeutralButton("Ok", (click, arg) -> { })

                    .create().show();
            return true;
        });
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

            ImageView image = newView.findViewById(R.id.activity_sr_imageview);
            File f = new File(path + "/" + imageObj.getFileName());
            if (f.exists()) {
                Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
                int bmSize = bm.getByteCount();
                while (bmSize > (100 * 1024 * 1024)) {
                    bm = resize(bm, bm.getWidth() / 2, bm.getHeight() / 2);
                    bmSize = bm.getByteCount();
                }
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                bm.compress(Bitmap.CompressFormat.JPEG, 80, baos);

                System.out.println(f.getAbsolutePath());
                image.setImageBitmap(bm);
            } else {
                image.setImageBitmap(null);
            }
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