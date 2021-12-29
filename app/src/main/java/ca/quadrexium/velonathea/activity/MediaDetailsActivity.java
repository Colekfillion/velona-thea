package ca.quadrexium.velonathea.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Constants;
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
        Media media = dataToPass.getParcelable(Constants.MEDIA);
        int position = dataToPass.getInt(Constants.POSITION);

        if (media.getLink() == null) {
            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getWritableDatabase();
            media = myOpenHelper.getRemainingData(db, media);
        }

        fileNameView.setText(media.getFileName());
        nameView.setText(media.getName());
        authorView.setText(media.getAuthor());
        linkView.setText(media.getLink());

        Button updateImageDataButton = findViewById(R.id.activity_image_details_btn_update);
        Media finalMedia = media;
        updateImageDataButton.setOnClickListener(v -> {

            Media newMedia = new Media.Builder()
                    .id(finalMedia.getId())
                    .name(nameView.getText().toString())
                    .fileName(fileNameView.getText().toString())
                    .author(authorView.getText().toString())
                    .link(linkView.getText().toString())
                    .build();

            if (!finalMedia.getFileName().equals(newMedia.getFileName())) {
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
                String path = prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES);

                File from = new File(path + "/" + finalMedia.getFileName());
                File to = new File(path + "/" + newMedia.getFileName());
                if (from.exists()) {
                    boolean wasRenamed = from.renameTo(to);
                    if (!wasRenamed) {
                        newMedia.setFileName(finalMedia.getFileName());
                        Toast.makeText(getApplicationContext(), "Could not rename", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getWritableDatabase();
            boolean wasUpdated = myOpenHelper.updateMedia(db, finalMedia, newMedia);
            String toastText = wasUpdated ? "updated" : "no change";
            if (wasUpdated) {
                db.close();
                Intent i = new Intent();
                i.putExtra(Constants.PREFS_UPDATED_MEDIA_POSITION, position);
                i.putExtra(Constants.MEDIA, newMedia);
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