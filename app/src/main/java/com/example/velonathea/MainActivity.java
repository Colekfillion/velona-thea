package com.example.velonathea;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar tb = findViewById(R.id.main_toolbar);
        setSupportActionBar(tb);

        EditText searchBar = findViewById(R.id.activity_main_searchbar);

        Button tagButton = findViewById(R.id.activity_main_searchtagbutton);
        tagButton.setOnClickListener(v -> searchIntent("tag", searchBar.getText().toString()));
        Button authorButton = findViewById(R.id.activity_main_searchauthorbutton);
        authorButton.setOnClickListener(v -> searchIntent("author", searchBar.getText().toString()));
        Button titleButton = findViewById(R.id.activity_main_searchtitlebutton);
        titleButton.setOnClickListener(v -> searchIntent("title", searchBar.getText().toString()));
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
}