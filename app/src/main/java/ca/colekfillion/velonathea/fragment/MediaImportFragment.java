package ca.colekfillion.velonathea.fragment;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.colekfillion.velonathea.R;
import ca.colekfillion.velonathea.activity.CacheDependentActivity;
import ca.colekfillion.velonathea.database.MyOpenHelper;
import ca.colekfillion.velonathea.pojo.Constants;
import ca.colekfillion.velonathea.pojo.Media;
import ca.colekfillion.velonathea.pojo.Notification;

public class MediaImportFragment extends BaseDialogFragment {
    private static boolean busy = false;
    private final int LOAD_FILE_NOTIFICATION_ID = 12;
    private final int LOAD_MEDIA_ROOT_NOTIFICATION_ID = 64;
    private boolean isVisible = true;

    private Notification notification;

    private RelativeLayout rlImportRaw;
    private RelativeLayout rlImportFile;

    private RadioGroup rgImportType;
    private EditText etInputPath;
    private final ActivityResultLauncher<Intent> chooseDirActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri uri = data.getData();
                        if (getActivity() != null) {
                            SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
                            SharedPreferences.Editor edit = prefs.edit();
                            String newPath = uri.getPath();
                            assert newPath != null; //we never display media from the web
                            File f = new File(newPath);
                            edit.putString(Constants.PATH, f.getAbsolutePath());
                            edit.apply();
                            etInputPath.setText(f.getAbsolutePath());
                        }
                    }
                }
            });
    private Button btnChooseDir;
    private Button btnChooseFile;
    private Button btnImport;
    private ProgressBar pb;
    private TextView tvLoading;
    private final ActivityResultLauncher<Intent> loadRowsActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        busy = true;
                        Uri uri = data.getData();

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Handler handler = new Handler(Looper.getMainLooper());

                        executor.execute(() -> {
                            //Read the file as a string
                            handler.post(() -> {
                                pb.setVisibility(View.VISIBLE);
                                tvLoading.setVisibility(View.VISIBLE);
                                if (getContext() != null) {
                                    notification = new Notification.Builder(getContext(), Constants.NOFICIATION_CHANNEL_1,
                                            LOAD_FILE_NOTIFICATION_ID)
                                            .title(getString(R.string.loading_media))
                                            .content(getString(R.string.preparing))
                                            .priority(android.app.Notification.PRIORITY_LOW)
                                            .smallIcon(R.mipmap.ic_launcher).build();
                                }
                            });

                            String text = CacheDependentActivity.readStringFromUri(uri, getActivity());
                            if (!text.equals("")) {
                                //Prepare for inserting rows into db
                                MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
                                ;
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
                                        notification.setTitle(getString(R.string.loading_complete));
                                        notification.setContent(numRows + "/" + numRows);
                                        notification.setProgress(0);
                                        notification.show();
                                    }
                                    pb.setProgress(0);
                                    pb.setVisibility(View.GONE);
                                    String output = getString(R.string.loading_complete) + ", " +
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
    protected void initViews(View v) {
        rlImportRaw = v.findViewById(R.id.fragment_media_import_rl_raw);
        rlImportFile = v.findViewById(R.id.fragment_media_import_rl_file);
        rgImportType = v.findViewById(R.id.fragment_media_import_rg_importtype);
        etInputPath = v.findViewById(R.id.fragment_media_import_et_inputpath);

        btnChooseDir = v.findViewById(R.id.fragment_media_import_btn_choosedir);
        btnChooseFile = v.findViewById(R.id.fragment_media_import_btn_choosefile);
        btnImport = v.findViewById(R.id.fragment_media_import_btn_import);

        pb = v.findViewById(R.id.fragment_media_import_pb_loading);
        tvLoading = v.findViewById(R.id.fragment_media_import_tv_loading);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rgImportType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.fragment_media_import_rg_importtype_raw) {
                rlImportFile.setVisibility(View.GONE);
                rlImportRaw.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.fragment_media_import_rg_importtype_file) {
                rlImportRaw.setVisibility(View.INVISIBLE);
                rlImportFile.setVisibility(View.VISIBLE);
            }
        });

        SharedPreferences prefs = requireActivity().getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);

        etInputPath.setText(prefs.getString(Constants.PATH, Environment.DIRECTORY_PICTURES));

        getParentFragmentManager().setFragmentResultListener(Constants.FRAGMENT_NEW_DIR_CALLBACK, this, (requestKey, result) -> {
            String newPath = result.getString(Constants.FRAGMENT_NEW_DIR_CALLBACK);
            etInputPath.setText(newPath);
        });
        btnChooseDir.setOnClickListener(v -> {
            String userInputPath = etInputPath.getText().toString();
            FragmentManager fm = requireActivity().getSupportFragmentManager();
            ChooseDirFragment chooseDirFragment = new ChooseDirFragment(userInputPath);
            chooseDirFragment.show(fm, Constants.FRAGMENT_CHOOSE_DIR);
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//            chooseDirActivity.launch(intent);
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

                tvLoading.setVisibility(View.VISIBLE);
                pb.setVisibility(View.VISIBLE);

                if (getContext() != null) {
                    notification = new Notification.Builder(getContext(), Constants.NOFICIATION_CHANNEL_1,
                            LOAD_MEDIA_ROOT_NOTIFICATION_ID)
                            .title(getString(R.string.loading_media))
                            .content(getString(R.string.preparing))
                            .priority(android.app.Notification.PRIORITY_LOW)
                            .smallIcon(R.mipmap.ic_launcher).build();
                }

                tvLoading.setText(String.format("%s, %s", getString(R.string.loading_media), getString(R.string.preparing)));

                String path = etInputPath.getText().toString();
                SharedPreferences.Editor edit = prefs.edit();

                edit.putString(Constants.PATH, path);
                edit.apply();

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    //Get all valid media files in root directory
                    File root = new File(path);
                    String[] filesInRoot = root.list((dir, name) -> {
                        int startingIndex = name.lastIndexOf(".");
                        if (startingIndex == -1) {
                            return false;
                        }
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
                    Set<String> allMediaPaths = myOpenHelper.getMediaPaths(db);
                    filePaths.removeAll(allMediaPaths);

                    int count = 0;
                    //Insert media
                    db.beginTransaction();
                    for (String filePath : filePaths) {
                        int extensionIndex = filePath.lastIndexOf(".");
                        String fileName = filePath.substring(filePath.lastIndexOf("/", extensionIndex - 1) + 1);
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
}