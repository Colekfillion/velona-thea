package ca.quadrexium.velonathea.pojo;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WhereFilterHashMap extends HashMap<String, Pair<String[], String[]>> {
    public WhereFilterHashMap(WhereFilterHashMap whereFilters) {
        for (Map.Entry<String, Pair<String[], String[]>> pair : whereFilters.entrySet()) {
            put(pair.getKey(), pair.getValue());
        }
    }

    public WhereFilterHashMap() {

    }

    public void addMandatory(String key, String value) {
        if (containsKey(key)) {
            Pair<String[], String[]> pair = get(key);
            ArrayList<String> mandatoryValues = new ArrayList<>(Arrays.asList(pair.first != null ? pair.first : new String[0]));
            mandatoryValues.add(value);
            remove(key);
            put(key, new Pair<>(mandatoryValues.toArray(new String[0]), pair.second));
        } else {
            put(key, new Pair<>(new String[] { value }, null));
        }
    }

    public void addMandatory(String key, Collection<String> values) {
        if (containsKey(key)) {
            Pair<String[], String[]> pair = get(key);
            ArrayList<String> mandatoryValues = new ArrayList<>(Arrays.asList(pair.first != null ? pair.first : new String[0]));
            mandatoryValues.addAll(values);
            remove(key);
            put(key, new Pair<>(mandatoryValues.toArray(new String[0]), pair.second));
        } else {
            put(key, new Pair<>(values.toArray(new String[0]), null));
        }
    }

    public void addOptional(String key, String value) {
        if (containsKey(key)) {
            Pair<String[], String[]> pair = get(key);
            ArrayList<String> optionalValues = new ArrayList<>(Arrays.asList(pair.second != null ? pair.second : new String[0]));
            optionalValues.add(value);
            remove(key);
            put(key, new Pair<>(pair.first, optionalValues.toArray(new String[0])));
        } else {
            put(key, new Pair<>(null, new String[] { value }));
        }
    }

    public void addOptional(String key, Collection<String> values) {
        if (containsKey(key)) {
            Pair<String[], String[]> pair = get(key);
            ArrayList<String> optionalValues = new ArrayList<>(Arrays.asList(pair.second != null ? pair.second : new String[0]));
            optionalValues.addAll(values);
            remove(key);
            put(key, new Pair<>(pair.first, optionalValues.toArray(new String[0])));
        } else {
            put(key, new Pair<>(null, values.toArray(new String[0])));
        }
    }

    public void addOptionalSet(String key, Collection<Collection<String>> values) {
        if (containsKey(key)) {
            Pair<String[], String[]> pair = get(key);
            ArrayList<String> optionalValues = new ArrayList<>(Arrays.asList(pair.second != null ? pair.second : new String[0]));
            optionalValues.addAll(values);
            remove(key);
            put(key, new Pair<>(pair.first, optionalValues.toArray(new String[0])));
        } else {
            put(key, new Pair<>(null, values.toArray(new String[0])));
        }
    }
}
