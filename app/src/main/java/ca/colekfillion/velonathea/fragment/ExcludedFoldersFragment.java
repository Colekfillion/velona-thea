package ca.colekfillion.velonathea.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import ca.colekfillion.velonathea.R;
import ca.colekfillion.velonathea.database.MyOpenHelper;
import ca.colekfillion.velonathea.pojo.Constants;

//TODO: Fix performance for this class
public class ExcludedFoldersFragment extends BaseDialogFragment {

    private ListView lvExcludedFolders;
    private ListViewAdapter lvAdapter;
    private Button btnConfirmFolders;
    private ArrayList<Pair<String, String>> dbFolders;

    @Override
    protected void initViews(View v) {
        lvExcludedFolders = v.findViewById(R.id.fragment_excluded_folders_lv_excludedfolders);
        btnConfirmFolders = v.findViewById(R.id.fragment_excluded_folders_btn_confirm);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_excluded_folders;
    }

    @Override
    protected String getName() {
        return "ExcludedFoldersFragment";
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentActivity parent = getActivity();
        if (parent == null) {
            dismiss();
        }
        MyOpenHelper myOpenHelper = new MyOpenHelper(getContext(), MyOpenHelper.DATABASE_NAME, null, MyOpenHelper.DATABASE_VERSION);
        SQLiteDatabase db = myOpenHelper.getReadableDatabase();
        String[] dbFoldersArray = myOpenHelper.getDbFolders(db);
        dbFolders = new ArrayList<>();
        for (String s : dbFoldersArray) {
            dbFolders.add(new Pair<>(s, "+"));
        }
        lvAdapter = new ListViewAdapter();
        lvExcludedFolders.setAdapter(lvAdapter);

        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        Set<String> excludedFolders = new HashSet<>(prefs.getStringSet(Constants.PREFS_EXCLUDED_FOLDERS, new HashSet<>()));

        //Parse through excludedFolders and change each symbol to - if there's a matching entry in listview
        for (String excludedFolder : excludedFolders) {
            for (int i = 0; i < dbFolders.size(); i++) {
                Pair<String, String> dbFolder = dbFolders.get(i);
                if (dbFolder.first.equals(excludedFolder)) {
                    dbFolders.set(i, new Pair<>(dbFolder.first, "-"));
                }
            }
        }

        //Reverse symbol when tapping on entry, add or remove from excludedFolders
        lvExcludedFolders.setOnItemClickListener((list, item, position, id) -> {
            Pair<String, String> current = dbFolders.get(position);

            if (excludedFolders.contains(current.first)) {
                excludedFolders.remove(current.first);
                dbFolders.set(position, new Pair<>(current.first, "+"));
            } else {
                excludedFolders.add(current.first);
                dbFolders.set(position, new Pair<>(current.first, "-"));
            }
            lvAdapter.notifyDataSetChanged();
        });

        //Confirm button that updates preferences and dismisses fragment
        btnConfirmFolders.setOnClickListener(view1 -> {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putStringSet(Constants.PREFS_EXCLUDED_FOLDERS, excludedFolders);
            edit.apply();
            Toast.makeText(getContext(), "Confirmed", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    private class ListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return dbFolders.size();
        }

        @Override
        public Object getItem(int i) {
            return dbFolders.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View old, ViewGroup parent) {
            View newView = old;
            if (old == null) {
                newView = getLayoutInflater().inflate(R.layout.row_excluded_folders_lv, parent, false);
            }

            TextView tvFolder = newView.findViewById(R.id.fragment_excluded_folders_tv_folder);
            TextView tvSymbol = newView.findViewById(R.id.fragment_excluded_folders_tv_symbol);
            Pair<String, String> current = (Pair<String, String>) getItem(i);
            tvFolder.setText(current.first);
            tvSymbol.setText(current.second);
            return newView;
        }
    }
}