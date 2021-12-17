package ca.quadrexium.velonathea;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import ca.quadrexium.velonathea.pojo.Constants;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class ViewPagerAdapter extends PagerAdapter {

    Context ctx;
    //ArrayList<Media> mediaList;
    ArrayList<String> fileNames;
    LayoutInflater layoutInflater;
    String path;
    SubsamplingScaleImageView imageView;
    VideoView videoView;
    GifImageView gifImageView;

    public ViewPagerAdapter(Context ctx, ArrayList<String> fileNames, String path) {
        this.ctx = ctx;
        this.fileNames = fileNames;
        layoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.path = path;
    }

    @Override
    public int getCount() {
        return fileNames.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        View itemView = layoutInflater.inflate(R.layout.activity_full_media_views, container, false);

        String fileName = fileNames.get(position);
        String extension = fileName.substring(fileName.lastIndexOf("."));

        imageView = itemView.findViewById(R.id.activity_full_media_iv_image);
        videoView = itemView.findViewById(R.id.activity_full_media_vv_video);
        gifImageView = itemView.findViewById(R.id.activity_full_media_giv_gif);
        imageView.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        gifImageView.setVisibility(View.GONE);

        if (Constants.IMAGE_EXTENSIONS.contains(extension)) {
            imageView.setVisibility(View.VISIBLE);

            imageView.setImage(ImageSource.uri(Uri.fromFile(new File(path + "/" + fileName))));
        } else if (Constants.VIDEO_EXTENSIONS.contains(extension)) {
            videoView.setVisibility(View.VISIBLE);

            videoView.setVideoPath(path + "/" + fileName);
            videoView.start();
            videoView.setOnCompletionListener(mpa -> videoView.start());

//            videoView.setOnPreparedListener(mp -> videoView.setOnClickListener(v -> {
//                mp.setLooping(true);
//            }));

        } else if (extension.equals(".gif")) {
            gifImageView.setVisibility(View.VISIBLE);

            File f = new File(path + "/" + fileName);
            if (f.exists()) {
                GifDrawable gifDrawable;
                try {
                    gifDrawable = new GifDrawable(f);
                    gifImageView.setImageDrawable(gifDrawable);
                    gifDrawable.start();
                } catch (Exception e) {
                    gifImageView.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
                }
            }
        }

        Objects.requireNonNull(container).addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object object) {

        container.removeView((View) object);
    }
}
