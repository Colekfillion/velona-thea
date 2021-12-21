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

    public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewPagerAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                    R.layout.activity_full_media_views, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewPagerAdapter.ViewHolder holder, int position) {

            holder.imageView.setVisibility(View.GONE);
            holder.videoView.setVisibility(View.GONE);
            holder.gifImageView.setVisibility(View.GONE);

            String fileName = fileNames.get(position);
            String extension = fileName.substring(fileName.lastIndexOf("."));

            if (Constants.IMAGE_EXTENSIONS.contains(extension)) {
                holder.imageView.setVisibility(View.VISIBLE);

                holder.imageView.setImage(ImageSource.uri(Uri.fromFile(new File(path + "/" + fileName))));
            } else if (Constants.VIDEO_EXTENSIONS.contains(extension)) {
                holder.videoView.setVisibility(View.VISIBLE);

                holder.videoView.setVideoPath(path + "/" + fileName);
                holder.videoView.start();
                holder.videoView.setOnCompletionListener(mpa -> holder.videoView.start());

//            videoView.setOnPreparedListener(mp -> videoView.setOnClickListener(v -> {
//                mp.setLooping(true);
//            }));

            } else if (extension.equals(".gif")) {
                holder.gifImageView.setVisibility(View.VISIBLE);

                File f = new File(path + "/" + fileName);
                if (f.exists()) {
                    GifDrawable gifDrawable;
                    try {
                        gifDrawable = new GifDrawable(f);
                        holder.gifImageView.setImageDrawable(gifDrawable);
                        gifDrawable.start();
                    } catch (Exception e) {
                        holder.gifImageView.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
                    }
                }
            }
        }

        @Override
        public int getItemCount() { return fileNames.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private final SubsamplingScaleImageView imageView;
            private final VideoView videoView;
            private final GifImageView gifImageView;

            public ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.activity_full_media_iv_image);
                videoView = view.findViewById(R.id.activity_full_media_vv_video);
                gifImageView = view.findViewById(R.id.activity_full_media_giv_gif);
            }
        }
    }
}