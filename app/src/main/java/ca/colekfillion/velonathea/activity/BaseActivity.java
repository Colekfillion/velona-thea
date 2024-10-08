package ca.colekfillion.velonathea.activity;

import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import ca.colekfillion.velonathea.R;
import ca.colekfillion.velonathea.database.MyOpenHelper;
import ca.colekfillion.velonathea.fragment.MediaImportFragment;
import ca.colekfillion.velonathea.pojo.Constants;

public abstract class BaseActivity extends AppCompatActivity {

    protected boolean verified = false;
    protected boolean isVisible = false;
    protected String activityName;

    //Verify user activity for things that require authorization
    protected final ActivityResultLauncher<Intent> verifyActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    verified = true;
                    isVerified();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());
        activityName = getName();

        Log.d("LIFECYCLE", "In " + getName() + " onCreate()");

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        //Activity turns blank when leaving for privacy
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        boolean secureWindow = prefs.getBoolean(Constants.PREFS_SECURE_WINDOW, true);
        if (secureWindow) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        //Request read permissions if not granted
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }

        initViews();
    }

    @Override
    protected void onResume() {
        isVisible = true;
        Log.d("LIFECYCLE", "In " + getName() + " onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        isVisible = false;
        Log.d("LIFECYCLE", "In " + getName() + " onPause()");
        super.onPause();
    }

    /**
     * Creates the toolbar given it's id.
     *
     * @param toolbarId the id of the toolbar to set
     */
    protected void createToolbar(int toolbarId) {
        //Toolbars are automatically make in recent Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Toolbar tb = findViewById(toolbarId);
            setSupportActionBar(tb);
        }
    }

    /**
     * Abstract method for setting the activity layout via BaseActivity
     *
     * @return the layout to inflate for the activity, used in setContentView
     */
    protected abstract int getLayoutResourceId();

    /**
     * Method called after the user was successfully verified.
     */
    protected abstract void isVerified();

    /**
     * For debugging, keeping track of what activity is doing what
     */
    protected abstract String getName();

    /**
     * Creates a new MyOpenHelper for the media database.
     *
     * @return a MyOpenHelper for the media database
     */
    public MyOpenHelper getMyOpenHelper() {
        return new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
    }

    /**
     * Called after the permissions message is dismissed. App quits if read permissions are not granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Called when the options menu is opened for the default toolbar
     *
     * @param m menu object to inflate the layout into
     * @return true if the menu should be displayed
     */
    public boolean onCreateOptionsMenu(Menu m) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_toolbar, m);
        return true;
    }

    /**
     * @param item The menu item that was clicked
     * @return true when selection processing is done
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.config_menubutton) {
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.import_menubutton) {
            FragmentManager fm = getSupportFragmentManager();
            MediaImportFragment mediaImportFragment = new MediaImportFragment();
            mediaImportFragment.show(fm, Constants.FRAGMENT_MEDIA_IMPORT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a NotificationChannel for notifications above API 26.
     */
    protected void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = "All notifications";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(Constants.NOFICIATION_CHANNEL_1, name, importance);
            channel.setDescription(description);
            channel.setSound(null, null);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    protected abstract void initViews();
}