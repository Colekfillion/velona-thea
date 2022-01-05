package ca.quadrexium.velonathea.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Pair;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Constants;

public class MainActivity extends BaseActivity {

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
        EditText etAuthor = findViewById(R.id.activity_main_et_author);
        EditText etTag = findViewById(R.id.activity_main_et_tag);
        RadioGroup rgMediaType = findViewById(R.id.activity_main_rg_mediatype);

        Button btnSearch = findViewById(R.id.activity_main_btn_search);
        btnSearch.setOnClickListener(v -> {
            Bundle dataToPass = new Bundle();

            String fileName = etFileName.getText().toString();
            String name = etName.getText().toString();
            String author = etAuthor.getText().toString();
            String tag = etTag.getText().toString();
            String mediaType = "";
            int checkedRadioButtonId = rgMediaType.getCheckedRadioButtonId();
            if (checkedRadioButtonId == R.id.activity_main_rg_mediatype_images) {
                mediaType = Constants.IMAGE;
            } else if (checkedRadioButtonId == R.id.activity_main_rg_mediatype_videos) {
                mediaType = Constants.VIDEO;
            }

            Set<String> selectedColumns = new LinkedHashSet<>();
            ArrayList<String> orderBy = new ArrayList<>();
            if (swtchRandom.isChecked()) {
                orderBy.add("RANDOM()");
            }

            Map<String, String[]> whereFilters = new HashMap<>();
            if (!fileName.equals("")) {
                whereFilters.put(MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME, new String[] { fileName });
            }
            if (!name.equals("")) {
                whereFilters.put(MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_NAME, new String[] { name });
            }
            if (!author.equals("")) {
                whereFilters.put(MyOpenHelper.AUTHOR_TABLE + "." + MyOpenHelper.COL_AUTHOR_NAME, new String[] { author });
            }
            if (!tag.equals("")) {
                whereFilters.put(MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_NAME, new String[] { tag });
            }
            if (!mediaType.equals("")) {
                if (mediaType.equals(Constants.IMAGE)) {
                    whereFilters.put(MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME, Constants.IMAGE_EXTENSIONS.toArray(new String[0]));
                } else if (mediaType.equals(Constants.VIDEO)) {
                    Set<String> videoExtensions = new HashSet<>(Constants.VIDEO_EXTENSIONS);
                    videoExtensions.add(".gif"); //gifs are considered videos except for viewing
                    whereFilters.put(MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILENAME, videoExtensions.toArray(new String[0]));
                }
            }
            if (!name.equals("") || !fileName.equals("")) {
                String naturalSortColumn = MyOpenHelper.MEDIA_TABLE + ".";
                naturalSortColumn += fileName.length() >= name.length() ? MyOpenHelper.COL_MEDIA_FILENAME : MyOpenHelper.COL_MEDIA_NAME;
                orderBy.add("LENGTH(" + naturalSortColumn + ")");
            }

            MyOpenHelper myOpenHelper = getMyOpenHelper();
            Pair<String, String[]> query = myOpenHelper.mediaQueryBuilder(selectedColumns,
                    whereFilters, orderBy.toArray(new String[0]), 0);

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
}