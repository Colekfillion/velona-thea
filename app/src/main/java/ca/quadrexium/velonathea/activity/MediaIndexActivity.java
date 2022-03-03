package ca.quadrexium.velonathea.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.database.MyOpenHelper;
import ca.quadrexium.velonathea.pojo.Constants;
import ca.quadrexium.velonathea.pojo.Media;
import ca.quadrexium.velonathea.python.ImageResult;
import ca.quadrexium.velonathea.python.ResultContainer;

public class MediaIndexActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);

        EditText etApiKey = findViewById(R.id.activity_media_index_et_apikey);
        etApiKey.setText(prefs.getString(Constants.API_KEY, ""));

        EditText etMinSim = findViewById(R.id.activity_media_index_et_minsim);
        etMinSim.setText(prefs.getString(Constants.MINSIM, "80"));

        Button btnExecute = findViewById(R.id.activity_media_index_btn_execute);
        btnExecute.setOnClickListener(v -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            AtomicBoolean shouldQuit = new AtomicBoolean(false);
            final String[] quitMessage = {""};

            ProgressBar pbProgress = findViewById(R.id.activity_media_index_pb_progress);
            pbProgress.setVisibility(View.VISIBLE);

            TextView tvProgress = findViewById(R.id.activity_media_index_tv_progress);
            tvProgress.setVisibility(View.VISIBLE);

            btnExecute.setText("Stop");
            btnExecute.setOnClickListener(v1 -> {
                shouldQuit.set(true);
            });

            executor.execute(() -> {
                MyOpenHelper myOpenHelper = getMyOpenHelper();
                SQLiteDatabase db = myOpenHelper.getWritableDatabase();

                ArrayList<Media> unindexedMediaList = myOpenHelper.getUnindexedMedia(db);

                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(getApplicationContext()));
                }

                Python py = Python.getInstance();

                PyObject main = py.getModule("main_script");

                Map<Integer, String> unindexedMedia = new HashMap<>();
                for (Media media : unindexedMediaList) {
                    String filePath = media.getFilePath();
                    String extension = filePath.substring(filePath.lastIndexOf("."));
                    if (Constants.IMAGE_EXTENSIONS.contains(extension) || extension.equals(".gif")) {
                        unindexedMedia.put(media.getId(), filePath);
                    }
                }

                boolean retry;
                int i = 0;
                int size = unindexedMedia.size();
                String message = "";

                if (size == 0) {
                    tvProgress.setText("No media to index/All media already indexed");
                    pbProgress.setProgress(100);
                    btnExecute.setText("Execute");
                }

                for (Map.Entry<Integer, String> entry : unindexedMedia.entrySet()) {
                    i++;
                    retry = true;
                    String filePath = entry.getValue();
                    System.out.println(filePath);
                    File f = new File(filePath);
                    Date startingTime = new Date();
                    message += "[" + new SimpleDateFormat("HH:mm:ss").format(startingTime) + "] " + f.getName() + ": ";
                    if (f.exists()) {
                        PyObject returnValue;
                        try {
                            while (retry) {
                                returnValue = main.callAttr("get_metadata",
                                        getBytes(f),
                                        "f27f5f299e284d6cfc4114270fbda03fa97e3234",
                                        etMinSim.getText().toString());
                                PyObject[] returnList = returnValue.asList().toArray(new PyObject[0]);
                                int resultCode = returnList[0].toInt();
                                if (resultCode == 1) {
                                    ResultContainer results = returnList[1].toJava(ResultContainer.class);
                                    if (results.getImageResults().length > 0) {
                                        ImageResult best = results.getImageResults()[0];
                                        int extensionIndex = filePath.lastIndexOf(".");
                                        String backupName = filePath.substring(filePath.lastIndexOf("/", extensionIndex - 1)+1, extensionIndex);
                                        Media.Builder mediaBuilder = new Media.Builder()
                                                .id(entry.getKey())
                                                .filePath(filePath)
                                                .name(Constants.isStringEmpty(best.getTitle()) ? backupName : best.getTitle())
                                                .author(Constants.isStringEmpty(best.getAuthor()) ? "unknown" : best.getAuthor())
                                                .link(best.getSources() != null && best.getSources().length > 0 ? best.getSources()[0] : "");
                                        if (best.getTags() != null && best.getTags().length > 0) {
                                            mediaBuilder.tags(new HashSet<>(Arrays.asList(best.getTags())));
                                        }
                                        Media updatedMedia = mediaBuilder.build();
                                        myOpenHelper.updateMedia(db, updatedMedia);
                                        message += "Indexed";
                                    } else {
                                        //If no results were returned
                                        myOpenHelper.setIndexed(db, entry.getKey());
                                        message += "No results found";
                                    }
                                    retry = false;
                                } else if (resultCode == -1) {
                                    retry = false;
                                    shouldQuit.set(true);
                                    message += returnList[1].toString();
                                    quitMessage[0] = returnList[1].toString();
                                } else if (resultCode == -2) {
                                    message += returnList[2].toString();
                                    int finalI = i;
                                    String finalMessage = message;
                                    handler.post(() -> {
                                        double percent = ((double) finalI / (double) size) * 100;
                                        pbProgress.setProgress((int) percent);
                                        tvProgress.setText(finalMessage);
                                    });
                                    //Controls retry time and allows users to quit in the middle of wait times
                                    int sleepTime = (int) ((returnList[1].toInt() * 1000L)/5000);
                                    System.out.println(returnList[1].toInt());
                                    for (int ii = 0; ii < sleepTime; ii++) {
                                        Thread.sleep(5 * 1000L);
                                        if (shouldQuit.get()) {
                                            break;
                                        }
                                    }
                                } else if (resultCode == -3) {
                                    message += returnList[1].toString();
                                    int finalI = i;
                                    String finalMessage = message;
                                    handler.post(() -> {
                                        double percent = ((double) finalI / (double) size) * 100;
                                        pbProgress.setProgress((int) percent);
                                        tvProgress.setText(finalMessage);
                                    });
                                    retry = false;
                                }
                            }
                            if (shouldQuit.get()) {
                                break;
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    int finalI = i;
                    String finalMessage = message;
                    handler.post(() -> {
                        double percent = ((double) finalI / (double) size) * 100;
                        pbProgress.setProgress((int) percent);
                        tvProgress.setText(finalMessage);
                    });
                    message += "\n";
                }
                handler.post(() -> {
                    if (shouldQuit.get()) {
                        if (quitMessage[0] != "") {
                            Toast.makeText(this, quitMessage[0], Toast.LENGTH_LONG).show();
                        }
                        finish();
                    }
                });
            });
        });
    }

    public byte[] getBytes(File f) throws IOException {
        InputStream is = getContentResolver().openInputStream(Uri.fromFile(f));
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_media_index; }

    @Override
    protected void isVerified() { }
}