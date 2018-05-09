package com.elementary.tasks.navigation.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.elementary.tasks.R;
import com.elementary.tasks.core.interfaces.RealmCallback;
import com.elementary.tasks.core.utils.CalendarUtils;
import com.elementary.tasks.core.utils.Constants;
import com.elementary.tasks.core.utils.DataLoader;
import com.elementary.tasks.core.utils.Dialogues;
import com.elementary.tasks.core.utils.RealmDb;
import com.elementary.tasks.core.utils.ReminderUtils;
import com.elementary.tasks.core.utils.ThemeUtil;
import com.elementary.tasks.core.views.FilterView;
import com.elementary.tasks.creators.CreateReminderActivity;
import com.elementary.tasks.databinding.FragmentTrashBinding;
import com.elementary.tasks.groups.GroupItem;
import com.elementary.tasks.reminder.DeleteFilesAsync;
import com.elementary.tasks.reminder.RecyclerListener;
import com.elementary.tasks.reminder.RemindersRecyclerAdapter;
import com.elementary.tasks.reminder.filters.FilterCallback;
import com.elementary.tasks.reminder.filters.ReminderFilterController;
import com.elementary.tasks.reminder.models.Reminder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
public class ArchiveFragment extends BaseNavigationFragment implements FilterCallback<Reminder> {

    private FragmentTrashBinding binding;
    private RecyclerView mRecyclerView;

    private RemindersRecyclerAdapter mAdapter;

    private List<String> mGroupsIds = new ArrayList<>();
    @NonNull
    private ReminderFilterController filterController = new ReminderFilterController(this);

    private SearchView mSearchView = null;
    private MenuItem mSearchMenu = null;

    private SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            if (mAdapter != null) filterController.setSearchValue(query);
            if (mSearchMenu != null) {
                mSearchMenu.collapseActionView();
            }
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (mAdapter != null) filterController.setSearchValue(newText);
            if (!getCallback().isFiltersVisible()) {
                showRemindersFilter();
            }
            return false;
        }
    };
    private SearchView.OnCloseListener mSearchCloseListener = () -> {
        refreshFilters();
        return false;
    };
    private RecyclerListener mEventListener = new RecyclerListener() {
        @Override
        public void onItemSwitched(int position, View view) {

        }

        @Override
        public void onItemClicked(int position, View view) {
            if (view.getId() == R.id.button_more) {
                showActionDialog(position, view);
            } else {
                Reminder reminder = mAdapter.getItem(position);
                if (reminder != null) editReminder(reminder.getUuId());
            }
        }

        @Override
        public void onItemLongClicked(int position, View view) {
        }
    };
    private RealmCallback<List<Reminder>> mLoadCallback = this::showData;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.archive_menu, menu);
        mSearchMenu = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        if (mSearchMenu != null) {
            mSearchView = (SearchView) mSearchMenu.getActionView();
        }
        if (mSearchView != null) {
            if (searchManager != null) {
                mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            }
            mSearchView.setOnQueryTextListener(queryTextListener);
            mSearchView.setOnCloseListener(mSearchCloseListener);
            mSearchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (!getCallback().isFiltersVisible()) {
                        showRemindersFilter();
                    }
                }
            });
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_all:
                deleteAll();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTrashBinding.inflate(inflater, container, false);
        initList();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getCallback() != null) {
            getCallback().onTitleChange(getString(R.string.trash));
            getCallback().onFragmentSelect(this);
            getCallback().setClick(null);
        }
        loadData();
    }

    private void editReminder(String uuId) {
        startActivity(new Intent(getContext(), CreateReminderActivity.class).putExtra(Constants.INTENT_ID, uuId));
    }

    private void deleteAll() {
        List<String> uids = RealmDb.getInstance().clearReminderTrash();
        new DeleteFilesAsync(getContext()).execute(uids.toArray(new String[uids.size()]));
        loadData();
        Toast.makeText(getContext(), getString(R.string.trash_cleared), Toast.LENGTH_SHORT).show();
        reloadView();
    }

    private void showData(List<Reminder> result) {
        filterController.setOriginal(result);
    }

    private void showActionDialog(int position, View view) {
        final String[] items = {getString(R.string.edit), getString(R.string.delete)};
        Dialogues.showPopup(getContext(), view, item -> {
            Reminder item1 = mAdapter.getItem(position);
            if (item1 == null) return;
            if (item == 0) {
                editReminder(item1.getUuId());
            }
            if (item == 1) {
                deleteReminder(item1);
                new DeleteFilesAsync(getContext()).execute(item1.getUuId());
                mAdapter.removeItem(position);
                Toast.makeText(getContext(), R.string.deleted, Toast.LENGTH_SHORT).show();
                reloadView();
            }
        }, items);
    }

    private void deleteReminder(Reminder reminder) {
        RealmDb.getInstance().deleteReminder(reminder.getUuId());
        CalendarUtils.deleteEvents(getContext(), reminder.getUuId());
        filterController.remove(reminder);
        refreshFilters();
    }

    private void refreshFilters() {
        if (getCallback().isFiltersVisible()) {
            showRemindersFilter();
        }
    }

    private void initList() {
        mRecyclerView = binding.recyclerView;
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new RemindersRecyclerAdapter(getContext());
        mAdapter.setEventListener(mEventListener);
        mAdapter.setEditable(false);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
    }

    private void loadData() {
        DataLoader.loadArchivedReminder(mLoadCallback);
    }

    private void showRemindersFilter() {
        List<FilterView.Filter> filters = new ArrayList<>();
        addGroupFilter(filters);
        addTypeFilter(filters);
        getCallback().addFilters(filters, true);
    }

    private void addTypeFilter(List<FilterView.Filter> filters) {
        List<Reminder> reminders = filterController.getOriginal();
        if (reminders.size() == 0) {
            return;
        }
        Set<Integer> types = new LinkedHashSet<>();
        for (Reminder reminder : reminders) {
            types.add(reminder.getType());
        }
        FilterView.Filter filter = new FilterView.Filter(new FilterView.FilterElementClick() {
            @Override
            public void onClick(View view, int id) {
                filterController.setTypeValue(id);
            }

            @Override
            public void onMultipleSelected(View view, List<Integer> ids) {

            }
        });
        filter.add(new FilterView.FilterElement(R.drawable.ic_bell_illustration, getString(R.string.all), 0, true));
        ThemeUtil util = ThemeUtil.getInstance(getContext());
        for (Integer integer : types) {
            filter.add(new FilterView.FilterElement(util.getReminderIllustration(integer), ReminderUtils.getType(getContext(), integer), integer));
        }
        if (filter.size() != 0) {
            filters.add(filter);
        }
    }

    private void addGroupFilter(List<FilterView.Filter> filters) {
        mGroupsIds = new ArrayList<>();
        FilterView.Filter filter = new FilterView.Filter(new FilterView.FilterElementClick() {
            @Override
            public void onClick(View view, int id) {
                if (id == 0) {
                    filterController.setGroupValue(null);
                } else {
                    filterController.setGroupValue(mGroupsIds.get(id - 1));
                }
            }

            @Override
            public void onMultipleSelected(View view, List<Integer> ids) {
                List<String> groups = new ArrayList<>();
                for (Integer i : ids) groups.add(mGroupsIds.get(i - 1));
                filterController.setGroupValues(groups);
            }
        });
        filter.add(new FilterView.FilterElement(R.drawable.ic_bell_illustration, getString(R.string.all), 0, true));
        List<GroupItem> groups = RealmDb.getInstance().getAllGroups();
        ThemeUtil util = ThemeUtil.getInstance(getContext());
        for (int i = 0; i < groups.size(); i++) {
            GroupItem item = groups.get(i);
            filter.add(new FilterView.FilterElement(util.getCategoryIndicator(item.getColor()), item.getTitle(), i + 1));
            mGroupsIds.add(item.getUuId());
        }
        filters.add(filter);
    }

    private void reloadView() {
        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            mRecyclerView.setVisibility(View.VISIBLE);
            binding.emptyItem.setVisibility(View.GONE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            binding.emptyItem.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onChanged(@NonNull List<Reminder> result) {
        mAdapter.setData(result);
        mRecyclerView.smoothScrollToPosition(0);
        reloadView();
    }
}
