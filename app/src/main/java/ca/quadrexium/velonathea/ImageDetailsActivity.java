package ca.quadrexium.velonathea;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import ca.quadrexium.velonathea.pojo.Media;

public class ImageDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        EditText fileNameView = findViewById(R.id.activity_image_details_et_filename);
        EditText nameView = findViewById(R.id.activity_image_details_et_name);
        EditText authorView = findViewById(R.id.activity_image_details_et_author);
        EditText linkView = findViewById(R.id.activity_image_details_et_link);

        Bundle dataToPass = getIntent().getExtras();
        Media media = dataToPass.getParcelable("media");
        int position = dataToPass.getInt("position");
        System.out.println("position: " + position);

        System.out.println("-----------" + media.getLink());
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

            String newFileName = fileNameView.getText().toString();
            String newName = nameView.getText().toString();
            String newAuthor = authorView.getText().toString();
            String newLink = linkView.getText().toString();

            //Making sure data has changed to reduce unnecessary updates
            ContentValues cv = new ContentValues();
            boolean hasChanged = false;
            if (!media.getFileName().equals(newFileName)) {
                //TODO: Change file name in storage as well
                cv.put(MyOpenHelper.COL_IMAGE_FILENAME, newFileName);
                media.setFileName(newFileName);
                hasChanged = true;
            }
            if (!media.getName().equals(newName)) {
                cv.put(MyOpenHelper.COL_IMAGE_NAME, newName);
                media.setName(newName);
                hasChanged = true;
            }
            if (!media.getAuthor().equals(newAuthor)) {
                cv.put(MyOpenHelper.COL_IMAGE_AUTHOR, newAuthor);
                media.setAuthor(newAuthor);
                hasChanged = true;
            }
            if (!media.getLink().equals(newLink)) {
                cv.put(MyOpenHelper.COL_IMAGE_LINK, newLink);
                media.setLink(newLink);
                hasChanged = true;
            }

            if (hasChanged) {
                MyOpenHelper myOpenHelper = new MyOpenHelper(this, MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                db.update(MyOpenHelper.IMAGE_TABLE, cv, "id = (SELECT id FROM image WHERE file_name = ?)", new String[]{ media.getFileName() });
                Toast.makeText(getApplicationContext(), "updated", Toast.LENGTH_LONG).show();
                db.close();
                Intent i = new Intent();
                i.putExtra("position", position);
                System.out.println("position: " + position);
                i.putExtra("media", media);
                setResult(RESULT_OK, i);
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "no change", Toast.LENGTH_LONG).show();
            }
        });
    }
}