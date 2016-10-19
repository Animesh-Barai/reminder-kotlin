package com.elementary.tasks.core;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.ThemeUtil;

public abstract class ThemedActivity extends AppCompatActivity {

    protected ThemeUtil themeUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeUtil = ThemeUtil.getInstance(this);
        setTheme(themeUtil.getStyle());
        setRequestedOrientation(themeUtil.getRequestOrientation());
        if (Module.isLollipop()) {
            getWindow().setStatusBarColor(themeUtil.getColor(themeUtil.colorPrimaryDark()));
        }
    }
}