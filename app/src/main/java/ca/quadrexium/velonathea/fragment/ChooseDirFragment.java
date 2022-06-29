package ca.quadrexium.velonathea.fragment;

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

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.pojo.Constants;

//TODO: Fix performance for this class
public class ChooseDirFragment extends BaseDialogFragment {

    private final Set<String> dirNames = new LinkedHashSet<>();
    private ListAdapter dnAdapter;
    private File currentDir;
    boolean showHiddenFiles;
    ExecutorService executor = Executors.newSingleThreadExecutor();

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

        ListView lvDirs = view.findViewById(R.id.fragment_choose_dir_lv_dirs);
        lvDirs.setAdapter(dnAdapter = new ListAdapter());

        listDirs(view);

        lvDirs.setOnItemClickListener((list, item, position, id) -> {
            TextView tvDir = item.findViewById(R.id.activity_choose_dir_tv_dir);
            String fileName = tvDir.getText().toString();
            if (!fileName.equals("...")) {
                currentDir = new File(currentDir, fileName);
            } else if (currentDir.getParent() != null){
                currentDir = currentDir.getParentFile();
            }
            listDirs(view);
        });

        Button confirmDirButton = view.findViewById(R.id.fragment_choose_dir_btn_confirm);
        confirmDirButton.setOnClickListener(v -> {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(Constants.PATH, currentDir.getAbsolutePath());
            edit.apply();
            dismiss();
        });
    }

    /**
     * Lists all directories within currentDir, and a '...' one for backing out.
     */
    private void listDirs(View view) {
        Handler handler = new Handler(Looper.getMainLooper());

        TextView tvFolderName = view.findViewById(R.id.fragment_choose_dir_tv_foldername);
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
                    pb.setProgress((int) (((double)count.get() / (double)numFilesInDir)*100));
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

            handler.post(() -> {
                pb.setVisibility(View.INVISIBLE);
            });
        });
    }

//    @Override
//    public void onBackPressed() {
//        String rootDirParent = rootDir.getParent();
//        if (rootDirParent != null && !rootDir.getAbsolutePath().equals(startingDir.getAbsolutePath())) {
//            rootDir = new File(rootDirParent);
//            listDirs();
//        } else {
//            super.onBackPressed();
//        }
//    }

    /**
     * ListAdapter for showing each folder in dirNames.
     */
    private class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() { return dirNames.size(); }

        @Override
        public Object getItem(int i) { return dirNames.toArray()[i]; }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int i, View old, ViewGroup parent) {
            View newView = old;
            LayoutInflater inflater = getLayoutInflater();

            String fileName = (String) getItem(i);

            //Initialize new view based on message_send layout
            if (old == null) {
                newView = inflater.inflate(R.layout.row_choose_dir_lv_dirs, parent, false);
            }

            //Find message widget and set it to the corresponding message text in the arraylist
            TextView tvDir = newView.findViewById(R.id.activity_choose_dir_tv_dir);
            tvDir.setText(fileName);

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