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
import ca.quadrexium.velonathea.pojo.Media;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class FullMediaActivity extends BaseActivity {

    ViewPager2 viewPager;
    ViewPagerAdapter viewPagerAdapter;
    String path;
    ArrayList<Media> mediaList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();

        mediaList = SearchResultsActivity.loadMediaFromCache(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath());

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        path = prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES);

        viewPager = findViewById(R.id.activity_full_media_vp);
        viewPager.setAdapter(viewPagerAdapter = new ViewPagerAdapter());
        viewPager.setCurrentItem(data.getInt(Constants.POSITION), false);
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_full_media; }

    public class ViewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            String extension = mediaList.get(position).getFileName().substring(mediaList.get(position).getFileName().lastIndexOf("."));
            if (Constants.IMAGE_EXTENSIONS.contains(extension)) {
                return Constants.MEDIA_TYPE_IMAGE;
            } else if (Constants.VIDEO_EXTENSIONS.contains(extension)) {
                return Constants.MEDIA_TYPE_VIDEO;
            } else if (extension.equals(".gif")) {
                return Constants.MEDIA_TYPE_GIF;
            }
            throw new IllegalStateException("Media is not an image, video, or gif");
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view;
            switch(viewType) {
                case Constants.MEDIA_TYPE_IMAGE:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(
                            R.layout.full_image, viewGroup, false);
                    return new ViewHolderImage(view);
                case Constants.MEDIA_TYPE_VIDEO:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(
                            R.layout.full_video, viewGroup, false);
                    return new ViewHolderVideo(view);
                case Constants.MEDIA_TYPE_GIF:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(
                            R.layout.full_gif, viewGroup, false);
                    return new ViewHolderGif(view);
            }
            throw new IllegalStateException("Media is not an image, video, or gif");
        }

        //When media is first scrolled to
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            File f = new File(path + "/" + mediaList.get(position).getFileName());
            if (f.exists()) {
                switch (holder.getItemViewType()) {
                    case Constants.MEDIA_TYPE_IMAGE:
                        ViewHolderImage holderImage = (ViewHolderImage) holder;

                        holderImage.imageView.setImage(ImageSource.uri(Uri.fromFile(f)));
                        break;
                    case Constants.MEDIA_TYPE_VIDEO:
                        assert holder instanceof ViewHolderVideo;
                        ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                        holderVideo.videoView.setVideoURI(Uri.fromFile(f));
                        break;
                    case Constants.MEDIA_TYPE_GIF:
                        assert holder instanceof ViewHolderGif;
                        ViewHolderGif holderGif = (ViewHolderGif) holder;

                        try {
                            GifDrawable gifDrawable = new GifDrawable(f);
                            holderGif.gifImageView.setImageDrawable(gifDrawable);
                            gifDrawable.start();
                        } catch (Exception e) {
                            holderGif.gifImageView.setImageBitmap(
                                    BitmapFactory.decodeFile(f.getAbsolutePath()));
                        }
                        break;
                }
            } else {
                switch (holder.getItemViewType()) {
                    case Constants.MEDIA_TYPE_IMAGE:
                        ViewHolderImage holderImage = (ViewHolderImage) holder;

                        holderImage.imageView.setImage(ImageSource.resource(R.drawable.null_image));
                        holderImage.imageView.setZoomEnabled(false);
                        break;
                    case Constants.MEDIA_TYPE_VIDEO:
                        assert holder instanceof ViewHolderVideo;
                        ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                        holderVideo.videoView.setVideoURI(null);
                        break;
                    case Constants.MEDIA_TYPE_GIF:
                        assert holder instanceof ViewHolderGif;
                        ViewHolderGif holderGif = (ViewHolderGif) holder;

                        holderGif.gifImageView.setImageURI(null);
                        break;
                }
            }
        }

        @Override
        public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
            if (holder.getItemViewType() == Constants.MEDIA_TYPE_VIDEO) {
                assert holder instanceof ViewHolderVideo;
                ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                toggleVideo(holderVideo.videoView);
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
            if (holder.getItemViewType() == Constants.MEDIA_TYPE_VIDEO) {
                assert holder instanceof ViewHolderVideo;
                ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                toggleVideo(holderVideo.videoView);
            }
        }

        @Override
        public int getItemCount() { return mediaList.size(); }

        //3 different view types for each type of media
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

    @Override
    protected void onPause() {
        super.onPause();
        //Pause video if it is visible, set time to 0
        VideoView videoView = findViewById(R.id.activity_full_media_vv_video);

        toggleVideo(videoView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Start video if it is visible
        VideoView videoView = findViewById(R.id.activity_full_media_vv_video);

        toggleVideo(videoView);
    }

    public void toggleVideo(VideoView videoView) {
        if (videoView != null && videoView.isPlaying()) {
            videoView.stopPlayback();
            videoView.seekTo(0);
        } else if (videoView != null){
            videoView.start();
            videoView.setOnCompletionListener(mpa -> videoView.start());
        }
    }
}