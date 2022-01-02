package ca.quadrexium.velonathea.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;

public abstract class BaseActivity extends AppCompatActivity {

    protected boolean verified = false;

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
    public MyOpenHelper openMediaDatabase() {
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
        }
        return super.onOptionsItemSelected(item);
    }
}