package ca.colekfillion.velonathea.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import ca.colekfillion.velonathea.R;
import ca.colekfillion.velonathea.database.MyOpenHelper;
import ca.colekfillion.velonathea.database.Query;
import ca.colekfillion.velonathea.pojo.Constants;
import ca.colekfillion.velonathea.pojo.Filter;

//TODO: Filter presets stored in either DB or sharedprefs
public class MainActivity extends BaseActivity {

    private final ArrayList<String> filterKeys = new ArrayList<>();
    private AutoCompleteTextView actvInput;
    private ImageButton iBtnInput;
    private Spinner spnrFilter;
    private Spinner spnrSortby;
    private ListView filterListView;
    private Button btnSearch;
    private Spinner spnrIsNot;
    private Map<String, Filter> filterMap = new LinkedHashMap<>();

    @Override
    protected void initViews() {
        spnrFilter = findViewById(R.id.activity_main_spnr_filter);
        spnrIsNot = findViewById(R.id.activity_main_spnr_isnot);
        actvInput = findViewById(R.id.activity_main_actv_input);
        iBtnInput = findViewById(R.id.activity_main_btn_clearinput);
        spnrSortby = findViewById(R.id.activity_main_spnr_sortby);
        filterListView = findViewById(R.id.activity_main_lv_filters);
        btnSearch = findViewById(R.id.activity_main_btn_search);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);
        createNotificationChannel();

        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this, R.array.filters_array, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnrFilter.setAdapter(filterAdapter);
        actvInput.setHint(spnrFilter.getSelectedItem().toString());

        ArrayAdapter<CharSequence> isNotAdapter = ArrayAdapter.createFromResource(this, R.array.isnot_array, android.R.layout.simple_spinner_item);
        isNotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnrIsNot.setAdapter(isNotAdapter);
        iBtnInput.setOnClickListener(v -> actvInput.setText(""));

        ArrayAdapter<CharSequence> sortbyAdapter = ArrayAdapter.createFromResource(this, R.array.sortby_array, android.R.layout.simple_spinner_item);
        sortbyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnrSortby.setAdapter(sortbyAdapter);

        //Autocomplete
        Set<String> tags = new HashSet<>();
        Set<String> authors = new HashSet<>();
        AtomicReference<ArrayAdapter<String>> tagsAdapter = new AtomicReference<>(new ArrayAdapter<>(this,
                R.layout.textview_autocomplete,
                new ArrayList<>(tags)));
        AtomicReference<ArrayAdapter<String>> authorsAdapter = new AtomicReference<>(new ArrayAdapter<>(this,
                R.layout.textview_autocomplete,
                new ArrayList<>(authors)));
        spnrFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String filterType = spnrFilter.getSelectedItem().toString();
                actvInput.setHint(filterType);
                if (filterType.equals("Tag")) {
                    if (tags.size() == 0) {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Handler handler = new Handler(Looper.getMainLooper());

                        executor.execute(() -> {
                            MyOpenHelper myOpenHelper = getMyOpenHelper();
                            SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                            tags.addAll(myOpenHelper.getTagNameSet(db));
                            db.close();
                            tagsAdapter.set(new ArrayAdapter<>(getApplicationContext(),
                                    R.layout.textview_autocomplete,
                                    new ArrayList<>(tags)));
                            handler.post(() -> actvInput.setAdapter(tagsAdapter.get()));
                        });
                        executor.shutdown();
                    }
                    actvInput.setAdapter(tagsAdapter.get());
                } else if (filterType.equals("Author")) {
                    if (authors.size() == 0) {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Handler handler = new Handler(Looper.getMainLooper());

                        executor.execute(() -> {
                            MyOpenHelper myOpenHelper = getMyOpenHelper();
                            SQLiteDatabase db = myOpenHelper.getReadableDatabase();
                            authors.addAll(myOpenHelper.getAuthorSet(db));
                            db.close();
                            authorsAdapter.set(new ArrayAdapter<>(getApplicationContext(),
                                    R.layout.textview_autocomplete,
                                    new ArrayList<>(authors)));
                            handler.post(() -> actvInput.setAdapter(authorsAdapter.get()));
                        });
                        executor.shutdown();
                    }
                    actvInput.setAdapter(authorsAdapter.get());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        FilterListAdapter filterListAdapter = new FilterListAdapter();
        filterListView.setAdapter(filterListAdapter);

        //On enter in the input box, put filter in the filterList
        actvInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP &&
                    keyCode == KeyEvent.KEYCODE_ENTER) {
                boolean include, isOr;
                include = !spnrIsNot.getSelectedItem().toString().equals("IS NOT");
                isOr = spnrIsNot.getSelectedItem().toString().equals("IN");

                String key = spnrFilter.getSelectedItem().toString() + "_" + spnrIsNot.getSelectedItem().toString();
                if (filterMap.get(key) != null) {
                    filterMap.get(key).addArg(actvInput.getText().toString());
                } else {
                    Filter newFilter = new Filter(spnrFilter.getSelectedItem().toString(), include, isOr, new LinkedHashSet<>(Collections.singleton(actvInput.getText().toString())));
                    filterMap.put(key, newFilter);
                    filterKeys.add(key);
                }
                filterListAdapter.notifyDataSetChanged();
                return true;
            }
            return false;
        });

        btnSearch.setOnClickListener(view -> {
            Bundle dataToPass = new Bundle();

            Pair<String, String[]> queryData = createSearchQuery();

            dataToPass.putString(Constants.MEDIA_QUERY, queryData.first);
            dataToPass.putStringArray(Constants.QUERY_ARGS, queryData.second);

            Log.d("DB", "Query: " + queryData.first);
            Log.d("DB", "Args: " + Arrays.toString(queryData.second));

            Intent intent = new Intent(this, SearchResultsActivity.class);
            intent.putExtras(dataToPass);
            startActivity(intent);
        });
    }

    private void debugSetFilters() {
        filterMap.clear();

        String key = "Filename_IN";
        Filter newFilter = new Filter("Filename", true, true, new LinkedHashSet<>(Arrays.asList("%3%", "%5%")));
        filterMap.put(key, newFilter);
        filterKeys.add(key);

        key = "Filename_IS";
        newFilter = new Filter("Filename", true, false, new LinkedHashSet<>(Arrays.asList("%_p_%", "%.jpg")));
        filterMap.put(key, newFilter);
        filterKeys.add(key);

        key = "Author_IS NOT";
        newFilter = new Filter("Author", false, false, new LinkedHashSet<>(Collections.singletonList("unknown")));
        filterMap.put(key, newFilter);
        filterKeys.add(key);

        key = "Author_IS";
        newFilter = new Filter("Author", true, true, new LinkedHashSet<>(Collections.singletonList("nekoya saki")));
        filterMap.put(key, newFilter);
        filterKeys.add(key);

        key = "Tag_IN";
        newFilter = new Filter("Tag", true, true, new LinkedHashSet<>(Arrays.asList("3girls", "2girls")));
        filterMap.put(key, newFilter);
        filterKeys.add(key);
    }

    private Pair<String, String[]> createSearchQuery() {
        MyOpenHelper myOpenHelper = getMyOpenHelper();
        SQLiteDatabase db = myOpenHelper.getReadableDatabase();

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        boolean showHiddenFiles = prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false);
        if (!showHiddenFiles) {
            Filter newFilter = new Filter("Folder", false, false, new LinkedHashSet<>(Collections.singleton("%/.%")));
            String key = "Folder_IS_NOT";
            filterMap.put(key, newFilter);
            filterKeys.add(key);
        }
        Set<String> excludedFolders = new LinkedHashSet<>(prefs.getStringSet(Constants.PREFS_EXCLUDED_FOLDERS, new HashSet<>()));
        Set<String> excludedFoldersMod = new LinkedHashSet<>();
        if (excludedFolders.size() > 0) {
            for (String excludedFolder :
                    excludedFolders) {
                excludedFoldersMod.add(excludedFolder + "%");
            }
            Filter newFilter = new Filter("Folder", false, true, excludedFoldersMod);
            String key = "Folder_IS_NOT";
            filterMap.put(key, newFilter);
            filterKeys.add(key);
        }

        ArrayList<String> argsList = new ArrayList<>();
        Query.Builder qb = new Query.Builder();
        qb.select(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID)
                .select(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_FILENAME)
                .select(MyOpenHelper.FILEPATH_TABLE, MyOpenHelper.COL_FILEPATH_NAME)
                .from(MyOpenHelper.MEDIA_TABLE);
        qb.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_FILEPATH_ID, MyOpenHelper.FILEPATH_TABLE, MyOpenHelper.COL_FILEPATH_ID);

        if (filterMap.size() > 0) {
            for (Filter filter : filterMap.values()) {
                switch (filter.getType()) {
                    case "Filename":
                        qb.where(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_FILENAME,
                                filter.getArgs().size(), true, filter.isInclude(), filter.isOr());
                        argsList.addAll(filter.getArgs());
                        break;
                    case "Name":
                        qb.where(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_NAME,
                                filter.getArgs().size(), true, filter.isInclude(), filter.isOr());
                        argsList.addAll(filter.getArgs());
                        break;
                    case "Author":
                        if (filter.isInclude()) {
                            qb.whereIn(MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_NAME,
                                    filter.getArgs().size(), filter.isInclude());
                        } else {
                            qb.where(MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_NAME,
                                    filter.getArgs().size(), false, filter.isInclude(), false);
                        }
                        qb.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_AUTHOR_ID, MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_ID);
                        argsList.addAll(filter.getArgs());
                        break;
                    case "Tag":
                        if (!filter.isInclude()) {
                            Query.Builder tagNotQb = new Query.Builder();
                            tagNotQb.select(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID)
                                    .from(MyOpenHelper.MEDIA_TAG_TABLE);
                            tagNotQb.whereIn(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_TAG_ID,
                                    filter.getArgs().size(), true);

                            qb.whereIn(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID,
                                    tagNotQb, false);
                            qb.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID, MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID);

                            for (String tag : filter.getArgs()) {
                                argsList.add(String.valueOf(myOpenHelper.getTagId(db, tag)));
                            }
                        }
                        break;
                    case "Folder":
                        qb.where(MyOpenHelper.FILEPATH_TABLE, MyOpenHelper.COL_FILEPATH_NAME,
                                filter.getArgs().size(), true, filter.isInclude(), filter.isOr());
                        argsList.addAll(filter.getArgs());
                        break;
                }
            }
            int totalTagArgs = 0;
            if (filterMap.get("Tag_IS") != null) {
                totalTagArgs += filterMap.get("Tag_IS").getArgs().size();
            }
            if (filterMap.get("Tag_IN") != null) {
                totalTagArgs += 1;
            }
            if (totalTagArgs > 0) {
                Filter filter;
                if (filterMap.get("Tag_IN") != null) {
                    filter = filterMap.get("Tag_IN");
                    qb.whereIn(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_TAG_ID,
                            filter.getArgs().size(), true);
                    qb.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID, MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID);
                    for (String tag : filter.getArgs()) {
                        argsList.add(String.valueOf(myOpenHelper.getTagId(db, tag)));
                    }
                }
                if (filterMap.get("Tag_IS") != null) {
                    filter = filterMap.get("Tag_IS");
                    if (filterMap.get("Tag_IN") != null) {
                        qb.whereIn(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_TAG_ID,
                                1, true);
                        qb.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID, MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID);
                        for (String tag : filter.getArgs()) {
                            argsList.add(String.valueOf(myOpenHelper.getTagId(db, tag)));
                            filter.getArgs().remove(tag);
                            break;
                        }
                    }
                    for (String tag : filter.getArgs()) {
                        Query.Builder qbIntersect = new Query.Builder();
                        qbIntersect.select(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID)
                                .select(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_FILENAME)
                                .select(MyOpenHelper.FILEPATH_TABLE, MyOpenHelper.COL_FILEPATH_NAME)
                                .from(MyOpenHelper.MEDIA_TABLE);
                        qbIntersect.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_FILEPATH_ID, MyOpenHelper.FILEPATH_TABLE, MyOpenHelper.COL_FILEPATH_ID);
                        qbIntersect.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_ID, MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_MEDIA_ID);
                        qbIntersect.where(MyOpenHelper.MEDIA_TAG_TABLE, MyOpenHelper.COL_MEDIA_TAG_TAG_ID, 1, false, true, false);
                        qb.intersect(qbIntersect);
                        argsList.add(String.valueOf(myOpenHelper.getTagId(db, tag)));
                    }
                }
            }
        }
        qb.groupBy(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_FILENAME);
        switch (spnrSortby.getSelectedItem().toString()) {
            case "Filename":
                qb.orderBy(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_FILENAME);
                break;
            case "Name":
                qb.select(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_NAME);
                qb.orderBy(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_NAME);
                break;
            case "Author":
                qb.select(MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_NAME);
                qb.join(MyOpenHelper.MEDIA_TABLE, MyOpenHelper.COL_MEDIA_AUTHOR_ID, MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_ID);
                qb.orderBy(MyOpenHelper.AUTHOR_TABLE, MyOpenHelper.COL_AUTHOR_NAME);
                break;
            case "Random":
                qb.orderBy("RANDOM()");
                break;
        }

        String[] args = argsList.toArray(new String[0]);
        String query = qb.build().getQuery();
        return new Pair<>(query, args);
    }

    @Override
    protected void isVerified() {
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    public class FilterListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filterKeys.size();
        }

        @Override
        public Object getItem(int i) {
            return filterMap.get(filterKeys.get(i));
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.filter_listview, viewGroup, false);
            }

            Filter current = (Filter) getItem(i);
            CardView cardView = view.findViewById(R.id.filter_listview_cv);
            if (current.isInclude()) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
            }

            TextView tvDesc = view.findViewById(R.id.filter_listview_tv_desc);
            String tvDescText = "";
            switch (current.getType()) {
                case "Filename":
                    tvDescText = "FN";
                    break;
                case "Name":
                    tvDescText = "N";
                    break;
                case "Author":
                    tvDescText = "A";
                    break;
                case "Tag":
                    tvDescText = "T";
                    break;
            }
            tvDescText += " ";
            if (current.isInclude() && !current.isOr()) {
                tvDescText += "IS";
            } else if (!current.isInclude() && !current.isOr()) {
                tvDescText += "IS NOT";
            } else if (current.isInclude() && current.isOr()) {
                tvDescText += "IN";
            }
            tvDesc.setText(tvDescText);

            TextView tvArg = view.findViewById(R.id.filter_listview_tv_arg);
            tvArg.setText(current.getArgs().toString());

            ImageButton iBtnClear = view.findViewById(R.id.filter_listview_imgbtn_delete);
            iBtnClear.setOnClickListener(view2 -> {
                filterMap.remove(filterKeys.get(i));
                filterKeys.remove(i);
                notifyDataSetChanged();
            });

            return view;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Delete the query cache if it exists
        File queryCache = new File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath() + "/" + Constants.QUERY_CACHE_FILENAME);
        if (queryCache.exists()) {
            boolean cacheDeleted = queryCache.delete();
            if (!cacheDeleted) {
                Toast.makeText(this, R.string.fail_delete_cache, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected String getName() {
        return "MainActivity";
    }
}