package ca.colekfillion.velonathea.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ca.colekfillion.velonathea.R;
import ca.colekfillion.velonathea.pojo.Constants;

//TODO: Fix performance for this class
public class ChooseDirFragment extends BaseDialogFragment {

    private final Set<String> dirNames = new LinkedHashSet<>();
    TextView tvFolderName;
    private File currentDir;
    boolean showHiddenFiles;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    ListView lvDirs;
    Button btnConfirmDir;
    private FolderListAdapter dnAdapter;

    @Override
    protected void initViews(View v) {
        tvFolderName = v.findViewById(R.id.fragment_choose_dir_tv_foldername);
        lvDirs = v.findViewById(R.id.fragment_choose_dir_lv_dirs);
        btnConfirmDir = v.findViewById(R.id.fragment_choose_dir_btn_confirm);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_choose_dir;
    }

    @Override
    protected String getName() {
        return "ChooseDirFragment";
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentActivity parent = getActivity();
        if (parent == null) {
            dismiss();
        }
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        showHiddenFiles = prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false);
        currentDir = new File(prefs.getString(Constants.PATH, Environment.getExternalStorageDirectory().getAbsolutePath()));

        lvDirs.setAdapter(dnAdapter = new FolderListAdapter());

        listDirs(view);

        lvDirs.setOnItemClickListener((list, item, position, id) -> {
            TextView tvDir = item.findViewById(R.id.activity_choose_dir_tv_dir);
            String fileName = tvDir.getText().toString();
            if (!fileName.equals("...")) {
                currentDir = new File(currentDir, fileName);
            } else if (currentDir.getParent() != null) {
                currentDir = currentDir.getParentFile();
            }
            listDirs(view);
        });


        btnConfirmDir.setOnClickListener(v -> {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(Constants.PATH, currentDir.getAbsolutePath());
            edit.apply();
            Bundle result = new Bundle();
            result.putString(Constants.FRAGMENT_NEW_DIR_CALLBACK, currentDir.getAbsolutePath());
            getParentFragmentManager().setFragmentResult(Constants.FRAGMENT_NEW_DIR_CALLBACK, result);
            dismiss();
        });
    }

    /**
     * Lists all directories within currentDir, and a '...' one for backing out.
     */
    private void listDirs(View view) {
        Handler handler = new Handler(Looper.getMainLooper());

        tvFolderName.setText(currentDir.getName());

        if (executor.isTerminated() || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }

        executor.execute(() -> {
            dirNames.clear();
            handler.post(() -> dnAdapter.notifyDataSetChanged());
            ProgressBar pb = view.findViewById(R.id.fragment_choose_dir_pb);
            if (currentDir.getParent() != null) {
                //to go back to the parent folder
                dirNames.add("...");
            }
            handler.post(() -> {
                dnAdapter.notifyDataSetChanged();
                pb.setProgress(0);
                pb.setVisibility(View.VISIBLE);
            });

            //Getting number of files for setting the progress bar values
            String[] fileNames = currentDir.list();
            int numFilesInDir = fileNames != null ? fileNames.length : 0;
            AtomicInteger count = new AtomicInteger();

            //Getting all valid dirs
            File[] dirs = currentDir.listFiles(f -> {
                handler.post(() -> {
                    count.getAndIncrement();
                    pb.setProgress((int) (((double) count.get() / (double) numFilesInDir) * 100));
                });
                //Add to dirs if the file is a directory and either showHiddenFiles is true or, if false,
                // the file name does not contain a period (hidden directory)
                return f.isDirectory() && (showHiddenFiles || !f.getName().contains("."));
            });

            //Add all the dir names to dirNames
            if (dirs != null && dirs.length != 0) {
                for (File f : dirs) {
                    //Make sure the dir is the child of the current directory (can change)
                    if (Objects.equals(f.getParentFile(), currentDir)) {
                        dirNames.add(f.getName());
                        handler.post(() -> dnAdapter.notifyDataSetChanged());
                    }
                }
            }

            handler.post(() -> pb.setVisibility(View.INVISIBLE));
        });
    }

    /**
     * ListAdapter for showing each folder in dirNames.
     */
    private class FolderListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return dirNames.size();
        }

        @Override
        public Object getItem(int i) {
            return dirNames.toArray()[i];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int i, View old, ViewGroup parent) {
            View newView = old;
            LayoutInflater inflater = getLayoutInflater();

            if (old == null) {
                newView = inflater.inflate(R.layout.row_choose_dir_lv_dirs, parent, false);
            }

            //Find message widget and set it to the corresponding message text in the arraylist
            TextView tvDir = newView.findViewById(R.id.activity_choose_dir_tv_dir);
            tvDir.setText((String) getItem(i));

            return newView;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (executor.isTerminated() || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        executor.shutdownNow();
    }

}