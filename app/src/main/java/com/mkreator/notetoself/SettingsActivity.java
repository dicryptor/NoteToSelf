package com.mkreator.notetoself;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.ToggleButton;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;
    private boolean mSound;
    public static final int FAST = 0;
    public static final int SLOW = 1;
    public static final int NONE = 2;
    private int mAnimOption;
    private boolean mNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mPrefs = getSharedPreferences("Note To Self", MODE_PRIVATE);
        mEditor = mPrefs.edit();

        mSound = mPrefs.getBoolean("sound", true);
        CheckBox checkBoxSound = (CheckBox) findViewById(R.id.checkBoxSound);

        if (mSound) {
            checkBoxSound.setChecked(true);
        } else {
            checkBoxSound.setChecked(false);
        }

        checkBoxSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.i("sound=", "" + mSound);
                Log.i("is checked=", "" + b);

                // if true make it false, if false make it true
                mSound = ! mSound;
                mEditor.putBoolean("sound", mSound);
                mEditor.apply();
            }
        });

        mNotification = mPrefs.getBoolean("notification", true);
        Switch swNotification = (Switch) findViewById(R.id.swNotification);
        if(mNotification) {
            swNotification.setChecked(true);
        } else {
            swNotification.setChecked(false);
        }
        swNotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mNotification = ! mNotification;
                mEditor.putBoolean("notification", mNotification);
                mEditor.apply();
            }
        });

        mAnimOption =  mPrefs.getInt("anim option", FAST);

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.clearCheck();

        switch (mAnimOption){
            case FAST:
                radioGroup.check(R.id.radioFast);
                break;
            case SLOW:
                radioGroup.check(R.id.radioSlow);
                break;
            case NONE:
                radioGroup.check(R.id.radioNone);
                break;
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton rb = (RadioButton) radioGroup.findViewById(i);
                if (null != rb && i > -1) {
                    switch (rb.getId()){
                        case R.id.radioFast:
                            mAnimOption = FAST;
                            break;
                        case R.id.radioSlow:
                            mAnimOption = SLOW;
                            break;
                        case R.id.radioNone:
                            mAnimOption = NONE;
                            break;
                    }
                    // switch block end

                    mEditor.putInt("anim option", mAnimOption);
                    mEditor.apply();
                }

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        // save the settings
        mEditor.commit();
    }
}





















