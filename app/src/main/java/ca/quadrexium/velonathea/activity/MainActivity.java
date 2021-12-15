package ca.quadrexium.velonathea.activity;

import androidx.appcompat.widget.SwitchCompat;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import ca.quadrexium.velonathea.R;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_main_toolbar);

        SharedPreferences prefs = getSharedPreferences("preferences", MODE_PRIVATE);

        EditText searchBar = findViewById(R.id.activity_main_et_search);
        SwitchCompat randomOrder = findViewById(R.id.activity_main_swtch_random);
        randomOrder.setChecked(prefs.getBoolean("randomOrder", false));

        Button tagButton = findViewById(R.id.activity_main_btn_tagsearch);
        tagButton.setOnClickListener(v -> searchIntent("tag", searchBar.getText().toString()));
        Button authorButton = findViewById(R.id.activity_main_btn_authorsearch);
        authorButton.setOnClickListener(v -> searchIntent("author", searchBar.getText().toString()));
        Button titleButton = findViewById(R.id.activity_main_btn_namesearch);
        titleButton.setOnClickListener(v -> searchIntent("title", searchBar.getText().toString()));
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_main; }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences prefs = getSharedPreferences("preferences", MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        SwitchCompat randomOrder = findViewById(R.id.activity_main_swtch_random);
        edit.putBoolean("randomOrder", randomOrder.isChecked());
        edit.apply();
    }

    //Given search mode and text, go to SearchResultsActivity
    private void searchIntent(String searchMode, String searchFor) {
        Bundle dataToPass = new Bundle();
        Intent i = new Intent(this, SearchResultsActivity.class);
        dataToPass.putString("searchMode", searchMode);
        dataToPass.putString("searchFor", searchFor.toLowerCase());
        i.putExtras(dataToPass);
        startActivity(i);
    }
}