package ca.colekfillion.velonathea.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public abstract class BaseDialogFragment extends DialogFragment {

    protected String fragmentName;

    protected abstract void initViews(View v);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentName = getName();

        Log.d("LIFECYCLE", "In " + getName() + " onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResourceId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    /**
     * Abstract method for setting the fragment layout via BaseFragment
     *
     * @return the layout to inflate for the fragment
     */
    protected abstract int getLayoutResourceId();

    protected abstract String getName();

    @Override
    public void onPause() {
        Log.d("LIFECYCLE", "In " + getName() + " onPause()");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d("LIFECYCLE", "In " + getName() + " onResume()");
        super.onResume();
    }
}