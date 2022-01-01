package ca.quadrexium.velonathea.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import ca.quadrexium.velonathea.R;
import ca.quadrexium.velonathea.pojo.Constants;

//TODO: Convert this into a fragment that shows in ConfigActivity
public class ChooseDirActivity extends BaseActivity {

    private final Set<String> dirNames = new LinkedHashSet<>();
    private ListAdapter dnAdapter;
    private File currentDir;
    boolean showHiddenFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        showHiddenFiles = prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false);
        currentDir = new File(prefs.getString(Constants.PATH, Environment.getExternalStorageDirectory().getAbsolutePath()));

        ListView lvDirs = findViewById(R.id.activity_choose_dir_lv_dirs);
        lvDirs.setAdapter(dnAdapter = new ListAdapter());

        listDirs();

        lvDirs.setOnItemClickListener((list, item, position, id) -> {
            TextView tvDir = item.findViewById(R.id.activity_choose_dir_tv_dir);
            String fileName = tvDir.getText().toString();
            if (!fileName.equals("...")) {
                currentDir = new File(currentDir, fileName);
            } else if (currentDir.getParent() != null){
                currentDir = currentDir.getParentFile();
            }
            listDirs();
        });

        Button confirmDirButton = findViewById(R.id.activity_choose_dir_btn_confirm);
        confirmDirButton.setOnClickListener(v -> {
            Intent i = new Intent();
            i.putExtra(Constants.PATH, currentDir.getAbsolutePath());
            setResult(RESULT_OK, i);
            finish();
        });
    }

    /**
     * Lists all directories within currentDir, and a '...' one for backing out.
     */
    //TODO: Async this and add a progress bar, specifically for the File::isDirectory check
    private void listDirs() {
        dirNames.clear();
        File[] dirs = currentDir.listFiles(File::isDirectory);
        if (currentDir.getParent() != null) {
            //If the dir has a parent, add a '...' entry that represents it
            dirNames.add("...");
        }
        if (dirs != null) {
            for (File f : dirs) {
                //Only show files the user has access to
                if (showHiddenFiles || !f.getName().contains(".")) {
                    dirNames.add(f.getName());
                }
            }
        }
        dnAdapter.notifyDataSetChanged();
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
    protected int getLayoutResourceId() { return R.layout.activity_choose_dir; }

    @Override
    protected void isVerified() { }
}