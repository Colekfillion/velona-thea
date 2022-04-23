package ca.quadrexium.velonathea.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.database.Query;
import ca.quadrexium.velonathea.pojo.Constants;

public class MainActivity extends BaseActivity {

    Set<String> tagFilters;
    Set<String> authorFilters;

    //Set<String> tagFiltersExclude;
    Set<String> authorFiltersExclude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);
        createNotificationChannel();

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);

        SwitchCompat swtchRandom = findViewById(R.id.activity_main_swtch_random);
        swtchRandom.setChecked(prefs.getBoolean(Constants.PREFS_RANDOM_ORDER, false));

        EditText etFileName = findViewById(R.id.activity_main_et_filename);
        EditText etName = findViewById(R.id.activity_main_et_name);

        AutoCompleteTextView autocTvAuthor = findViewById(R.id.activity_main_autoctv_author);
        authorFilters = new LinkedHashSet<>();
        autocTvAuthor.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String newAuthor = autocTvAuthor.getText().toString().trim();
                if (!authorFilters.contains(newAuthor)) {
                    authorFilters.add(newAuthor);
                    autocTvAuthor.setText("");
                    RelativeLayout layout = findViewById(R.id.activity_main_rl_authors);
                    refreshLayout(layout, authorFilters);
                    return true;
                }
            }
            return false;
        });
        autocTvAuthor.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == 1 && event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                String newAuthor = autocTvAuthor.getText().toString().trim();
                if (!authorFilters.contains(newAuthor)) {
                    authorFilters.add(newAuthor);
                    autocTvAuthor.setText("");
                    RelativeLayout layout = findViewById(R.id.activity_main_rl_authors);
                    refreshLayout(layout, authorFilters);
                    return true;
                }
            }
            return false;
        });

        AutoCompleteTextView autocTvAuthorExclude = findViewById(R.id.activity_main_autoctv_author_exclude);
        authorFiltersExclude = new LinkedHashSet<>();
        autocTvAuthorExclude.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String newAuthor = autocTvAuthorExclude.getText().toString().trim();
                if (!authorFiltersExclude.contains(newAuthor)) {
                    authorFiltersExclude.add(newAuthor);
                    autocTvAuthorExclude.setText("");
                    RelativeLayout layout = findViewById(R.id.activity_main_rl_authors_exclude);
                    refreshLayout(layout, authorFiltersExclude);
                    return true;
                }
            }
            return false;
        });
        autocTvAuthorExclude.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == 2 && event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                String newAuthor = autocTvAuthorExclude.getText().toString().trim();
                if (!authorFiltersExclude.contains(newAuthor)) {
                    authorFiltersExclude.add(newAuthor);
                    autocTvAuthorExclude.setText("");
                    RelativeLayout layout = findViewById(R.id.activity_main_rl_authors_exclude);
                    refreshLayout(layout, authorFiltersExclude);
                    return true;
                }
            }
            return false;
        });

        AutoCompleteTextView autocTvTag = findViewById(R.id.activity_main_autoctv_tag);
        tagFilters = new LinkedHashSet<>();
        autocTvTag.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String newTag = autocTvTag.getText().toString().trim();
                if (!tagFilters.contains(newTag)) {
                    tagFilters.add(newTag);
                    autocTvTag.setText("");
                    RelativeLayout layout = findViewById(R.id.activity_main_rl_tags);
                    refreshLayout(layout, tagFilters);
                    return true;
                }
            }
            return false;
        });
        autocTvTag.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == 3 && event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                String newTag = autocTvTag.getText().toString().trim();
                if (!tagFilters.contains(newTag)) {
                    tagFilters.add(newTag);
                    autocTvTag.setText("");
                    RelativeLayout layout = findViewById(R.id.activity_main_rl_tags);
                    refreshLayout(layout, tagFilters);
                    return true;
                }
            }
            return false;
        });

//        AutoCompleteTextView autocTvTagExclude = findViewById(R.id.activity_main_autoctv_tag_exclude);
//        tagFiltersExclude = new LinkedHashSet<>();
//        autocTvTagExclude.setOnKeyListener((v, keyCode, event) -> {
//            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
//                String newTag = autocTvTagExclude.getText().toString().trim();
//                if (!tagFiltersExclude.contains(newTag)) {
//                    tagFiltersExclude.add(newTag);
//                    autocTvTagExclude.setText("");
//                    RelativeLayout layout = findViewById(R.id.activity_main_rl_tags_exclude);
//                    refreshLayout(layout, tagFiltersExclude);
//                    return true;
//                }
//            }
//            return false;
//        });
//        autocTvTagExclude.setOnEditorActionListener((v, actionId, event) -> {
//            if (actionId == 4 && event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
//                String newTag = autocTvTagExclude.getText().toString().trim();
//                if (!tagFiltersExclude.contains(newTag)) {
//                    tagFiltersExclude.add(newTag);
//                    autocTvTagExclude.setText("");
//                    RelativeLayout layout = findViewById(R.id.activity_main_rl_tags_exclude);
//                    refreshLayout(layout, tagFiltersExclude);
//                    return true;
//                }
//            }
//            return false;
//        });

        //Buttons to clear edittexts
        ImageButton iBtnFileName = findViewById(R.id.activity_main_btn_clearfilename);
        ImageButton iBtnName = findViewById(R.id.activity_main_btn_clearname);
        iBtnFileName.setOnClickListener(v -> etFileName.setText(""));
        iBtnName.setOnClickListener(v -> etName.setText(""));

        RadioGroup rgTagType = findViewById(R.id.activity_main_rg_tagtype);
        RadioButton checkedRadioButton;
        String tagType = prefs.getString(Constants.PREFS_TAGTYPE, "similar");
        if ("matchall".equals(tagType)) {
            checkedRadioButton = findViewById(R.id.activity_main_rg_tagtype_matchall);
        } else {
            checkedRadioButton = findViewById(R.id.activity_main_rg_tagtype_similar);
        }
        checkedRadioButton.setChecked(true);

        //Author autocomplete
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
                        MyOpenHelper myOpenHelper = getMyOpenHelper();
                        SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                        authors.addAll(myOpenHelper.getAuthorSet(db)); //to avoid lambda final
                        db.close();
                        handler.post(() -> {
                            autocTvAuthor.setAdapter(new ArrayAdapter<>(this,
                                    R.layout.textview_autocomplete,
                                    new ArrayList<>(authors)));
                        });
                    }
                    gotAuthors.set(true);
                    gettingAuthors.set(false);
                });
            }
        });

        RadioGroup rgMediaType = findViewById(R.id.activity_main_rg_mediatype);

        Button btnSearch = findViewById(R.id.activity_main_btn_search);
        btnSearch.setOnClickListener(v -> {

            String fileName = etFileName.getText().toString();
            String name = etName.getText().toString();
            String author = autocTvAuthor.getText().toString();
            String mediaType = "";
            int checkedMediaRadioButtonId = rgMediaType.getCheckedRadioButtonId();
            if (checkedMediaRadioButtonId == R.id.activity_main_rg_mediatype_images) {
                mediaType = Constants.IMAGE;
            } else if (checkedMediaRadioButtonId == R.id.activity_main_rg_mediatype_videos) {
                mediaType = Constants.VIDEO;
            }

            Query.Builder builder = new Query.Builder();
            builder.select(MyOpenHelper.COL_MEDIA_ID, MyOpenHelper.MEDIA_TABLE)
                    .select(MyOpenHelper.COL_MEDIA_PATH, MyOpenHelper.MEDIA_TABLE)
                    .from(MyOpenHelper.MEDIA_TABLE);

            if (!Constants.isStringEmpty(fileName)) {
                ArrayList<String> filePathList = new ArrayList<>();
                filePathList.add("%" + fileName + "%");
                builder.whereCondition(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_PATH, filePathList, true, true, false);
            }

            if (!Constants.isStringEmpty(name)) {
                ArrayList<String> nameList = new ArrayList<>();
                nameList.add("%" + name + "%");
                builder.whereCondition(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_NAME, nameList, true, true, false);
            }

            if (!Constants.isStringEmpty(author)) {
                authorFilters.add(author);
                autocTvAuthor.setText("");
                RelativeLayout layout = findViewById(R.id.activity_main_rl_authors);
                refreshLayout(layout, authorFilters);
            }

            if (!Constants.isStringEmpty(autocTvAuthorExclude.getText().toString())) {
                authorFiltersExclude.add(autocTvAuthorExclude.getText().toString());
                autocTvAuthorExclude.setText("");
                RelativeLayout layout = findViewById(R.id.activity_main_rl_authors_exclude);
                refreshLayout(layout, authorFiltersExclude);
            }

            if (!Constants.isStringEmpty(autocTvTag.getText().toString())) {
                tagFilters.add(autocTvTag.getText().toString());
                autocTvTag.setText("");
                RelativeLayout layout = findViewById(R.id.activity_main_rl_tags);
                refreshLayout(layout, tagFilters);
            }

//            if (!Constants.isStringEmpty(autocTvTagExclude.getText().toString())) {
//                tagFiltersExclude.add(autocTvTagExclude.getText().toString());
//                autocTvTagExclude.setText("");
//                RelativeLayout layout = findViewById(R.id.activity_main_rl_tags_exclude);
//                refreshLayout(layout, tagFiltersExclude);
//            }

            if (authorFilters.size() > 0) {
                builder.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_AUTHOR_ID, MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_ID);
                ArrayList<String> authorList = new ArrayList<>(authorFilters);
                builder.whereCondition(MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_NAME, authorList, true, false, false);
            }

            if (authorFiltersExclude.size() > 0) {
                builder.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_AUTHOR_ID, MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_ID);
                ArrayList<String> authorList = new ArrayList<>(authorFiltersExclude);
                builder.whereCondition(MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_NAME, authorList, false, true, true);
            }

            if (tagFilters.size() > 0) {
                int checkedTagRadioButtonId = rgTagType.getCheckedRadioButtonId();
                if (checkedTagRadioButtonId == R.id.activity_main_rg_tagtype_matchall) {
                    builder.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID, MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID);

                    MyOpenHelper myOpenHelper = getMyOpenHelper();
                    SQLiteDatabase db = myOpenHelper.getReadableDatabase();

                    ArrayList<String> tagList = new ArrayList<>();
                    int numValidTags = 0;
                    for (String tag : tagFilters) {
                        int tagId = myOpenHelper.getTagId(db, tag);
                        if (tagId != -1) {
                            numValidTags++;
                            tagList.add(String.valueOf(tagId));
                        }
                    }
                    db.close();
                    if (numValidTags > 0) {
                        builder.whereCondition(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_TAG_ID, tagList, false, false, false);
                        builder.having(numValidTags);
                    }

                } else if (checkedTagRadioButtonId == R.id.activity_main_rg_tagtype_similar) {
                    builder.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID, MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID);
                    ArrayList<String> tagIds = new ArrayList<>();
                    MyOpenHelper myOpenHelper = getMyOpenHelper();
                    SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                    for (String tag : tagFilters) {
                        tagIds.addAll(myOpenHelper.getSimilarTagIds(db, tag));
                    }
                    db.close();
                    if (tagIds.size() > 0) {
                        builder.whereIn(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_TAG_ID, tagIds, false, tagFilters.size());
                    }
                }

            }

//            if (tagFiltersExclude.size() > 0) {
//                int checkedTagRadioButtonId = rgTagType.getCheckedRadioButtonId();
//                if (checkedTagRadioButtonId == R.id.activity_main_rg_tagtype_matchall) {
//                    builder.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID, MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID);
//
//                    MyOpenHelper myOpenHelper = getMyOpenHelper();
//                    SQLiteDatabase db = myOpenHelper.getReadableDatabase();
//
//                    ArrayList<String> tagList = new ArrayList<>();
//                    for (String tag : tagFiltersExclude) {
//                        int tagId = myOpenHelper.getTagId(db, tag);
//                        tagList.add(String.valueOf(tagId));
//                    }
//                    db.close();
//                    builder.whereCondition(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_TAG_ID, tagList, false, true, true);
//
//                } else if (checkedTagRadioButtonId == R.id.activity_main_rg_tagtype_similar) {
//                    builder.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID, MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID);
//                    ArrayList<String> tagIds = new ArrayList<>();
//                    MyOpenHelper myOpenHelper = getMyOpenHelper();
//                    SQLiteDatabase db = myOpenHelper.getReadableDatabase();
//                    for (String tag : tagFiltersExclude) {
//                        tagIds.addAll(myOpenHelper.getSimilarTagIds(db, tag));
//                    }
//                    db.close();
//                    if (tagIds.size() == 0) {
//                        tagIds.add("-2");
//                    }
//                    builder.whereIn(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_TAG_ID, tagIds, true, tagFilters.size());
//                }
//
//            }

            switch (mediaType) {
                case Constants.IMAGE:
                    ArrayList<String> imageExtensions = new ArrayList<>(Constants.IMAGE_EXTENSIONS);
                    builder.whereCondition(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_PATH, imageExtensions, true, false, false);
                    break;

                case Constants.VIDEO:
                    ArrayList<String> videoExtensions = new ArrayList<>(Constants.VIDEO_EXTENSIONS);
                    videoExtensions.add(".gif");
                    builder.whereCondition(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_PATH, videoExtensions, true, false, false);
                    break;

                default:

                    break;
            }

            builder.groupBy(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_PATH);

            if (swtchRandom.isChecked()) {
                builder.orderByRandom();
            }

            String query = builder.build().getQuery();
            String[] args = new String[0];

            Bundle dataToPass = new Bundle();

            dataToPass.putString(Constants.MEDIA_QUERY, query);
            dataToPass.putStringArray(Constants.QUERY_ARGS, args);
            Log.d("DB", "Query: " + query);
            Log.d("DB", "Args: " + Arrays.toString(args));

            Intent intent = new Intent(this, SearchResultsActivity.class);
            intent.putExtras(dataToPass);
            startActivity(intent);
        });
    }

    @Override
    protected void isVerified() { }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_main; }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        SwitchCompat randomOrder = findViewById(R.id.activity_main_swtch_random);
        edit.putBoolean(Constants.PREFS_RANDOM_ORDER, randomOrder.isChecked());

        RadioGroup rgTagType = findViewById(R.id.activity_main_rg_tagtype);
        int checkedRadioButtonId = rgTagType.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.activity_main_rg_tagtype_similar) {
            edit.putString(Constants.PREFS_TAGTYPE, "similar");
        } else if (checkedRadioButtonId == R.id.activity_main_rg_tagtype_matchall) {
            edit.putString(Constants.PREFS_TAGTYPE, "matchall");
        }

        edit.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Delete the query cache if it exists
        File queryCache = new File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath() + "/" + Constants.QUERY_CACHE_FILENAME);
        if (queryCache.exists()) {
            boolean cacheDeleted = queryCache.delete();
            if (!cacheDeleted) {
                Toast.makeText(this, R.string.fail_delete_cache, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Refreshes a relativelayout, adding new text based on a given dataset
     * @param layout the layout to refresh
     * @param dataset the dataset to populate the layout
     */
    private void refreshLayout(RelativeLayout layout, Set<String> dataset) {

        layout.removeAllViewsInLayout();

        int maxWidth = layout.getMeasuredWidth();
        final int[] currentWidth = {0};

        final TextView[] prev = {null};
        final TextView[] above = {null};

        for (String value : dataset) {
            TextView tv = new TextView(this);
            tv.setText(value);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
            tv.setId(View.generateViewId());
            tv.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
            tv.setPadding(3, 0, 3, 0);
            tv.setSingleLine();
            tv.setOnClickListener(v1 -> {
                layout.removeView(tv);
                dataset.remove(tv.getText().toString());
                refreshLayout(layout, dataset);
            });

            layout.addView(tv);
        }

        layout.post(() ->  {

            int count = layout.getChildCount();
            for (int i = 0; i < count; i++) {
                TextView tv = (TextView) layout.getChildAt(i);
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
                        tv.getPaddingStart() + tv.getPaddingEnd()) > maxWidth) {
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
    }

    @Override
    protected String getName() {
        return "MainActivity";
    }
}