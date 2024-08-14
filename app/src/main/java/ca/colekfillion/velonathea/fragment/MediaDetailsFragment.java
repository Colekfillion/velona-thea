package ca.colekfillion.velonathea.fragment;

import android.content.DialogInterface;
import android.database.Cursor;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.colekfillion.velonathea.R;
import ca.colekfillion.velonathea.activity.SearchResultsActivity;
import ca.colekfillion.velonathea.database.MyOpenHelper;
import ca.colekfillion.velonathea.pojo.Media;

//TODO: Reuse this fragment so the author list doesn't have to be queried every time
//TODO: Fix tag layout so it doesn't refresh when tags are added or removed
public class MediaDetailsFragment extends BaseDialogFragment {

    private Media media;
    private Set<String> mediaTags;
    private EditText etFilePath;
    private EditText etName;
    private AutoCompleteTextView autocTvAuthor;
    private EditText etLink;
    private AutoCompleteTextView autocTvTag;
    private RelativeLayout tagLayout;
    private ImageButton iBtnFilePath;
    private ImageButton iBtnName;
    private ImageButton iBtnAuthor;
    private ImageButton iBtnLink;
    private ImageButton iBtnTag;
    private Button btnUpdate;
    private int position;

    /**
     * @param position the position of the media in the parent activity's mediaList
     * @param media    the media shown
     */
    public MediaDetailsFragment(int position, Media media) {
        this.position = position;
        this.media = media;
    }

    @Override
    protected void initViews(View v) {
        etFilePath = v.findViewById(R.id.fragment_media_details_et_filepath);
        etName = v.findViewById(R.id.fragment_media_details_et_name);
        autocTvAuthor = v.findViewById(R.id.fragment_media_details_autoctv_author);
        etLink = v.findViewById(R.id.fragment_media_details_et_link);
        autocTvTag = v.findViewById(R.id.fragment_media_details_autoctv_tag);

        iBtnFilePath = v.findViewById(R.id.fragment_media_details_btn_clearfilepath);
        iBtnName = v.findViewById(R.id.fragment_media_details_btn_clearname);
        iBtnAuthor = v.findViewById(R.id.fragment_media_details_btn_clearauthor);
        iBtnLink = v.findViewById(R.id.fragment_media_details_btn_clearlink);
        iBtnTag = v.findViewById(R.id.fragment_media_details_btn_cleartag);

        tagLayout = v.findViewById(R.id.fragment_media_details_rl_tags);

        btnUpdate = v.findViewById(R.id.fragment_media_details_btn_update);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_media_details;
    }

    @Override
    protected String getName() {
        return "MediaDetailsFragment";
    }

    public void update(int position, Media media) {
        this.position = position;
        this.media = media;

        if (media.getTags() != null) {
            mediaTags = new LinkedHashSet<>(media.getTags());
        } else {
            mediaTags = new LinkedHashSet<>();
        }

        etFilePath.setText(media.getFilePath());
        etName.setText(media.getName());
        autocTvAuthor.setText(media.getAuthor());
        etLink.setText(media.getLink());
        refreshTags(tagLayout, mediaTags);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        System.out.println(media.toString());

        iBtnFilePath.setOnClickListener(v -> etFilePath.setText(""));
        iBtnName.setOnClickListener(v -> etName.setText(""));
        iBtnAuthor.setOnClickListener(v -> autocTvAuthor.setText(""));
        iBtnLink.setOnClickListener(v -> etLink.setText(""));
        iBtnTag.setOnClickListener(v -> autocTvTag.setText(""));

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

        update(position, media);

        Set<String> allAuthors = new HashSet<>();
        autocTvAuthor.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && allAuthors.size() == 0) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {

                    MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                    SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                    Cursor c = db.rawQuery("SELECT " + MyOpenHelper.AUTHOR_TABLE + "." + MyOpenHelper.COL_AUTHOR_NAME + " FROM " + MyOpenHelper.AUTHOR_TABLE,
                            new String[]{}
                    );
                    c.moveToFirst();
                    while (c.moveToNext()) {
                        allAuthors.add(c.getString(c.getColumnIndex(MyOpenHelper.COL_AUTHOR_NAME)));
                    }
                    c.close();
                    db.close();
                    handler.post(() -> autocTvAuthor.setAdapter(new ArrayAdapter<>(requireContext(),
                            R.layout.textview_autocomplete,
                            new ArrayList<>(allAuthors))));
                });
                executor.shutdown();
            }
        });

        Set<String> allTags = new HashSet<>();
        autocTvTag.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && allTags.size() == 0) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                    SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                    Cursor c = db.rawQuery("SELECT " + MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_NAME + " FROM " + MyOpenHelper.TAG_TABLE,
                            new String[]{}
                    );
                    c.moveToFirst();
                    while (c.moveToNext()) {
                        allTags.add(c.getString(c.getColumnIndex(MyOpenHelper.COL_TAG_NAME)));
                    }
                    c.close();
                    db.close();
                    handler.post(() -> autocTvTag.setAdapter(new ArrayAdapter<>(requireContext(),
                            R.layout.textview_autocomplete,
                            new ArrayList<>(allTags))));
                });
                executor.shutdown();
            }
        });

        autocTvTag.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String newTag = autocTvTag.getText().toString().trim();
                if (!mediaTags.contains(newTag)) {
                    autocTvTag.setText("");
                    mediaTags.add(newTag);
                    refreshTags(tagLayout, mediaTags);
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
            String newMediaFilePath = etFilePath.getText().toString().trim();
            if (!newMediaFilePath.equals("") && !media.getFilePath().equals(newMediaFilePath)) {
                changed = true;
                media.setFilePath(newMediaFilePath);
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

            if (!mediaTags.equals(media.getTags())) {
                changed = true;
                media.setTags(new HashSet<>(mediaTags));
            }
            System.out.println(media.toString());

            String toastText = "no change";
            SearchResultsActivity parentActivity = ((SearchResultsActivity) getContext());
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

    /**
     * Displays tags in a left->right order
     *
     * @param tagLayout a RelativeLayout where the tags will be displayed
     * @param tags      String Set of tags to display
     */
    private void refreshTags(RelativeLayout tagLayout, Set<String> tags) {
        final int[] maxWidth = new int[1];
        final int[] layoutHeight = new int[1];
        tagLayout.post(new Runnable() { //MUST NOT BE LAMBDA OR IT BREAKS
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
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tv.setId(View.generateViewId());
            tv.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black));
            tv.setPadding(3, 0, 3, 0);
            tv.setSingleLine();
            tv.setOnClickListener(v1 -> {
                tagLayout.removeView(tv);
                tags.remove(tv.getText().toString());
                refreshTags(tagLayout, tags);
            });

            tagLayout.addView(tv);
        }

        tagLayout.post(() -> {

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

    private void startLoadingMedia() {
        SearchResultsActivity parentActivity = ((SearchResultsActivity) getContext());
        if (parentActivity != null) {
            parentActivity.startLoadingMedia();
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        //getParentFragmentManager().beginTransaction().addToBackStack(Constants.FRAGMENT_MEDIA_DETAILS).commit();
        super.onDismiss(dialog);
        startLoadingMedia();
    }
}