package ca.quadrexium.velonathea.fragment;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Constants;
import ca.quadrexium.velonathea.pojo.Media;
import ca.quadrexium.velonathea.pojo.Notification;

public class MediaImportFragment extends BaseDialogFragment {
    private static boolean busy = false;
    private final int LOAD_FILE_NOTIFICATION_ID = 12;
    private final int LOAD_MEDIA_ROOT_NOTIFICATION_ID = 64;
    private boolean isVisible = true;

    private Notification notification;
    private View root;
    private String loadingCompleteText;

    private final ActivityResultLauncher<Intent> loadRowsActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        busy = true;
                        Uri uri = data.getData();

                        ProgressBar pb = root.findViewById(R.id.fragment_media_import_pb_loading);
                        TextView tvLoading = root.findViewById(R.id.fragment_media_import_tv_loading);

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Handler handler = new Handler(Looper.getMainLooper());

                        executor.execute(() -> {
                            //Read the file as a string
                            handler.post(() -> {
                                pb.setVisibility(View.VISIBLE);
                                tvLoading.setVisibility(View.VISIBLE);
                                notification = new Notification.Builder(getContext(), Constants.NOFICIATION_CHANNEL_1,
                                        LOAD_FILE_NOTIFICATION_ID)
                                        .title(getString(R.string.loading_media))
                                        .content(getString(R.string.preparing))
                                        .priority(android.app.Notification.PRIORITY_LOW)
                                        .smallIcon(R.drawable.null_image).build();
                            });

                            String text = readStringFromUri(uri);
                            if (!text.equals("")) {
                                //Prepare for inserting rows into db
                                MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);;
                                SQLiteDatabase db = myOpenHelper.getWritableDatabase();

                                Set<String> allMediaPaths = myOpenHelper.getMediaPaths(db);

                                db.beginTransaction();
                                //Dropping indexes for performance
                                for (String query : MyOpenHelper.DROP_INDEX_QUERIES) {
                                    db.execSQL(query);
                                }

                                String[] rows = text.split("\n");

                                int numRows = rows.length;
                                final int[] delayControl = {0};
                                for (int i = 0; i < rows.length; i++) {
                                    String[] rowValues = rows[i].split("\t");
                                    if (!allMediaPaths.remove(rowValues[0])) {
                                        Media.Builder mediaBuilder = new Media.Builder()
                                                .id(-1) //temp value
                                                .filePath(rowValues[0])
                                                .name(rowValues[1])
                                                .author(Constants.isStringEmpty(rowValues[2]) ? "unknown" : rowValues[2]);
                                        try {
                                            mediaBuilder.link(rowValues[3]);
                                        } catch (ArrayIndexOutOfBoundsException e) {
                                            //no link to source, do nothing
                                        }
                                        try {
                                            mediaBuilder.tags(new HashSet<>(Arrays.asList(rowValues[4].split(" "))));
                                        } catch (ArrayIndexOutOfBoundsException e) {
                                            //no tags, do nothing
                                        }

                                        Media media = mediaBuilder.build();

                                        int mediaId = myOpenHelper.insertMedia(db, media);
                                        if ((media.getTags() != null &&
                                                (media.getTags().size() > 0)) ||
                                                (!media.getAuthor().equals("unknown")) ||
                                                (!Constants.isStringEmpty(media.getLink())) ||
                                                (!media.getFileName().contains(media.getName()))) {
                                            ContentValues cv = new ContentValues();
                                            cv.put(MyOpenHelper.COL_MEDIA_INDEXED, 1);
                                            db.update(MyOpenHelper.MEDIA_TABLE, cv,
                                                    MyOpenHelper.COL_MEDIA_ID + " = ?", new String[]{String.valueOf(mediaId)});
                                        }
                                    }
                                    int finalI = i;
                                    handler.post(() -> {
                                        if (delayControl[0] == 5) {
                                            int percentProgress = (int) (((double) finalI / (double) numRows) * 100);
                                            String content = finalI + "/" + numRows +
                                                    " (" + percentProgress + "%)";
                                            if (!isVisible) {
                                                notification.setContent(content);
                                                notification.setProgress(percentProgress * 10);
                                                notification.show();
                                            } else {
                                                pb.setProgress(percentProgress * 10);
                                                tvLoading.setText(String.format("%s, %s", getString(R.string.loading_media), content));
                                            }
                                            delayControl[0] = 0;
                                        } else {
                                            delayControl[0]++;
                                        }
                                    });
                                }
                                //Recreating the indexes
                                for (String query : MyOpenHelper.CREATE_INDEX_QUERIES) {
                                    db.execSQL(query);
                                }
                                db.setTransactionSuccessful();
                                db.endTransaction();
                                busy = false;
                                handler.post(() -> {
                                    if (!isVisible) {
                                        notification.setTitle(loadingCompleteText);
                                        notification.setContent(numRows + "/" + numRows);
                                        notification.setProgress(0);
                                        notification.show();
                                    }
                                    pb.setProgress(0);
                                    pb.setVisibility(View.GONE);
                                    String output = loadingCompleteText + ", " +
                                            numRows + "/" + numRows;
                                    tvLoading.setText(output);
                                });
                            }
                        });
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_media_import;
    }

    @Override
    protected String getName() {
        return "MediaImportFragment";
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadingCompleteText = getString(R.string.loading_complete);

        RelativeLayout rlImportRaw = view.findViewById(R.id.fragment_media_import_rl_raw);
        RelativeLayout rlImportFile = view.findViewById(R.id.fragment_media_import_rl_file);

        RadioGroup rgImportType = view.findViewById(R.id.fragment_media_import_rg_importtype);
        rgImportType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.fragment_media_import_rg_importtype_raw) {
                rlImportFile.setVisibility(View.GONE);
                rlImportRaw.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.fragment_media_import_rg_importtype_file) {
                rlImportRaw.setVisibility(View.INVISIBLE);
                rlImportFile.setVisibility(View.VISIBLE);
            }
        });

        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        String path = prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES);

        EditText etInputPath = view.findViewById(R.id.fragment_media_import_et_inputpath);
        etInputPath.setText(path);

        Button btnChooseDir = view.findViewById(R.id.fragment_media_import_btn_choosedir);
        Button btnChooseFile = view.findViewById(R.id.fragment_media_import_btn_choosefile);
        Button btnImport = view.findViewById(R.id.fragment_media_import_btn_import);

        btnChooseDir.setOnClickListener(v -> {
            FragmentManager fm = getActivity().getSupportFragmentManager();
            ChooseDirFragment chooseDirFragment = new ChooseDirFragment();
            chooseDirFragment.show(fm, Constants.FRAGMENT_CHOOSE_DIR);
        });

        btnChooseFile.setOnClickListener(v -> {
            if (!busy) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/plain");
                loadRowsActivity.launch(intent);
            }
        });

        btnImport.setOnClickListener(v -> {
            if (!busy) {
                busy = true;

                ProgressBar pb = view.findViewById(R.id.fragment_media_import_pb_loading);
                TextView tvLoading = view.findViewById(R.id.fragment_media_import_tv_loading);
                tvLoading.setVisibility(View.VISIBLE);
                pb.setVisibility(View.VISIBLE);

                notification = new Notification.Builder(getContext(), Constants.NOFICIATION_CHANNEL_1,
                        LOAD_MEDIA_ROOT_NOTIFICATION_ID)
                        .title(getString(R.string.loading_media))
                        .content(getString(R.string.preparing))
                        .priority(android.app.Notification.PRIORITY_LOW)
                        .smallIcon(R.drawable.null_image).build();

                tvLoading.setText(String.format("%s, %s", getString(R.string.loading_media), getString(R.string.preparing)));
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    //Get all valid media files in root directory
                    File root = new File(path);
                    String[] filesInRoot = root.list((dir, name) -> {
                        int startingIndex = name.lastIndexOf(".");
                        if (startingIndex == -1) { return false; }
                        String extension = name.substring(startingIndex);
                        return Constants.IMAGE_EXTENSIONS.contains(extension) ||
                                Constants.VIDEO_EXTENSIONS.contains(extension) ||
                                extension.equals(".gif");
                    });
                    if (filesInRoot == null) {
                        return; //no files
                    }
                    for (int i = 0; i < filesInRoot.length; i++) {
                        filesInRoot[i] = path + "/" + filesInRoot[i];
                    }
                    Set<String> filePaths = new HashSet<>(Arrays.asList(filesInRoot));
                    int filePathsLength = filePaths.size();

                    //Filter filenames that are already in the database
                    MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                    SQLiteDatabase db = myOpenHelper.getWritableDatabase();
                    Cursor c = db.rawQuery("SELECT " + MyOpenHelper.MEDIA_TABLE + "." +
                            MyOpenHelper.COL_MEDIA_PATH + " FROM " + MyOpenHelper.MEDIA_TABLE, null);
                    c.moveToFirst();

                    while (!c.isAfterLast()) {
                        String filePath = c.getString(0);
                        boolean wasRemoved = filePaths.remove(filePath);
                        if (wasRemoved) {
                            filePathsLength--;
                        }
                        c.moveToNext();
                    }
                    c.close();

                    int count = 0;
                    //Insert media
                    db.beginTransaction();
                    for (String filePath : filePaths) {
                        int extensionIndex = filePath.lastIndexOf(".");
                        String fileName = filePath.substring(filePath.lastIndexOf("/", extensionIndex-1)+1);
                        String name = fileName.substring(0, fileName.lastIndexOf("."));
                        Media media = new Media.Builder()
                                .id(-1)
                                .filePath(filePath)
                                .name(name)
                                .author("unknown")
                                .build();
                        //To diagnose a one-time unreproducible bug where the name was not the
                        // filename without the extension
                        if (!fileName.contains(media.getName())) {
                            handler.post(() -> {
                                Toast.makeText(getContext(), "Assertion error: " +
                                        fileName.substring(0, fileName.lastIndexOf("."))
                                        + " != " + name, Toast.LENGTH_LONG).show();
                                dismiss();
                            });
                            return;
                        }
                        int mediaId = myOpenHelper.insertMedia(db, media);
                        if (mediaId == -1) { //insertion error
                            handler.post(() -> {
                                Toast.makeText(getContext(), "Error inserting " +
                                                "media into database",
                                        Toast.LENGTH_LONG).show();
                                dismiss();
                            });
                            return;
                        }
                        count++;
                        int finalCount = count;
                        double percent = ((double) finalCount / (double) filePathsLength) * 1000;
                        String notificationContent = count + "/" + filePathsLength;
                        String tvLoadingText = getString(R.string.loading_media) + ", " + count + "/" + filePathsLength;
                        if (count % 4 == 0) { //give the ui thread some time to process
                            handler.post(() -> {
                                tvLoading.setText(tvLoadingText);
                                pb.setProgress((int) percent);
                                if (!isVisible) {
                                    notification.setContent(notificationContent);
                                    notification.setProgress((int) percent);
                                    notification.show();
                                }
                            });
                        }
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    db.close();
                    busy = false;
                    String content = count + "/" + count;
                    String tvLoadingText = getString(R.string.loading_complete) + ", " + content;
                    handler.post(() -> {
                        tvLoading.setText(tvLoadingText);
                        pb.setVisibility(View.GONE);
                        pb.setProgress(0);
                        if (!isVisible) {
                            notification.setTitle(tvLoadingText);
                            notification.setContent(content);
                            notification.setProgress(0);
                            notification.show();
                        }
                    });
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        isVisible = true;
        if (notification != null) {
            notification.dismiss();
        }
        Log.d("LIFECYCLE", "In MediaImportFragment onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        isVisible = false;
        Log.d("LIFECYCLE", "In MediaImportFragment onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("LIFECYCLE", "In MediaImportFragment onStop()");
    }

    /**
     * Reads the contents of a file as a String
     * @param uri the file to read
     * @return the file contents as a string
     */
    protected String readStringFromUri(Uri uri) {
        String text = "";
        try {
            InputStream in = getActivity().getContentResolver().openInputStream(uri);
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
}