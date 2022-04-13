package ca.quadrexium.velonathea.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;

public abstract class BaseDialogFragment extends DialogFragment {

    protected String fragmentName;

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

    /**
     * Abstract method for setting the fragment layout via BaseFragment
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