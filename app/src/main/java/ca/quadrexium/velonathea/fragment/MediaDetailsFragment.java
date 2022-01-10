package ca.quadrexium.velonathea.fragment;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.activity.SearchResultsActivity;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Media;

//TODO: Reuse this fragment so the author list doesn't have to be queried every time
//TODO: Fix tag layout so it doesn't refresh when tags are added or removed
public class MediaDetailsFragment extends DialogFragment {

    private Media media;
    private final int position;

    /**
     * @param position the position of the media in the parent activity's mediaList
     * @param media the media shown
     */
    public MediaDetailsFragment(int position, Media media) {
        this.position = position;
        this.media = media;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        System.out.println(media.toString());
        EditText etFileName = view.findViewById(R.id.fragment_media_details_et_filename);
        EditText etName = view.findViewById(R.id.fragment_media_details_et_name);
        AutoCompleteTextView autocTvAuthor = view.findViewById(R.id.fragment_media_details_autoctv_author);
        EditText etLink = view.findViewById(R.id.fragment_media_details_et_link);
        EditText etTag = view.findViewById(R.id.fragment_media_details_et_tag);

        ImageButton iBtnFileName = view.findViewById(R.id.fragment_media_details_btn_clearfilename);
        ImageButton iBtnName = view.findViewById(R.id.fragment_media_details_btn_clearname);
        ImageButton iBtnAuthor = view.findViewById(R.id.fragment_media_details_btn_clearauthor);
        ImageButton iBtnLink = view.findViewById(R.id.fragment_media_details_btn_clearlink);
        ImageButton iBtnTag = view.findViewById(R.id.fragment_media_details_btn_cleartag);
        iBtnFileName.setOnClickListener(v -> etFileName.setText(""));
        iBtnName.setOnClickListener(v -> etName.setText(""));
        iBtnAuthor.setOnClickListener(v -> autocTvAuthor.setText(""));
        iBtnLink.setOnClickListener(v -> etLink.setText(""));
        iBtnTag.setOnClickListener(v -> etTag.setText(""));

        Button btnUpdate = view.findViewById(R.id.fragment_media_details_btn_update);

        if (media.getLink() == null || (media.getTags() == null || media.getTags().size() == 0)) {
            MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getReadableDatabase();
            try {
                media = myOpenHelper.depthMediaQuery(db, media.getId());
                db.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Set<String> tags;
        if (media.getTags() != null) {
            tags = new LinkedHashSet<>(media.getTags());
        } else {
            tags = new LinkedHashSet<>();
        }

        etFileName.setText(media.getFileName());
        etName.setText(media.getName());
        autocTvAuthor.setText(media.getAuthor());
        etLink.setText(media.getLink());
        refreshTags(view, tags);

        Set<String> authors = new HashSet<>();
        AtomicBoolean gotAuthors = new AtomicBoolean(false);
        AtomicBoolean gettingAuthors = new AtomicBoolean(false);
        autocTvAuthor.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    if (!gotAuthors.get() && !gettingAuthors.get()) {
                        gettingAuthors.set(true);
                        MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                        SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                        authors.addAll(myOpenHelper.getAuthorSet(db)); //to avoid lambda final
                        handler.post(() -> {
                            autocTvAuthor.setAdapter(new ArrayAdapter<>(getContext(),
                                    R.layout.textview_autocomplete,
                                    new ArrayList<>(authors)));
                        });
                    }
                    gotAuthors.set(true);
                    gettingAuthors.set(false);
                });
            }
        });

        etTag.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String newTag = etTag.getText().toString().trim();
                if (!tags.contains(newTag)) {
                    etTag.setText("");
                    tags.add(newTag);
                    refreshTags(view, tags);
                    return true;
                }
            }
            return false;
        });

        //Update button, update media in database and send updated media to SearchResultsActivity
        btnUpdate.setOnClickListener(v -> {

            boolean changed = false;
            String newMediaName = etName.getText().toString().trim();
            if (!newMediaName.equals("") && !media.getName().equals(newMediaName)) {
                changed = true;
                media.setName(newMediaName);
            }
            String newMediaFileName = etFileName.getText().toString().trim();
            if (!newMediaFileName.equals("") && !media.getFileName().equals(newMediaFileName)) {
                changed = true;
                media.setFileName(newMediaFileName);
            }
            String newMediaAuthor = autocTvAuthor.getText().toString().trim();
            if (!newMediaAuthor.equals("") && !media.getAuthor().equals(newMediaAuthor)) {
                changed = true;
                media.setAuthor(newMediaAuthor);
            }
            String newMediaLink = etLink.getText().toString().trim();
            if (!newMediaLink.equals("") && !media.getLink().equals(newMediaLink)) {
                changed = true;
                media.setLink(newMediaLink);
            }

            if (!tags.equals(media.getTags())) {
                changed = true;
                media.setTags(new HashSet<>(tags));
            }
            System.out.println(media.toString());

            String toastText = "no change";
            SearchResultsActivity parentActivity = ((SearchResultsActivity)getContext());
            if (changed) {
                MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                boolean wasUpdated = myOpenHelper.updateMedia(db, media);
                toastText = wasUpdated ? "updated" : "no change";
                if (wasUpdated) {
                    if (parentActivity != null) {
                        parentActivity.mediaChanged(position, media);
                    }
                } else {
                    toastText = "error updating media";
                }
                db.close();
            }
            Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
            if (parentActivity != null) {
                parentActivity.cancelDataLoading(false);
            }
            dismiss();
        });
    }

    private void refreshTags(View view, Set<String> tags) {
        RelativeLayout tagLayout = view.findViewById(R.id.fragment_media_details_rl_tags);
        final int[] maxWidth = new int[1];
        final int[] layoutHeight = new int[1];
        tagLayout.post(new Runnable() {
            @Override
            public void run() {
                maxWidth[0] = tagLayout.getMeasuredWidth();
                layoutHeight[0] = tagLayout.getMeasuredHeight();
            }
        });

        ViewGroup.LayoutParams tagParams = tagLayout.getLayoutParams();
        tagParams.height = layoutHeight[0];
        tagLayout.setLayoutParams(tagParams);
        tagLayout.removeAllViews();

        final int[] currentWidth = {0};

        final TextView[] prev = {null};
        final TextView[] above = {null};

        for (String tag : tags) {
            TextView tv = new TextView(getContext());
            tv.setText(tag);
            tv.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            tv.setId(View.generateViewId());
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.black));
            tv.setPadding(3, 0, 3, 0);
            tv.setSingleLine();
            tv.setOnClickListener(v1 -> {
                tagLayout.removeView(tv);
                tags.remove(tv.getText().toString());
                refreshTags(view, tags);
            });

            tagLayout.addView(tv);
        }

        tagLayout.post(() ->  {

            int count = tagLayout.getChildCount();
            for (int i = 0; i < count; i++) {
                TextView tv = (TextView) tagLayout.getChildAt(i);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
                params.setMargins(10, 5, 10, 0);
                if (prev[0] != null) {
                    params.addRule(RelativeLayout.END_OF, prev[0].getId());
                }
                if (above[0] != null) {
                    params.addRule(RelativeLayout.BELOW, above[0].getId());
                }

                int tvWidth = tv.getMeasuredWidth();

                if (prev[0] != null && (currentWidth[0] + tvWidth +
                        params.leftMargin + params.rightMargin +
                        tv.getPaddingStart() + tv.getPaddingEnd()) > maxWidth[0]) {
                    above[0] = prev[0];
                    params.removeRule(RelativeLayout.END_OF);
                    params.addRule(RelativeLayout.BELOW, above[0].getId());
                    currentWidth[0] = tvWidth + params.leftMargin + params.rightMargin +
                            tv.getPaddingStart() + tv.getPaddingEnd();
                } else {
                    currentWidth[0] += tvWidth + params.leftMargin + params.rightMargin +
                            tv.getPaddingStart() + tv.getPaddingEnd();
                }
                tv.setLayoutParams(params);
                prev[0] = tv;
            }
        });
        tagParams = tagLayout.getLayoutParams();
        tagParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        tagLayout.setLayoutParams(tagParams);
    }
}