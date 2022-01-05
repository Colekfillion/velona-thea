package ca.quadrexium.velonathea.fragment;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.activity.SearchResultsActivity;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Media;

public class MediaDetailsFragment extends DialogFragment {

    private Media media;
    private final int position;

    /**
     * @param position the position of the media in the parent activity's mediaList
     * @param media the media shown
     */
    public MediaDetailsFragment(int position, Media media) {
        this.position = position;
        this.media = media;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText etFileName = view.findViewById(R.id.fragment_media_details_et_filename);
        EditText etName = view.findViewById(R.id.fragment_media_details_et_name);
        EditText etAuthor = view.findViewById(R.id.fragment_media_details_et_author);
        EditText etLink = view.findViewById(R.id.fragment_media_details_et_link);
        EditText etTags = view.findViewById(R.id.fragment_media_details_et_tags);
        Button btnUpdate = view.findViewById(R.id.fragment_media_details_btn_update);

        if (media.getLink() == null || (media.getTags() == null || media.getTags().size() == 0)) {
            MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
            SQLiteDatabase db = myOpenHelper.getReadableDatabase();
            try {
                media = myOpenHelper.getRemainingData(db, media);
                db.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        etFileName.setText(media.getFileName());
        etName.setText(media.getName());
        etAuthor.setText(media.getAuthor());
        etLink.setText(media.getLink());
        etTags.setText(media.getTagsAsString());

        //Update button, update media in database and send updated media to SearchResultsActivity
        btnUpdate.setOnClickListener(v -> {

            boolean changed = false;
            String newMediaName = etName.getText().toString();
            if (!newMediaName.equals("") && !media.getName().equals(newMediaName)) {
                changed = true;
                media.setName(newMediaName);
            }
            String newMediaFileName = etFileName.getText().toString();
            if (!newMediaFileName.equals("") && !media.getFileName().equals(newMediaFileName)) {
                changed = true;
                media.setFileName(newMediaFileName);
            }
            String newMediaAuthor = etAuthor.getText().toString();
            if (!newMediaAuthor.equals("") && !media.getAuthor().equals(newMediaAuthor)) {
                changed = true;
                media.setAuthor(newMediaAuthor);
            }
            String newMediaLink = etLink.getText().toString();
            if (!newMediaLink.equals("") && !media.getLink().equals(newMediaLink)) {
                changed = true;
                media.setLink(newMediaLink);
            }
            String newMediaTags = etTags.getText().toString();

            if (!newMediaTags.equals("")) {
                Set<String> newMediaTagsSet = new HashSet<>(Arrays.asList(newMediaTags.split(" ")));
                if (media.getTags() == null || (media.getTags() != null && !media.getTags().equals(newMediaTagsSet))) {
                    changed = true;
                    media.setTags(newMediaTagsSet);
                }
            }

            String toastText = "no change";
            if (changed) {
                MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                boolean wasUpdated = myOpenHelper.updateMedia(db, media);
                toastText = wasUpdated ? "updated" : "no change";
                if (wasUpdated) {
                    SearchResultsActivity parentActivity = ((SearchResultsActivity)getContext());
                    if (parentActivity != null) {
                        parentActivity.mediaChanged(position, media);
                    }
                } else {
                    toastText = "error updating media";
                }
                db.close();
            }
            Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }
}