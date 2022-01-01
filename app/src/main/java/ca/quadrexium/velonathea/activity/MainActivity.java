package ca.quadrexium.velonathea.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import java.io.File;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.pojo.Constants;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);

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

            dataToPass.putString(Constants.PREFS_MEDIA_FILENAME, etFileName.getText().toString());
            dataToPass.putString(Constants.PREFS_MEDIA_NAME, etName.getText().toString());
            dataToPass.putString(Constants.PREFS_MEDIA_AUTHOR, etAuthor.getText().toString());
            dataToPass.putString(Constants.PREFS_MEDIA_TAG, etTag.getText().toString());
            String mediaType = "";
            int checkedRadioButtonId = rgMediaType.getCheckedRadioButtonId();
            if (checkedRadioButtonId == R.id.activity_main_rg_mediatype_images) {
                mediaType = Constants.IMAGE;
            } else if (checkedRadioButtonId == R.id.activity_main_rg_mediatype_videos) {
                mediaType = Constants.VIDEO;
            }
            if (!mediaType.equals("")) {
                dataToPass.putString(Constants.PREFS_MEDIA_TYPE, mediaType);
            }

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
                Toast.makeText(this, "Cache could not be deleted", Toast.LENGTH_LONG).show();
            }
        }
    }
}