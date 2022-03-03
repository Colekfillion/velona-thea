package ca.quadrexium.velonathea.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

    ViewPager2 vp;
    ViewPagerAdapter vpAdapter;
    String path;
    ArrayList<Media> mediaList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();

        mediaList = loadMediaFromCache(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath());

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        path = prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES);

        vp = findViewById(R.id.activity_full_media_vp);
        vp.setAdapter(vpAdapter = new ViewPagerAdapter());
        vp.setCurrentItem(data.getInt(Constants.POSITION), false);
    }

    @Override
    protected void isVerified() { }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_full_media; }

    /**
     * A recyclerview adapter that shows different types of media - images, video, and gifs.
     */
    public class ViewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            String fileName = mediaList.get(position).getFileName();
            String extension = fileName.substring(fileName.lastIndexOf("."));
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

            File f = new File(mediaList.get(position).getFilePath());
            if (f.exists()) {
                switch (holder.getItemViewType()) {
                    case Constants.MEDIA_TYPE_IMAGE:
                        assert holder instanceof ViewHolderImage;
                        ViewHolderImage holderImage = (ViewHolderImage) holder;

                        holderImage.ivImage.setImage(ImageSource.uri(Uri.fromFile(f)));
                        break;
                    case Constants.MEDIA_TYPE_VIDEO:
                        assert holder instanceof ViewHolderVideo;
                        ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                        holderVideo.vvVideo.setVideoURI(Uri.fromFile(f));
                        break;
                    case Constants.MEDIA_TYPE_GIF:
                        assert holder instanceof ViewHolderGif;
                        ViewHolderGif holderGif = (ViewHolderGif) holder;

                        try {
                            GifDrawable gifDrawable = new GifDrawable(f);
                            holderGif.givGif.setImageDrawable(gifDrawable);
                            gifDrawable.start();
                        } catch (Exception e) {
                            holderGif.givGif.setImageBitmap(
                                    BitmapFactory.decodeFile(f.getAbsolutePath()));
                        }
                        break;
                }
            //If the media file could not be found
            } else {
                switch (holder.getItemViewType()) {
                    case Constants.MEDIA_TYPE_IMAGE:
                        ViewHolderImage holderImage = (ViewHolderImage) holder;

                        holderImage.ivImage.setVisibility(View.GONE);
                        break;
                    case Constants.MEDIA_TYPE_VIDEO:
                        assert holder instanceof ViewHolderVideo;
                        ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                        holderVideo.vvVideo.setVideoURI(null);
                        break;
                    case Constants.MEDIA_TYPE_GIF:
                        assert holder instanceof ViewHolderGif;
                        ViewHolderGif holderGif = (ViewHolderGif) holder;

                        holderGif.givGif.setImageURI(null);
                        break;
                }
            }
        }

        //When the view is on screen
        @Override
        public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
            if (holder.getItemViewType() == Constants.MEDIA_TYPE_VIDEO) {
                assert holder instanceof ViewHolderVideo;
                ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                startVideo(holderVideo.vvVideo);
            }
        }

        //When the view goes off screen
        @Override
        public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
            if (holder.getItemViewType() == Constants.MEDIA_TYPE_VIDEO) {
                assert holder instanceof ViewHolderVideo;
                ViewHolderVideo holderVideo = (ViewHolderVideo) holder;

                stopVideo(holderVideo.vvVideo);
            }
        }

        @Override
        public int getItemCount() { return mediaList.size(); }

        //3 different view types for each type of media
        public class ViewHolderImage extends RecyclerView.ViewHolder {
            private final SubsamplingScaleImageView ivImage;

            public ViewHolderImage(View view) {
                super(view);
                ivImage = view.findViewById(R.id.activity_full_media_iv_image);
            }
        }

        public class ViewHolderVideo extends RecyclerView.ViewHolder {
            private final VideoView vvVideo;

            public ViewHolderVideo(View view) {
                super(view);
                vvVideo = view.findViewById(R.id.activity_full_media_vv_video);
            }
        }

        public class ViewHolderGif extends RecyclerView.ViewHolder {
            private final GifImageView givGif;

            public ViewHolderGif(View view) {
                super(view);
                givGif = view.findViewById(R.id.activity_full_media_giv_gif);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoView vvVideo = findViewById(R.id.activity_full_media_vv_video);

        stopVideo(vvVideo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoView vvVideo = findViewById(R.id.activity_full_media_vv_video);

        startVideo(vvVideo);
    }

    /**
     * Starts the specified videoview.
     * @param vvVideo the videoview to start
     */
    public void startVideo(VideoView vvVideo) {
         if (vvVideo != null && !vvVideo.isPlaying()){
             vvVideo.setOnPreparedListener(mp -> vvVideo.start());
             vvVideo.setOnCompletionListener(mpa -> vvVideo.start());
        }
    }

    /**
     * Stops the specified videoview.
     * @param vvVideo the videoview to stop
     */
    public void stopVideo(VideoView vvVideo) {
        if (vvVideo != null && vvVideo.isPlaying()) {
            vvVideo.stopPlayback();
            vvVideo.seekTo(0);
        }
    }


    @Override
    public void onBackPressed() {
        //Return to SearchResultsActivity, set the position to scroll to
        Intent dataToReturn = new Intent();
        dataToReturn.putExtra(Constants.MEDIA_LAST_POSITION, vp.getCurrentItem());
        setResult(Activity.RESULT_OK, dataToReturn);
        finish();
        super.onBackPressed();
    }
}