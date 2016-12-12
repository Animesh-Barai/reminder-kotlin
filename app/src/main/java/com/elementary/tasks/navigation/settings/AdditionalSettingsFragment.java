package com.elementary.tasks.navigation.settings;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.elementary.tasks.R;
import com.elementary.tasks.core.utils.Permissions;
import com.elementary.tasks.core.utils.Prefs;
import com.elementary.tasks.core.views.PrefsView;
import com.elementary.tasks.databinding.DialogWithSeekAndTitleBinding;
import com.elementary.tasks.databinding.FragmentSettingsAdditionalBinding;
import com.elementary.tasks.navigation.settings.additional.TemplatesFragment;

/**
 * Copyright 2016 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class AdditionalSettingsFragment extends BaseSettingsFragment {

    private static final int MISSED = 107;
    private static final int QUICK_SMS = 108;
    private static final int FOLLOW = 109;

    private FragmentSettingsAdditionalBinding binding;
    private PrefsView mMissedPrefs;
    private PrefsView mQuickSmsPrefs;
    private View.OnClickListener mMissedClick = view -> changeMissedPrefs();
    private View.OnClickListener mMissedTimeClick = view -> showTimePickerDialog();
    private View.OnClickListener mQuickSmsClick = view -> changeQuickSmsPrefs();
    private View.OnClickListener mFollowClick = view -> changeFollowPrefs();
    private View.OnClickListener mMessagesClick = view -> replaceFragment(new TemplatesFragment(), getString(R.string.messages));

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsAdditionalBinding.inflate(inflater, container, false);
        initMissedPrefs();
        initMissedTimePrefs();
        initQuickSmsPrefs();
        initMessagesPrefs();
        binding.followReminderPrefs.setOnClickListener(mFollowClick);
        binding.followReminderPrefs.setChecked(Prefs.getInstance(mContext).isFollowReminderEnabled());
        return binding.getRoot();
    }

    private void initMessagesPrefs() {
        PrefsView mMessagesPrefs = binding.templatesPrefs;
        mMessagesPrefs.setOnClickListener(mMessagesClick);
        mMessagesPrefs.setDependentView(mQuickSmsPrefs);
    }

    private void initQuickSmsPrefs() {
        mQuickSmsPrefs = binding.quickSMSPrefs;
        mQuickSmsPrefs.setOnClickListener(mQuickSmsClick);
        mQuickSmsPrefs.setChecked(Prefs.getInstance(mContext).isQuickSmsEnabled());
    }

    private void initMissedTimePrefs() {
        PrefsView mMissedTimePrefs = binding.missedTimePrefs;
        mMissedTimePrefs.setOnClickListener(mMissedTimeClick);
        mMissedTimePrefs.setDependentView(mMissedPrefs);
    }

    private void initMissedPrefs() {
        mMissedPrefs = binding.missedPrefs;
        mMissedPrefs.setOnClickListener(mMissedClick);
        mMissedPrefs.setChecked(Prefs.getInstance(mContext).isMissedReminderEnabled());
    }

    private void changeFollowPrefs() {
        if (!Permissions.checkPermission(getActivity(), Permissions.READ_PHONE_STATE)) {
            Permissions.requestPermission(getActivity(), FOLLOW, Permissions.READ_PHONE_STATE);
            return;
        }
        boolean isChecked = binding.followReminderPrefs.isChecked();
        binding.followReminderPrefs.setChecked(!isChecked);
        Prefs.getInstance(mContext).setFollowReminderEnabled(!isChecked);
    }

    private void changeMissedPrefs() {
        if (!Permissions.checkPermission(getActivity(), Permissions.READ_PHONE_STATE)) {
            Permissions.requestPermission(getActivity(), MISSED, Permissions.READ_PHONE_STATE);
            return;
        }
        boolean isChecked = mMissedPrefs.isChecked();
        mMissedPrefs.setChecked(!isChecked);
        Prefs.getInstance(mContext).setMissedReminderEnabled(!isChecked);
    }

    private void changeQuickSmsPrefs() {
        if (!Permissions.checkPermission(getActivity(), Permissions.READ_PHONE_STATE)) {
            Permissions.requestPermission(getActivity(), QUICK_SMS, Permissions.READ_PHONE_STATE);
            return;
        }
        boolean isChecked = mQuickSmsPrefs.isChecked();
        mQuickSmsPrefs.setChecked(!isChecked);
        Prefs.getInstance(mContext).setQuickSmsEnabled(!isChecked);
    }

    private void showTimePickerDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.interval);
        DialogWithSeekAndTitleBinding b = DialogWithSeekAndTitleBinding.inflate(LayoutInflater.from(mContext));
        b.seekBar.setMax(60);
        b.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                b.titleView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        int time = Prefs.getInstance(mContext).getMissedReminderTime();
        b.seekBar.setProgress(time);
        b.titleView.setText(String.valueOf(time));
        builder.setView(b.getRoot());
        builder.setPositiveButton(R.string.ok, (dialog, which) -> Prefs.getInstance(mContext).setMissedReminderTime(b.seekBar.getProgress()));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCallback != null) {
            mCallback.onTitleChange(getString(R.string.additional));
            mCallback.onFragmentSelect(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MISSED:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    changeMissedPrefs();
                }
                break;
            case QUICK_SMS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    changeQuickSmsPrefs();
                }
                break;
            case FOLLOW:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    changeFollowPrefs();
                }
                break;
        }
    }
}