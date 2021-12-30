package ca.quadrexium.velonathea.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Button;
import android.widget.EditText;

import java.util.Objects;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.pojo.Constants;

public class ConfigActivity extends BaseActivity {

    private boolean verified = false;

    private final ActivityResultLauncher<Intent> chooseDirActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putString(Constants.PATH, data.getStringExtra(Constants.PATH));
                        edit.apply();
                    }
                }
            });

    protected final ActivityResultLauncher<Intent> verifyActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    verified = true;
                    SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = prefs.edit();

                    SwitchCompat showHiddenFiles = findViewById(R.id.activity_config_swtch_hiddenfiles);
                    edit.putBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, showHiddenFiles.isChecked());
                    edit.apply();
                    showHiddenFiles.setChecked(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Button dbConfigButton = findViewById(R.id.activity_config_btn_dbconfig);
        dbConfigButton.setOnClickListener(v -> {
            Intent i = new Intent(ConfigActivity.this, DatabaseConfigActivity.class);
            startActivity(i);
        });

        //Launches the chooseDirActivity
        Button chooseDirButton = findViewById(R.id.activity_config_choosedirbutton);
        chooseDirButton.setOnClickListener(v -> {
            Intent i = new Intent(this, ChooseDirActivity.class);
            chooseDirActivity.launch(i);
        });

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);

        EditText imageCacheSize = findViewById(R.id.activity_config_maxcachesize);
        imageCacheSize.setText(String.valueOf(prefs.getInt(Constants.PREFS_CACHE_SIZE, 20)));

        SwitchCompat invalidFiles = findViewById(R.id.activity_config_swtch_invalidfiles);
        invalidFiles.setChecked(prefs.getBoolean(Constants.PREFS_SHOW_INVALID_FILES, true));
        invalidFiles.setOnLongClickListener(v -> {
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

        SwitchCompat showHiddenFiles = findViewById(R.id.activity_config_swtch_hiddenfiles);
        showHiddenFiles.setChecked(prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false));
        showHiddenFiles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !verified) {
                showHiddenFiles.setChecked(false);
                KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
                Intent intent = km.createConfirmDeviceCredentialIntent("Velona Thea", "This app requires you to authenticate.");
                verifyActivity.launch(intent);
            }
        });
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_config; }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        EditText imageCacheSize = findViewById(R.id.activity_config_maxcachesize);
        edit.putInt(Constants.PREFS_CACHE_SIZE, Integer.parseInt(imageCacheSize.getText().toString()));

        SwitchCompat showHiddenFiles = findViewById(R.id.activity_config_swtch_hiddenfiles);
        edit.putBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, showHiddenFiles.isChecked());

        SwitchCompat invalidFiles = findViewById(R.id.activity_config_swtch_invalidfiles);
        edit.putBoolean(Constants.PREFS_SHOW_INVALID_FILES, invalidFiles.isChecked());
        edit.apply();
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