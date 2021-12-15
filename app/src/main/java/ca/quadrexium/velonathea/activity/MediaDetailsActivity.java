package ca.quadrexium.velonathea.activity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Media;

public class MediaDetailsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_main_toolbar);

        EditText fileNameView = findViewById(R.id.activity_image_details_et_filename);
        EditText nameView = findViewById(R.id.activity_image_details_et_name);
        EditText authorView = findViewById(R.id.activity_image_details_et_author);
        EditText linkView = findViewById(R.id.activity_image_details_et_link);

        Bundle dataToPass = getIntent().getExtras();
        Media media = dataToPass.getParcelable("media");
        int position = dataToPass.getInt("position");

        if (media.getLink() == null) {
            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getWritableDatabase();
            Cursor c = db.rawQuery("SELECT link FROM image WHERE file_name = ? LIMIT 1;", new String[] { media.getFileName() });
            c.moveToFirst();
            media.setLink(c.getString(c.getColumnIndex(MyOpenHelper.COL_IMAGE_LINK)));
            c.close();
        }

        fileNameView.setText(media.getFileName());
        nameView.setText(media.getName());
        authorView.setText(media.getAuthor());
        linkView.setText(media.getLink());

        Button updateImageDataButton = findViewById(R.id.activity_image_details_btn_update);
        updateImageDataButton.setOnClickListener(v -> {

            Media newMedia = new Media.Builder()
                    .id(media.getId())
                    .name(nameView.getText().toString())
                    .fileName(fileNameView.getText().toString())
                    .author(authorView.getText().toString())
                    .link(linkView.getText().toString())
                    .build();

            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getWritableDatabase();
            boolean wasUpdated = myOpenHelper.updateMedia(db, media, newMedia);
            String toastText = wasUpdated ? "updated" : "no change";
            if (wasUpdated) {
                db.close();
                Intent i = new Intent();
                i.putExtra("position", position);
                i.putExtra("media", newMedia);
                setResult(RESULT_OK, i);
                finish();
            }
            Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_media_details;
    }
}