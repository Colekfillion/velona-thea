package ca.quadrexium.velonathea.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.pojo.Constants;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class FullMediaActivity extends BaseActivity {

    String path;
    SubsamplingScaleImageView image;
    VideoView video;
    GifImageView gif;
    boolean isImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_full_image_tb);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        Bundle data = getIntent().getExtras();
        ArrayList<String> fileNames = data.getStringArrayList("fileNames");
        AtomicInteger position = new AtomicInteger(data.getInt("position"));
        System.out.println("position: " + position);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        path = prefs.getString("path", Environment.DIRECTORY_PICTURES);

        image = findViewById(R.id.activity_full_image_iv_image);
        video = findViewById(R.id.activity_full_image_vv_video);
        gif = findViewById(R.id.activity_full_image_giv_gif);
        image.setVisibility(View.GONE);
        video.setVisibility(View.GONE);
        gif.setVisibility(View.GONE);
        loadMedia(fileNames.get(position.get()));

//        iteratorOtl = (v, event) -> {
//            if (event.getAction() == MotionEvent.ACTION_UP) {
//                v.performClick();
//                float x = event.getX();
//                if (x / screenWidth > 0.8 && position.get() < fileNames.size()-1) {
//                    position.getAndIncrement();
//                } else if (x / screenWidth < 0.2 && position.get() > 0) {
//                    position.getAndDecrement();
//                }
//                loadMedia(fileNames.get(position.get()));
//            }
//            return true;
//        };
//        image.setOnTouchListener(iteratorOtl);
//        video.setOnTouchListener(iteratorOtl);
//        gif.setOnTouchListener(iteratorOtl);
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_full_image; }

    private void loadMedia(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf("."));
        if (Constants.IMAGE_EXTENSIONS.contains(extension)) {
            System.out.println(fileName + " is image");
            isImage = true;
            image.setVisibility(View.VISIBLE);
            video.setVisibility(View.GONE);
            gif.setVisibility(View.GONE);
            if (video.isPlaying()) {
                video.stopPlayback();
            }
            File f = new File(path + "/" + fileName);
            try {
                image.setImage(ImageSource.uri(Uri.fromFile(f)));
            } catch (NullPointerException e) {
                image.setImage(ImageSource.resource(R.drawable.null_icon));
            }
        } else if (Constants.VIDEO_EXTENSIONS.contains(extension)) {
            System.out.println(fileName + " is video");
            isImage = false;
            video.setVisibility(View.VISIBLE);
            image.setVisibility(View.GONE);
            gif.setVisibility(View.GONE);

            video.setVideoPath(path + "/" + fileName);
            video.start();
            video.setOnCompletionListener(mp -> video.start());
        } else if (extension.equals(".gif")) {
            System.out.println(fileName + " is a gif");
            isImage = false;
            gif.setVisibility(View.VISIBLE);
            image.setVisibility(View.GONE);
            video.setVisibility(View.GONE);

            File f = new File(path + "/" + fileName);
            if (f.exists()) {
                GifDrawable gifDrawable;
                try {
                    gifDrawable = new GifDrawable(f);
                    gif.setImageDrawable(gifDrawable);
                    gifDrawable.start();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Cannot show gif " + fileName, Toast.LENGTH_SHORT).show();
                    gif.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
                }
            }
        }
    }

//    public boolean onCreateOptionsMenu(Menu m) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_full_image_toolbar, m);
//        return true;
//    }

//    @SuppressLint("ClickableViewAccessibility")
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.menu_togglezoom) {
//            boolean isChecked = item.isChecked();
//            if (isChecked) {
//                if (isImage) {
//                    image.setOnTouchListener(iteratorOtl);
//                } else {
//                    video.setOnTouchListener(iteratorOtl);
//                }
//            } /*else {
//                if (isImage) {
//                    image.setOnTouchListener(ZoomableImageView.otl);
//                }
//            }*/
//            item.setChecked(!isChecked);
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}