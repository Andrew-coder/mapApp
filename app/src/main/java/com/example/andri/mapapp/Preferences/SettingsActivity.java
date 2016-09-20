package com.example.andri.mapapp.Preferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.example.andri.mapapp.R;


public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.preference);
        addPreferencesFromResource(R.xml.preference);
    }
}
