package ca.quadrexium.velonathea.activity;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Constants;
import ca.quadrexium.velonathea.pojo.Media;

public abstract class BaseActivity extends AppCompatActivity {

    protected boolean verified = false;
    protected final static String NOTIFICATION_CHANNEL_1 = "channel1";
    protected boolean isVisible = false;

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

        //Activity turns blank when leaving
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        //Request read permissions if not granted
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            },1);
        }
    }

    @Override
    protected void onResume() {
        isVisible = true;
        super.onResume();
    }

    @Override
    protected void onPause() {
        isVisible = false;
        super.onPause();
    }

    /**
     * Creates the toolbar given it's id.
     * @param toolbarId the id of the toolbar to set
     */
    protected void createToolbar(int toolbarId) {
        Toolbar tb = findViewById(toolbarId);
        setSupportActionBar(tb);
    }

    /**
     * Abstract method for setting the activity layout via BaseActivity
     * @return the layout to inflate for the activity, used in setContentView
     */
    protected abstract int getLayoutResourceId();

    /**
     * Method called after the user was successfully verified.
     */
    protected abstract void isVerified();

    /**
     * Creates a new MyOpenHelper for the media database.
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
        } else if (item.getItemId() == R.id.index_menubutton) {
            Intent intent = new Intent(this, MediaIndexActivity.class);
            startActivity(intent);
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
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_1, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Reads the contents of a file as a String
     * @param uri the file to read
     * @return the file contents as a string
     */
    protected String readStringFromUri(Uri uri) {
        String text = "";
        try {
            InputStream in = this.getContentResolver().openInputStream(uri);
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }
            text = total.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }

    /**
     * Loads a media list from the query cache, if it exists.
     * @param cacheFileLocation the parent folder of the query cache
     * @return an arraylist of media with id and filename set
     */
    public ArrayList<Media> loadMediaFromCache(String cacheFileLocation) {
        ArrayList<Media> mediaList = new ArrayList<>();
        File queryCache = new File(cacheFileLocation, Constants.QUERY_CACHE_FILENAME);
        String content = readStringFromUri(Uri.fromFile(queryCache));

        if (!content.equals("")) {
            content = content.substring(content.indexOf("\n")+1); //remove query

            String[] rows = content.split("\n");
            for (String row : rows) {
                String[] rowValues = row.split("\t");
                Media media = new Media.Builder()
                        .id(Integer.parseInt(rowValues[0]))
                        .fileName(rowValues[1])
                        .build();
                mediaList.add(media);
            }
        }
        return mediaList;
    }
}