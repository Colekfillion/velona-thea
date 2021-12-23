package ca.quadrexium.velonathea.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.util.ArrayList;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.pojo.Constants;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class FullMediaActivity extends BaseActivity {

    ViewPager2 viewPager;
    ViewPagerAdapter viewPagerAdapter;
    String path;
    ArrayList<String> fileNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        fileNames = data.getStringArrayList("fileNames");

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        path = prefs.getString("path", Environment.DIRECTORY_PICTURES);

        viewPager = findViewById(R.id.activity_full_media_vp);
        viewPager.setAdapter(viewPagerAdapter = new ViewPagerAdapter());
        viewPager.setCurrentItem(data.getInt("position"), false);
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_full_media; }

    public class ViewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view;
            switch(viewType) {
                case 0:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(
                            R.layout.full_image, viewGroup, false);
                    return new ViewHolderImage(view);
                case 1:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(
                            R.layout.full_video, viewGroup, false);
                    return new ViewHolderVideo(view);
                case 2:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(
                            R.layout.full_gif, viewGroup, false);
                    return new ViewHolderGif(view);
            }
            throw new IllegalStateException("Media is not an image, video, or gif");
        }

        @Override
        public int getItemViewType(int position) {
            String extension = fileNames.get(position).substring(fileNames.get(position).lastIndexOf("."));
            if (Constants.IMAGE_EXTENSIONS.contains(extension)) {
                return 0;
            } else if (Constants.VIDEO_EXTENSIONS.contains(extension)) {
                return 1;
            } else if (extension.equals(".gif")) {
                return 2;
            }
            throw new IllegalStateException("Media is not an image, video, or gif");
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            File f = new File(path + "/" + fileNames.get(position));
            System.out.println("On file " + fileNames.get(position));
            if (f.exists()) {
                switch (holder.getItemViewType()) {
                    case 0:
                        ViewHolderImage holderImage = (ViewHolderImage) holder;

                        System.out.println(fileNames.get(position) + " is image");
                        holderImage.imageView.setImage(ImageSource.uri(Uri.fromFile(f)));
                        break;
                    case 1:
                        assert holder instanceof ViewHolderVideo;
                        ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                        System.out.println(fileNames.get(position) + " is video");
                        holderVideo.videoView.setVideoURI(Uri.fromFile(f));
                        holderVideo.videoView.start();
                        holderVideo.videoView.setOnCompletionListener(mpa -> holderVideo.videoView.start());
                        break;
                    case 2:
                        assert holder instanceof ViewHolderGif;
                        ViewHolderGif holderGif = (ViewHolderGif) holder;

                        System.out.println(fileNames.get(position) + " is gif");
                        try {
                            GifDrawable gifDrawable = new GifDrawable(f);
                            holderGif.gifImageView.setImageDrawable(gifDrawable);
                            gifDrawable.start();
                        } catch (Exception e) {
                            holderGif.gifImageView.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
                        }
                        break;
                }
            } else {
                System.out.println("Cannot find " + fileNames.get(position));
                switch (holder.getItemViewType()) {
                    case 0:
                        ViewHolderImage holderImage = (ViewHolderImage) holder;

                        holderImage.imageView.setImage(ImageSource.resource(R.drawable.null_image));
                        holderImage.imageView.setZoomEnabled(false);
                        break;
                    case 1:
                        assert holder instanceof ViewHolderVideo;
                        ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                        holderVideo.videoView.setVideoURI(null);
                        break;
                    case 2:
                        assert holder instanceof ViewHolderGif;
                        ViewHolderGif holderGif = (ViewHolderGif) holder;

                        holderGif.gifImageView.setImageURI(null);
                        break;
                }
            }
        }

        @Override
        public int getItemCount() { return fileNames.size(); }

        public class ViewHolderImage extends RecyclerView.ViewHolder {
            private final SubsamplingScaleImageView imageView;

            public ViewHolderImage(View view) {
                super(view);
                imageView = view.findViewById(R.id.activity_full_media_iv_image);
            }
        }

        public class ViewHolderVideo extends RecyclerView.ViewHolder {
            private final VideoView videoView;

            public ViewHolderVideo(View view) {
                super(view);
                videoView = view.findViewById(R.id.activity_full_media_vv_video);
            }
        }

        public class ViewHolderGif extends RecyclerView.ViewHolder {
            private final GifImageView gifImageView;

            public ViewHolderGif(View view) {
                super(view);
                gifImageView = view.findViewById(R.id.activity_full_media_giv_gif);
            }
        }
    }
}