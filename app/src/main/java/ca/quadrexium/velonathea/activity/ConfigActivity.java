package ca.quadrexium.velonathea.activity;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import java.util.Objects;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.fragment.ChooseDirFragment;
import ca.quadrexium.velonathea.pojo.Constants;

public class ConfigActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true); //set back button in toolbar

        //Launches DatabaseConfigActivity
        Button btnDbConfig = findViewById(R.id.activity_config_btn_dbconfig);
        btnDbConfig.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigActivity.this, DatabaseConfigActivity.class);
            startActivity(intent);
        });

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);

        TextView tvRootDir = findViewById(R.id.activity_config_tv_rootdir);
        tvRootDir.setText(prefs.getString(Constants.PATH, Environment.getExternalStorageDirectory().getAbsolutePath()));

        //Launches ChooseDirActivity
        Button btnChooseDir = findViewById(R.id.activity_config_btn_choosedir);
        btnChooseDir.setOnClickListener(v -> {
            FragmentManager fm = getSupportFragmentManager();
            ChooseDirFragment chooseDirFragment = new ChooseDirFragment();
            chooseDirFragment.show(fm, "chooseDirFragment");
        });

        EditText etCacheSize = findViewById(R.id.activity_config_et_cachesize);
        etCacheSize.setText(String.valueOf(prefs.getInt(Constants.PREFS_CACHE_SIZE, 150)));

        SwitchCompat swtchShowInvalidFiles = findViewById(R.id.activity_config_swtch_showinvalidfiles);
        swtchShowInvalidFiles.setChecked(prefs.getBoolean(Constants.PREFS_SHOW_INVALID_FILES, true));
        swtchShowInvalidFiles.setOnLongClickListener(v -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            alertDialogBuilder.setTitle(R.string.show_invalid_files)

                    .setMessage("An extra check is performed at the end of each search, which " +
                            "checks that the file exists and is not empty (0 bytes) \n" +
                            "Not recommended for large datasets, as it significantly slows down " +
                            "searches. ")

                    .setNeutralButton(android.R.string.ok, (click, arg) -> { })

                    .create().show();
            return true;
        });

        SwitchCompat showHiddenFiles = findViewById(R.id.activity_config_swtch_showhiddenfiles);
        showHiddenFiles.setChecked(prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false));
        showHiddenFiles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //Make sure the user is verified to do this
            if (isChecked && !verified) {
                showHiddenFiles.setChecked(false);
                KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
                Intent intent = km.createConfirmDeviceCredentialIntent("Velona Thea", "This app requires you to authenticate.");
                verifyActivity.launch(intent);
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

        SwitchCompat swtchShowHiddenFiles = findViewById(R.id.activity_config_swtch_showhiddenfiles);
        edit.putBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, swtchShowHiddenFiles.isChecked());

        SwitchCompat swtchShowInvalidFiles = findViewById(R.id.activity_config_swtch_showinvalidfiles);
        edit.putBoolean(Constants.PREFS_SHOW_INVALID_FILES, swtchShowInvalidFiles.isChecked());
        edit.apply();
    }

    @Override
    protected void isVerified() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        SwitchCompat swtchShowHiddenFiles = findViewById(R.id.activity_config_swtch_showhiddenfiles);
        edit.putBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, swtchShowHiddenFiles.isChecked());
        edit.apply();
        swtchShowHiddenFiles.setChecked(true);
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_config; }

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

    /**
     * Called by ChooseDirFragment to change the root path.
     * @param path the new root path
     */
    public void changePath(String path) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Constants.PATH, path);
        edit.apply();

        TextView tvRootDir = findViewById(R.id.activity_config_tv_rootdir);
        tvRootDir.setText(path);
    }
}