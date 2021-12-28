package ca.quadrexium.velonathea.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SwitchCompat;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Constants;
import ca.quadrexium.velonathea.pojo.Media;

public class ConfigActivity extends BaseActivity {

    private boolean verified = false;

    private final ActivityResultLauncher<Intent> chooseDirActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putString("path", data.getStringExtra("path"));
                        edit.apply();
                    }
                }
            });

    protected final ActivityResultLauncher<Intent> verifyActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    verified = true;
                    SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = prefs.edit();

                    SwitchCompat showHiddenFiles = findViewById(R.id.show_hidden_files_switch);
                    edit.putBoolean("showHiddenFiles", showHiddenFiles.isChecked());
                    edit.apply();
                    showHiddenFiles.setChecked(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);

        EditText imageCacheSize = findViewById(R.id.activity_config_maxcachesize);
        imageCacheSize.setText(String.valueOf(prefs.getInt("maxCacheSize", 20)));

        SwitchCompat showHiddenFiles = findViewById(R.id.show_hidden_files_switch);
        showHiddenFiles.setChecked(prefs.getBoolean("showHiddenFiles", false));
        showHiddenFiles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !verified) {
                showHiddenFiles.setChecked(false);
                KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
                Intent i = km.createConfirmDeviceCredentialIntent("Velona Thea", "This app requires you to authenticate.");
                verifyActivity.launch(i);
            }
        });
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_config; }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        EditText imageCacheSize = findViewById(R.id.activity_config_maxcachesize);
        edit.putInt("maxCacheSize", Integer.parseInt(imageCacheSize.getText().toString()));

        SwitchCompat showHiddenFiles = findViewById(R.id.show_hidden_files_switch);
        edit.putBoolean("showHiddenFiles", showHiddenFiles.isChecked());
        edit.apply();
    }
}