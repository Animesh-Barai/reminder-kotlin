package com.elementary.tasks.core.app_widgets.notes;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.elementary.tasks.R;
import com.elementary.tasks.core.utils.Constants;
import com.elementary.tasks.core.utils.RealmDb;
import com.elementary.tasks.core.utils.ThemeUtil;
import com.elementary.tasks.notes.NoteImage;
import com.elementary.tasks.notes.NoteItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2015 Nazar Suhovich
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

public class NotesFactory implements RemoteViewsService.RemoteViewsFactory {

    private List<NoteItem> notes = new ArrayList<>();
    private Context mContext;
    private ThemeUtil themeUtil;

    NotesFactory(Context ctx, Intent intent) {
        mContext = ctx;
        themeUtil = new ThemeUtil(ctx);
        int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        notes = RealmDb.getInstance().getAllNotes(null);
    }

    @Override
    public void onDestroy() {
        notes = null;
    }

    @Override
    public int getCount() {
        return notes.size();
    }

    @Override
    public RemoteViews getViewAt(int i) {
        RemoteViews rView = new RemoteViews(mContext.getPackageName(), R.layout.list_item_note_widget);
        NoteItem note = notes.get(i);
        rView.setInt(R.id.noteBackground, "setBackgroundColor", themeUtil.getNoteLightColor(note.getColor()));

        if (note.getImages() != null && note.getImages().size() > 0){
            NoteImage image = note.getImages().get(0);
            Bitmap photo = BitmapFactory.decodeByteArray(image.getImage(), 0, image.getImage().length);
            if (photo != null){
                rView.setImageViewBitmap(R.id.noteImage, photo);
                rView.setViewVisibility(R.id.noteImage, View.VISIBLE);
            } else rView.setViewVisibility(R.id.noteImage, View.GONE);
        } else rView.setViewVisibility(R.id.noteImage, View.GONE);
        rView.setTextViewText(R.id.note, note.getSummary());
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(Constants.INTENT_ID, note.getKey());
        rView.setOnClickFillInIntent(R.id.note, fillInIntent);
        rView.setOnClickFillInIntent(R.id.noteImage, fillInIntent);
        rView.setOnClickFillInIntent(R.id.noteBackground, fillInIntent);
        return rView;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}