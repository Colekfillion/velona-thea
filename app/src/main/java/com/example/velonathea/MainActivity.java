package com.example.velonathea;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Request read permissions if not granted
        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            },1);
        }

        Toolbar tb = findViewById(R.id.main_toolbar);
        setSupportActionBar(tb);

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);

        EditText searchBar = findViewById(R.id.activity_main_searchbar);
        SwitchCompat randomOrder = findViewById(R.id.activity_main_random_order_switch);
        randomOrder.setChecked(prefs.getBoolean("randomOrder", false));

        Button tagButton = findViewById(R.id.activity_main_searchtagbutton);
        tagButton.setOnClickListener(v -> searchIntent("tag", searchBar.getText().toString()));
        Button authorButton = findViewById(R.id.activity_main_searchauthorbutton);
        authorButton.setOnClickListener(v -> searchIntent("author", searchBar.getText().toString()));
        Button titleButton = findViewById(R.id.activity_main_searchtitlebutton);
        titleButton.setOnClickListener(v -> searchIntent("title", searchBar.getText().toString()));
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        SwitchCompat randomOrder = findViewById(R.id.activity_main_random_order_switch);
        edit.putBoolean("randomOrder", randomOrder.isChecked());
        edit.apply();
    }

    //Given search mode and text, go to SearchResultsActivity
    private void searchIntent(String searchMode, String searchFor) {
        Bundle dataToPass = new Bundle();
        Intent i = new Intent(this, SearchResultsActivity.class);
        dataToPass.putString("searchMode", searchMode);
        dataToPass.putString("searchFor", "%" + searchFor.toLowerCase() + "%");
        i.putExtras(dataToPass);
        startActivity(i);
    }

    public boolean onCreateOptionsMenu(Menu m) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_toolbar, m);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.config_menubutton) {
            Intent i = new Intent(this, ConfigActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0]!=PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}