package ca.quadrexium.velonathea.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.FragmentManager;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.HashMap;
import java.util.Map;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.fragment.ChooseDirFragment;
import ca.quadrexium.velonathea.pojo.Constants;

public class MediaIndexActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);

        EditText etApiKey = findViewById(R.id.activity_media_index_et_apikey);
        etApiKey.setText(prefs.getString(Constants.API_KEY, ""));

        EditText etInputPath = findViewById(R.id.activity_media_index_et_inputpath);
        etInputPath.setText(prefs.getString(Constants.INPUT_PATH, Environment.getExternalStorageDirectory().getAbsolutePath()));

        EditText etMinSim = findViewById(R.id.activity_media_index_et_minsim);
        etMinSim.setText(prefs.getString(Constants.MINSIM, "80"));

        Button btnChooseDir = findViewById(R.id.activity_media_index_btn_choosedir);
        btnChooseDir.setOnClickListener(v -> {
            FragmentManager fm = getSupportFragmentManager();
            ChooseDirFragment chooseDirFragment = new ChooseDirFragment();
            chooseDirFragment.show(fm, Constants.FRAGMENT_CHOOSE_DIR);
        });

        Button btnExecute = findViewById(R.id.activity_media_index_btn_execute);
        btnExecute.setOnClickListener(v -> {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(getApplicationContext()));
            }

            Python py = Python.getInstance();

            PyObject main = py.getModule("main");

            Map<String, String> dict = new HashMap<>();
            dict.put("api_key", etApiKey.getText().toString());
            dict.put("images_directory", etInputPath.getText().toString());
            dict.put("minimum_similarity", etMinSim.getText().toString());

            main.callAttr("execute_button", dict);
        });
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_media_index; }

    @Override
    protected void isVerified() { }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        EditText etInputPath = findViewById(R.id.activity_media_index_et_inputpath);
        etInputPath.setText(prefs.getString(Constants.PATH, Environment.getExternalStorageDirectory().getAbsolutePath()));
    }
}