package ca.colekfillion.velonathea.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import ca.colekfillion.velonathea.R;
import ca.colekfillion.velonathea.database.MyOpenHelper;
import ca.colekfillion.velonathea.pojo.Constants;
import ca.colekfillion.velonathea.pojo.Filter;

public class MainActivity extends BaseActivity {

    private final ArrayList<Filter> filterList = new ArrayList<>();
    private AutoCompleteTextView actvInput;
    private ImageButton iBtnInput;
    private Spinner spnrFilter;
    private Spinner spnrSortby;
    private ListView filterListView;
    private Button btnSearch;

    @Override
    protected void initViews() {
        actvInput = findViewById(R.id.activity_main_actv_input);
        iBtnInput = findViewById(R.id.activity_main_btn_clearinput);
        spnrFilter = findViewById(R.id.activity_main_spnr_filter);
        spnrSortby = findViewById(R.id.activity_main_spnr_sortby);
        filterListView = findViewById(R.id.activity_main_lv_filters);
        btnSearch = findViewById(R.id.activity_main_btn_search);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToolbar(R.id.activity_tb_default_toolbar);
        createNotificationChannel();

        iBtnInput.setOnClickListener(v -> actvInput.setText(""));

        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this, R.array.filters_array, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnrFilter.setAdapter(filterAdapter);
        actvInput.setHint(spnrFilter.getSelectedItem().toString());

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
                            Cursor c = db.rawQuery("SELECT " + MyOpenHelper.TAG_TABLE + "." + MyOpenHelper.COL_TAG_NAME + " FROM " + MyOpenHelper.TAG_TABLE,
                                    new String[]{}
                            );
                            c.moveToFirst();
                            while (c.moveToNext()) {
                                tags.add(c.getString(c.getColumnIndex(MyOpenHelper.COL_TAG_NAME)));
                            }
                            c.close();
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
                            Cursor c = db.rawQuery("SELECT " + MyOpenHelper.AUTHOR_TABLE + "." + MyOpenHelper.COL_AUTHOR_NAME + " FROM " + MyOpenHelper.AUTHOR_TABLE,
                                    new String[]{}
                            );
                            c.moveToFirst();
                            while (c.moveToNext()) {
                                authors.add(c.getString(c.getColumnIndex(MyOpenHelper.COL_AUTHOR_NAME)));
                            }
                            c.close();
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
                Filter newFilter = new Filter(spnrFilter.getSelectedItem().toString(), true, false, actvInput.getText().toString());
                filterList.add(newFilter);
                filterListAdapter.notifyDataSetChanged();
                return true;
            }
            return false;
        });

        btnSearch.setOnClickListener(view -> {
            Set<Integer> tagIds = new LinkedHashSet<>();
            MyOpenHelper myOpenHelper = getMyOpenHelper();
            SQLiteDatabase db = myOpenHelper.getReadableDatabase();

            StringBuilder query = new StringBuilder(MyOpenHelper.BASE_QUERY + "JOIN " + MyOpenHelper.FILEPATH_TABLE + " " +
                    "ON " + MyOpenHelper.MEDIA_TABLE + "." + MyOpenHelper.COL_MEDIA_FILEPATH_ID + " = " +
                    MyOpenHelper.FILEPATH_TABLE + "." + MyOpenHelper.COL_FILEPATH_ID + " ");

            SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
            boolean showHiddenFiles = prefs.getBoolean(Constants.PREFS_SHOW_HIDDEN_FILES, false);
            boolean having = false;
            if (filterList.size() > 0 || !showHiddenFiles) {
                ArrayList<String> filenames = new ArrayList<>();
                ArrayList<String> authorNames = new ArrayList<>();
                ArrayList<Filter> tagFilters = new ArrayList<>();
                Set<String> joins = new LinkedHashSet<>();
                String baseQuery;
                for (Filter filter : filterList) {
                    switch (filter.getType()) {
                        case "Author":
                            if (filter.isInclude()) {
                                authorNames.add(filter.getArg());
                            }
                            joins.add(MyOpenHelper.AUTHOR_JOIN);
                            break;
                        case "Tag":
                            tagFilters.add(filter);
                            joins.add("JOIN media_tag ON media.id = media_tag.media_id ");
                            break;
                    }
                }
                for (String join : joins) {
                    query.append(join);
                }
                query.append("WHERE ");
                for (Filter filter : filterList) {
                    switch (filter.getType()) {
                        case "Filename":
                            if (!filter.isOr()) {
                                query.append("media.filename ");
                                break;
                            }
                            continue;
                        case "Name":
                            query.append("media.name ");
                            break;
                        case "Author":
                            if (!filter.isInclude()) {
                                query.append("author.name ");
                                break;
                            }
                            //author OR is handled below
                            continue;
                        case "Tag":
                            continue;
                    }
                    if (!filter.isInclude()) {
                        query.append("NOT ");
                    }
                    query.append("LIKE \"").append(filter.getArg()).append("\" AND ");
                }
                for (Filter filter : filterList) {
                    if (filter.getType().equals("Filename") && filter.isOr()) {
                        filenames.add(filter.getArg());
                    }
                }
                if (filenames.size() > 0) {
                    query.append("(");
                    for (String filename : filenames) {
                        query.append("media.filename LIKE \"").append(filename).append("\" OR ");
                    }
                    query = new StringBuilder(query.substring(0, query.length() - 4));
                    query.append(") AND ");
                }
                if (!showHiddenFiles) {
                    query.append("filepath.name NOT LIKE \"%/.%\" AND ");
                }
                //author OR
                if (authorNames.size() > 0) {
                    query.append("author.name IN (");
                    for (String authorName : authorNames) {
                        query.append("\"").append(authorName).append("\", ");
                    }
                    query = new StringBuilder(query.substring(0, query.length() - 2));
                    query.append(") AND ");
                }
                //tag NOT
                for (Filter filter : tagFilters) {
                    if (!filter.isInclude()) {
                        tagIds.add(myOpenHelper.getTagId(db, filter.getArg()));
                    }
                }
                if (tagIds.size() > 0) {
                    query.append("media.id NOT IN (SELECT media_id FROM media_tag WHERE tag_id IN (");
                    for (Integer tagId : tagIds) {
                        query.append(tagId.toString()).append(", ");
                    }
                    query = new StringBuilder(query.substring(0, query.length() - 2));
                    query.append(")) AND ");
                }
                //removing tag NOT filters
                Iterator<Filter> itr = tagFilters.iterator();
                while (itr.hasNext()) {
                    Filter filter = itr.next();
                    if (!filter.isInclude()) {
                        itr.remove();
                    }
                }
                tagIds.clear();
                baseQuery = query.toString();
                for (Filter filter : tagFilters) {
                    if (filter.isOr()) {
                        tagIds.add(myOpenHelper.getTagId(db, filter.getArg()));
                    }
                }
                if (tagIds.size() > 0) {
                    query.append("media_tag.tag_id IN (");
                    for (Integer tagId : tagIds) {
                        query.append(tagId.toString()).append(", ");
                    }
                    query = new StringBuilder(query.substring(0, query.length() - 2));
                    query.append(") ");
                }
                //if there are still more tag filters (AND filters), we need to intersect
                if (tagIds.size() < tagFilters.size() && tagIds.size() > 0) {
                    query.append("INTERSECT ").append(baseQuery);
                }
                for (Filter filter : tagFilters) {
                    if (!filter.isOr()) {
                        having = true;
                        query.append("media_tag.tag_id = ").append(myOpenHelper.getTagId(db, filter.getArg())).append(" ");
                        query.append("INTERSECT ").append(baseQuery);
                    }
                }
                if (query.toString().endsWith("INTERSECT " + baseQuery)) {
                    query = new StringBuilder(query.substring(0, query.length() - ("INTERSECT " + baseQuery).length()));
                }
                query = new StringBuilder(query.toString().trim());
                if (query.toString().endsWith("AND")) {
                    query = new StringBuilder(query.substring(0, query.length() - 3));
                }
//                if (!query.endsWith("ESCAPE \"\\\"") && query.contains(" LIKE ")) {
//                    query += "ESCAPE \"\\\"";
//                } else {
//                    query = query.replaceAll("ESCAPE \"\"", "");
//                }
                db.close();
            }
            query.append(") GROUP BY (" + MyOpenHelper.MEDIA_TABLE + "_" + MyOpenHelper.COL_MEDIA_FILENAME + ") ");
            if (having) {
                query.append("HAVING COUNT(*) >= 1 ");
            }
            //TODO: For ordering by columns not in the base query (base query selects media id, filename, filepath)
            // Add SELECT column when ordering by author or media name

            switch (spnrSortby.getSelectedItem().toString()) {
                case "Random":
                    query.append("ORDER BY RANDOM()");
                    break;
                case "Filename":
                    query.append("ORDER BY media_filename");

                    break;
                default:
                    query.append("ORDER BY RANDOM()");
            }

            query.insert(0, "SELECT * FROM (");
            String[] args = new String[0];


            Bundle dataToPass = new Bundle();

            dataToPass.putString(Constants.MEDIA_QUERY, query.toString());
            dataToPass.putStringArray(Constants.QUERY_ARGS, args);
            Log.d("DB", "Query: " + query);
            Log.d("DB", "Args: " + Arrays.toString(args));

            Intent intent = new Intent(this, SearchResultsActivity.class);
            intent.putExtras(dataToPass);
            startActivity(intent);
        });
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
            return filterList.size();
        }

        @Override
        public Object getItem(int i) {
            return filterList.get(i);
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
            TextView tvInclude = view.findViewById(R.id.filter_listview_tv_include);
            if (current.isInclude()) {
                tvInclude.setText("+");
                cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                tvInclude.setText("-");
            }
            tvInclude.setOnClickListener(view1 -> {
                current.setInclude(!current.isInclude());
                if (current.isInclude()) {
                    tvInclude.setText("+");
                    cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                } else {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                    tvInclude.setText("-");
                }
            });

            TextView tvArg = view.findViewById(R.id.filter_listview_tv_arg);
            tvArg.setText(current.getType() + ": " + current.getArg());

            ImageButton iBtnClear = view.findViewById(R.id.filter_listview_imgbtn_delete);
            iBtnClear.setOnClickListener(view2 -> {
                filterList.remove(i);
                notifyDataSetChanged();
            });

            //and/or only for tags
            TextView tvAndOr = view.findViewById(R.id.filter_listview_tv_andor);
            if (current.getType().equals("Tag") || current.getType().equals("Filename")) {
                if (!current.isInclude()) {
                    //tag NOT IN is OR, cannot be AND
                    current.setIsOr(false);
                }
                if (current.isOr()) {
                    tvAndOr.setText(R.string.booleanOR);
                } else {
                    tvAndOr.setText(R.string.booleanAND);
                }
                tvAndOr.setVisibility(View.VISIBLE);
                tvAndOr.setOnClickListener(view12 -> {
                    if (!current.isInclude()) {
                        current.setIsOr(false);
                    } else {
                        current.setIsOr(!current.isOr());
                        if (current.isOr()) {
                            tvAndOr.setText(R.string.booleanOR);
                        } else {
                            tvAndOr.setText(R.string.booleanAND);
                        }
                    }
                });
            } else {
                tvAndOr.setVisibility(View.GONE);
            }

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