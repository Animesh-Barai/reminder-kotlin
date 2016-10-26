package com.elementary.tasks.creators.fragments;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.SeekBar;

import com.elementary.tasks.R;
import com.elementary.tasks.core.views.roboto.RoboTextView;
import com.elementary.tasks.databinding.DialogWithSeekAndTitleBinding;

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

abstract class RepeatableTypeFragment extends TypeFragment {

    protected void changeLimit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.repeat_limit);
        DialogWithSeekAndTitleBinding b = DialogWithSeekAndTitleBinding.inflate(LayoutInflater.from(mContext));
        b.seekBar.setMax(366);
        b.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setRepeatTitle(b.titleView, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        b.seekBar.setProgress(mInterface.getRepeatLimit() != -1 ? mInterface.getRepeatLimit() : 0);
        setRepeatTitle(b.titleView, mInterface.getRepeatLimit());
        builder.setView(b.getRoot());
        builder.setPositiveButton(R.string.ok, (dialog, which) -> saveLimit(b.seekBar.getProgress()));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void setRepeatTitle(RoboTextView textView, int progress) {
        if (progress <= 0) {
            textView.setText(getString(R.string.no_limits));
        } else if (progress == 1) {
            textView.setText(R.string.once);
        } else {
            textView.setText(progress + " " + getString(R.string.times));
        }
    }

    private void saveLimit(int progress) {
        int repeatLimit = progress;
        if (progress == 0) repeatLimit = -1;
        mInterface.setRepeatLimit(repeatLimit);
    }
}