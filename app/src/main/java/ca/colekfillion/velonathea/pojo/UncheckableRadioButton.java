package ca.colekfillion.velonathea.pojo;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioGroup;

import androidx.appcompat.widget.AppCompatRadioButton;

/**
 * An extension of AppCompatRadioButton that allows for radio buttons to be unchecked.
 */
public class UncheckableRadioButton extends AppCompatRadioButton {

    public UncheckableRadioButton(Context context) {
        super(context);
    }

    public UncheckableRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UncheckableRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void toggle() {
        //If the button is already checked and in a radiogroup, clear the checked buttons
        if (isChecked()) {
            if (getParent() != null && getParent() instanceof RadioGroup) {
                ((RadioGroup) getParent()).clearCheck();
            }
        } else {
            super.toggle();
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return UncheckableRadioButton.class.getName();
    }
}
