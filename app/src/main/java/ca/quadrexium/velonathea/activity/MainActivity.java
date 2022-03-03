package ca.quadrexium.velonathea.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import ca.quadrexium.velonathea.pojo.Constants;
import ca.quadrexium.velonathea.pojo.WhereFilterHashMap;

public class MainActivity extends BaseActivity {

    Set<String> tagFilters;

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
        EditText etTag = findViewById(R.id.activity_main_et_tag);

        tagFilters = new LinkedHashSet<>();
        etTag.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String newTag = etTag.getText().toString().trim();
                if (!tagFilters.contains(newTag)) {
                    tagFilters.add(newTag);
                    etTag.setText("");
                    refreshTags();
                    return true;
                }
            }
            return false;
        });

        //Buttons to clear edittexts
        ImageButton iBtnFileName = findViewById(R.id.activity_main_btn_clearfilename);
        ImageButton iBtnName = findViewById(R.id.activity_main_btn_clearname);
        ImageButton iBtnAuthor = findViewById(R.id.activity_main_btn_clearauthor);
        ImageButton iBtnTag = findViewById(R.id.activity_main_btn_cleartag);
        iBtnFileName.setOnClickListener(v -> etFileName.setText(""));
        iBtnName.setOnClickListener(v -> etName.setText(""));
        iBtnAuthor.setOnClickListener(v -> autocTvAuthor.setText(""));
        iBtnTag.setOnClickListener(v -> etTag.setText(""));

        RadioGroup rgMediaType = findViewById(R.id.activity_main_rg_mediatype);

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

        Button btnSearch = findViewById(R.id.activity_main_btn_search);
        btnSearch.setOnClickListener(v -> {

            String fileName = etFileName.getText().toString();
            String name = etName.getText().toString();
            String author = autocTvAuthor.getText().toString();
            String mediaType = "";
            int checkedRadioButtonId = rgMediaType.getCheckedRadioButtonId();
            if (checkedRadioButtonId == R.id.activity_main_rg_mediatype_images) {
                mediaType = Constants.IMAGE;
            } else if (checkedRadioButtonId == R.id.activity_main_rg_mediatype_videos) {
                mediaType = Constants.VIDEO;
            }

            ArrayList<String> orderBy = new ArrayList<>();
            if (swtchRandom.isChecked()) {
                orderBy.add("RANDOM()");
            }

            WhereFilterHashMap whereFilters = new WhereFilterHashMap();
            if (!fileName.equals("")) {
                whereFilters.addMandatory(MyOpenHelper.COL_MEDIA_FILEPATH_ALIAS, fileName.trim());
            }
            if (!name.equals("")) {
                whereFilters.addMandatory(MyOpenHelper.COL_MEDIA_NAME_ALIAS, name.trim());
            }
            if (!author.equals("")) {
                whereFilters.addMandatory(MyOpenHelper.COL_AUTHOR_NAME_ALIAS, author.trim());
            }

            if (!mediaType.equals("")) {
                if (mediaType.equals(Constants.IMAGE)) {
                    //If there is already a filename filter, just add to that map's values
                    whereFilters.addOptional(MyOpenHelper.COL_MEDIA_FILEPATH_ALIAS, Constants.IMAGE_EXTENSIONS);
                } else if (mediaType.equals(Constants.VIDEO)) {
                    Set<String> videoExtensions = new HashSet<>(Constants.VIDEO_EXTENSIONS);
                    videoExtensions.add(".gif"); //gifs are considered videos except for viewing
                    whereFilters.addOptional(MyOpenHelper.COL_MEDIA_FILEPATH_ALIAS, videoExtensions);
                }
            }
            if ((!name.equals("") || !fileName.equals("")) && tagFilters.size() <= 1) {
                String naturalSortColumn = MyOpenHelper.MEDIA_TABLE + ".";
                naturalSortColumn += fileName.length() >= name.length() ? MyOpenHelper.COL_MEDIA_PATH : MyOpenHelper.COL_MEDIA_NAME;
                orderBy.add("LENGTH(" + naturalSortColumn + ")");
            }

            Pair<String, String[]> query;
            MyOpenHelper myOpenHelper = getMyOpenHelper();
            //This is for matching multiple tags: it creates multiple queries where tag like ?,
            // then intersects them all to find the ones where the media has all the selected tags
            if (tagFilters.size() > 0) {
                StringBuilder queryBuilder = new StringBuilder();
                ArrayList<String> selectionArgs = new ArrayList<>();
                for (String tag : tagFilters) {
                    WhereFilterHashMap temp = new WhereFilterHashMap(whereFilters);
                    temp.addMandatory(MyOpenHelper.COL_TAG_NAME_ALIAS, tag);
                    query = myOpenHelper.initialMediaQueryBuilder(temp,
                            orderBy.toArray(new String[0]));
                    queryBuilder.append(query.first);
                    queryBuilder.append("INTERSECT ");
                    selectionArgs.addAll(Arrays.asList(query.second));
                }
                queryBuilder.setLength(queryBuilder.length()-("INTERSECT ").length());
                query = new Pair<>(queryBuilder.toString(), selectionArgs.toArray(new String[0]));
            } else {

                query = myOpenHelper.initialMediaQueryBuilder(whereFilters,
                        orderBy.toArray(new String[0]));
            }

            Bundle dataToPass = new Bundle();

            dataToPass.putString(Constants.MEDIA_QUERY, query.first);
            dataToPass.putStringArray(Constants.QUERY_ARGS, query.second);

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

    private void refreshTags() {
        RelativeLayout tagLayout = findViewById(R.id.activity_main_rl_tags);

        tagLayout.removeAllViewsInLayout();

        int maxWidth = tagLayout.getMeasuredWidth();
        final int[] currentWidth = {0};

        final TextView[] prev = {null};
        final TextView[] above = {null};

        for (String tag : tagFilters) {
            TextView tv = new TextView(this);
            tv.setText(tag);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
            tv.setId(View.generateViewId());
            tv.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
            tv.setPadding(3, 0, 3, 0);
            tv.setSingleLine();
            tv.setOnClickListener(v1 -> {
                tagLayout.removeView(tv);
                tagFilters.remove(tv.getText().toString());
                refreshTags();
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
}