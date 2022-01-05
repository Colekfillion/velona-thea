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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.activity.ConfigActivity;
import ca.quadrexium.velonathea.pojo.Constants;

public class ChooseDirFragment extends DialogFragment {

    private final Set<String> dirNames = new LinkedHashSet<>();
    private ListAdapter dnAdapter;
    private File currentDir;
    boolean showHiddenFiles;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choose_dir, container, false);
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

        //TODO: Decouple this from ConfigActivity so it can be reused
        Button confirmDirButton = view.findViewById(R.id.fragment_choose_dir_btn_confirm);
        confirmDirButton.setOnClickListener(v -> {
            ConfigActivity parentActivity = ((ConfigActivity)getContext());
            if (parentActivity != null) {
                parentActivity.changePath(currentDir.getAbsolutePath());
            }
            dismiss();
        });
    }

    /**
     * Lists all directories within currentDir, and a '...' one for backing out.
     */
    private void listDirs(View view) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            dirNames.clear();
            ProgressBar pb = view.findViewById(R.id.fragment_choose_dir_pb);
            if (currentDir.getParent() != null) {
                //If the dir has a parent, add a '...' entry that represents it
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
                    //Setting progress
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
                    dirNames.add(f.getName());
                }
            }

            handler.post(() -> {
                dnAdapter.notifyDataSetChanged();
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
}