package ca.quadrexium.velonathea;

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
import java.util.ArrayList;

public class ChooseDirActivity extends BaseActivity {

    private final ArrayList<Folder> dirList = new ArrayList<>();
    private ListAdapter adapter;
    private final File startingDir = Environment.getExternalStorageDirectory();
    private File rootDir = startingDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListView listView = findViewById(R.id.activity_choose_dir_lv_dirs);
        listView.setAdapter(adapter = new ListAdapter());

        listDirs();

        listView.setOnItemClickListener((list, item, position, id) -> {
            Folder f = dirList.get(position);
            rootDir = new File(f.getFullPath());
            listDirs();
        });

        Button confirmDirButton = findViewById(R.id.activity_choose_dir_btn_confirm);
        confirmDirButton.setOnClickListener(v -> {
            Intent i = new Intent();
            i.putExtra("path", rootDir.getAbsolutePath());
            setResult(RESULT_OK, i);
            finish();
        });
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_choose_dir;
    }

    private void listDirs() {
        dirList.clear();
        File[] dirs = rootDir.listFiles(File::isDirectory);
        if (rootDir.getParent() != null) {
            dirList.add(new Folder("...", rootDir.getParent()));
        }
        if (dirs != null) {
            SharedPreferences prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
            boolean showHiddenFiles = prefs.getBoolean("showHiddenFiles", false);
            for (File f : dirs) {
                if (showHiddenFiles || !f.getName().contains(".")) {
                    dirList.add(new Folder(f.getName(), f.getAbsolutePath()));
                }
            }
        }
        adapter.notifyDataSetChanged();
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

    private static class Folder {
        String name;
        String fullPath;

        Folder(String name, String fullPath) {
            this.name = name;
            this.fullPath = fullPath;
        }

        public String getName() {
            return name;
        }
        public String getFullPath() {
            return fullPath;
        }
    }

    private class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() { return dirList.size(); }

        @Override
        public Object getItem(int i) { return dirList.get(i); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int i, View old, ViewGroup parent) {
            View newView = old;
            LayoutInflater inflater = getLayoutInflater();
            Folder f = ((Folder) getItem(i));

            //Initialize new view based on message_send layout
            if (old == null) {
                newView = inflater.inflate(R.layout.row_choose_dir_lv_dirs, parent, false);
            }

            //Find message widget and set it to the corresponding message text in the arraylist
            TextView name = newView.findViewById(R.id.activity_choose_dir_tv_dir);
            name.setText(f.getName());

            return newView;
        }
    }
}