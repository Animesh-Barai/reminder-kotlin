package com.elementary.tasks.core;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.elementary.tasks.R;
import com.elementary.tasks.ReminderApp;
import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.Prefs;
import com.elementary.tasks.core.utils.SuperUtil;
import com.elementary.tasks.core.utils.ThemeUtil;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public abstract class ThemedActivity extends AppCompatActivity {

    private ThemeUtil themeUtil;
    private Prefs mPrefs;
    private Tracker mTracker;

    protected Prefs getPrefs() {
        return mPrefs;
    }

    protected ThemeUtil getThemeUtil() {
        return themeUtil;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.enter_slide_in, R.anim.enter_slide_out);
        mPrefs = Prefs.getInstance(this);
        themeUtil = ThemeUtil.getInstance(this);
        setTheme(themeUtil.getStyle());
        if (Module.isLollipop()) {
            getWindow().setStatusBarColor(themeUtil.getColor(themeUtil.colorPrimaryDark()));
        }
        initTracker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            overridePendingTransition(R.anim.exit_left_to_right, R.anim.exit_right_to_left);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendStats();
    }

    private void sendStats() {
        if (SuperUtil.isGooglePlayServicesAvailable(this) && getStats() != null) {
            mTracker.setScreenName(getStats());
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    protected String getStats() {
        return this.getClass().getName();
    }

    private void initTracker() {
        if (SuperUtil.isGooglePlayServicesAvailable(this)) {
            ReminderApp application = (ReminderApp) getApplication();
            mTracker = application.getDefaultTracker();
        }
    }
}
