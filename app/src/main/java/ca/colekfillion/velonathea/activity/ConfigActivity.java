package ca.colekfillion.velonathea.activity;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.widget.SwitchCompat;

import java.util.Objects;

import ca.colekfillion.velonathea.R;
import ca.colekfillion.velonathea.pojo.Constants;

public class ConfigActivity extends BaseActivity {

    private Button btnDbConfig;
    private EditText etCacheSize;
    private SwitchCompat showHiddenFiles;
    private SwitchCompat secureWindow;

    @Override
    protected void initViews() {
        btnDbConfig = findViewById(R.id.activity_config_btn_dbconfig);
        etCacheSize = findViewById(R.id.activity_config_et_cachesize);
        showHiddenFiles = findViewById(R.id.activity_config_swtch_showhiddenfiles);
        secureWindow = findViewById(R.id.activity_config_swtch_secure);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true); //set back button in toolbar

        btnDbConfig.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigActivity.this, DatabaseConfigActivity.class);
            startActivity(intent);
        });

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);

        etCacheSize.setText(String.valueOf(prefs.getInt(Constants.PREFS_CACHE_SIZE, 150)));

        showHiddenFiles.setChecked(prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false));
        showHiddenFiles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //Make sure the user is verified to do this
            if (isChecked && !verified) {
                showHiddenFiles.setChecked(false);
                KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
                Intent intent = km.createConfirmDeviceCredentialIntent(getString(R.string.app_name), getString(R.string.permission_message));
                verifyActivity.launch(intent);
            } else if (!isChecked) {
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false);
                edit.apply();
            }
        });

        secureWindow.setChecked(prefs.getBoolean(Constants.PREFS_SECURE_WINDOW, true));
        secureWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //Make sure the user is verified to do this
            if (isChecked && !verified) {
                secureWindow.setChecked(false);
                KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
                Intent intent = km.createConfirmDeviceCredentialIntent(getString(R.string.app_name), getString(R.string.permission_message));
                verifyActivity.launch(intent);
            } else if (!isChecked) {
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(Constants.PREFS_SECURE_WINDOW, false);
                edit.apply();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        EditText etCacheSize = findViewById(R.id.activity_config_et_cachesize);
        edit.putInt(Constants.PREFS_CACHE_SIZE, Integer.parseInt(etCacheSize.getText().toString()));

        edit.apply();
    }

    @Override
    protected void isVerified() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        SwitchCompat swtchShowHiddenFiles = findViewById(R.id.activity_config_swtch_showhiddenfiles);
        edit.putBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, true);
        edit.apply();
        swtchShowHiddenFiles.setChecked(true);
    }

    @Override
    protected String getName() {
        return "ConfigActivity";
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_config;
    }

    //No options menu
    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        return true;
    }


    //Back button in toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}