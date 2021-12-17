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

        SwitchCompat randomOrder = findViewById(R.id.activity_main_swtch_random);
        randomOrder.setChecked(prefs.getBoolean("randomOrder", false));

        EditText etTitle = findViewById(R.id.activity_main_et_title_search);
        EditText etAuthor = findViewById(R.id.activity_main_et_author_search);
        EditText etTag = findViewById(R.id.activity_main_et_tag_search);

        Button btnSearch = findViewById(R.id.activity_main_btn_search);
        btnSearch.setOnClickListener(v -> {
            Bundle dataToPass = new Bundle();
            Intent i = new Intent(this, SearchResultsActivity.class);
            dataToPass.putString("title", etTitle.getText().toString());
            dataToPass.putString("author", etAuthor.getText().toString());
            dataToPass.putString("tag", etTag.getText().toString());
            i.putExtras(dataToPass);
            startActivity(i);
        });
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
}