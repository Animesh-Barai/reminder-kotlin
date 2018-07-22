package com.elementary.tasks.notes.list

import android.app.ProgressDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.elementary.tasks.R
import com.elementary.tasks.core.data.models.Note
import com.elementary.tasks.core.interfaces.ActionsListener
import com.elementary.tasks.core.utils.*
import com.elementary.tasks.core.viewModels.notes.NotesViewModel
import com.elementary.tasks.navigation.fragments.BaseNavigationFragment
import com.elementary.tasks.notes.create.CreateNoteActivity
import com.elementary.tasks.notes.preview.NotePreviewActivity
import com.elementary.tasks.notes.work.SyncNotes
import com.elementary.tasks.reminder.lists.filters.FilterCallback
import kotlinx.android.synthetic.main.fragment_notes.*
import java.io.File

/**
 * Copyright 2016 Nazar Suhovich
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class NotesFragment : BaseNavigationFragment(), FilterCallback<Note> {

    private lateinit var viewModel: NotesViewModel

    private var mAdapter: NotesRecyclerAdapter = NotesRecyclerAdapter()
    private var enableGrid = false
    private var mProgress: ProgressDialog? = null

    private val filterController = NoteFilterController(this)

    private var mSearchView: SearchView? = null
    private var mSearchMenu: MenuItem? = null

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean {
            filterController.setSearchValue(query)
            if (mSearchMenu != null) {
                mSearchMenu?.collapseActionView()
            }
            return false
        }

        override fun onQueryTextChange(newText: String): Boolean {
            filterController.setSearchValue(newText)
            return false
        }
    }

    private val mCloseListener = {
        filterController.setSearchValue("")
        true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.notes_menu, menu)
        val item = menu?.findItem(R.id.action_list)
        if (item != null) {
            item.setIcon(if (!enableGrid) R.drawable.ic_view_quilt_black_24dp else R.drawable.ic_view_list_white_24dp)
            item.title = if (!enableGrid) getString(R.string.grid_view) else getString(R.string.list_view)
        }
        if (viewModel.notes.value != null && viewModel.notes.value!!.isNotEmpty()) {
            menu?.add(Menu.NONE, MENU_ITEM_DELETE, 100, getString(R.string.delete_all))
        }
        mSearchMenu = menu?.findItem(R.id.action_search)
        val searchManager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager?
        if (mSearchMenu != null) {
            mSearchView = mSearchMenu?.actionView as SearchView?
        }
        if (mSearchView != null) {
            if (searchManager != null) {
                mSearchView?.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))
            }
            mSearchView?.setOnQueryTextListener(queryTextListener)
            mSearchView?.setOnCloseListener(mCloseListener)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun shareNote(note: Note) {
        showProgress()
        Thread { BackupTool.getInstance().createNote(note, object : BackupTool.CreateCallback {
            override fun onReady(file: File?) {
                if (file != null) sendNote(note, file)
            }
        }) }.start()
    }

    private fun sendNote(note: Note, file: File) {
        hideProgress()
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(context, getString(R.string.error_sending), Toast.LENGTH_SHORT).show()
            return
        }
        TelephonyUtil.sendNote(file, context!!, note.summary)
    }

    private fun hideProgress() {
        if (mProgress != null && mProgress!!.isShowing) {
            mProgress!!.dismiss()
        }
    }

    private fun showProgress() {
        mProgress = ProgressDialog.show(context, null, getString(R.string.please_wait), true, false)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_sync -> SyncNotes(context!!, null).execute()
            R.id.action_order -> showDialog()
            MENU_ITEM_DELETE -> deleteDialog()
            R.id.action_list -> {
                enableGrid = !enableGrid
                prefs.isNotesGridEnabled = enableGrid
                mAdapter.notifyDataSetChanged()
                activity!!.invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initList()
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(NotesViewModel::class.java)
        viewModel.notes.observe(this, Observer{ list ->
            if (list != null) {
                filterController.original = list
            }
        })
    }

    private fun initList() {
        var layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
        enableGrid = prefs.isNotesGridEnabled
        if (enableGrid) {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }
        recyclerView.layoutManager = layoutManager
        mAdapter = NotesRecyclerAdapter()
        mAdapter.actionsListener = object : ActionsListener<Note> {
            override fun onAction(view: View, position: Int, t: Note?, actions: ListActions) {
                when (actions) {
                    ListActions.OPEN -> if (t != null) previewNote(t.key, view)
                    ListActions.MORE -> if (t != null) showMore(view, t)
                }
            }
        }
        recyclerView.adapter = mAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
        refreshView()
    }

    private fun showMore(view: View, note: Note) {
        var showIn = getString(R.string.show_in_status_bar)
        showIn = showIn.substring(0, showIn.length - 1)
        val items = arrayOf(getString(R.string.open), getString(R.string.share), showIn, getString(R.string.change_color), getString(R.string.edit), getString(R.string.delete))
        Dialogues.showLCAM(context!!, { item ->
            when (item) {
                0 -> previewNote(note.key, view)
                1 -> shareNote(note)
                2 -> showInStatusBar(note)
                3 -> selectColor(note)
                4 -> context!!.startActivity(Intent(context, CreateNoteActivity::class.java)
                        .putExtra(Constants.INTENT_ID, note.key))
                5 -> viewModel.deleteNote(note)
            }
        }, *items)
    }

    private fun showDialog() {
        val items = arrayOf<CharSequence>(getString(R.string.by_date_az), getString(R.string.by_date_za), getString(R.string.name_az), getString(R.string.name_za))
        val builder = Dialogues.getDialog(context!!)
        builder.setTitle(getString(R.string.order))
        builder.setItems(items) { dialog, which ->
            var value = ""
            when (which) {
                0 -> value = Constants.ORDER_DATE_A_Z
                1 -> value = Constants.ORDER_DATE_Z_A
                2 -> value = Constants.ORDER_NAME_A_Z
                3 -> value = Constants.ORDER_NAME_Z_A
            }
            prefs.noteOrder = value
            dialog.dismiss()
            viewModel.reload()
        }
        val alert = builder.create()
        alert.show()
    }

    override fun onResume() {
        super.onResume()
        if (callback != null) {
            callback?.onTitleChange(getString(R.string.notes))
            callback?.onFragmentSelect(this)
            callback?.setClick(View.OnClickListener { startActivity(Intent(context, CreateNoteActivity::class.java)) })
            callback?.onScrollChanged(recyclerView)
        }
    }

    private fun deleteDialog() {
        val builder = Dialogues.getDialog(context!!)
        builder.setCancelable(true)
        builder.setMessage(R.string.delete_all_notes)
        builder.setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
        builder.setPositiveButton(getString(R.string.yes)) { dialog, _ ->
            dialog.dismiss()
            deleteAll()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun deleteAll() {
        val notes = viewModel.notes.value
        if (notes != null) {
            viewModel.deleteAll(notes)
        }
    }

    private fun previewNote(id: String?, view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val intent = Intent(context, NotePreviewActivity::class.java)
            intent.putExtra(Constants.INTENT_ID, id)
            val transitionName = "image"
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity!!, view, transitionName)
            context!!.startActivity(intent, options.toBundle())
        } else {
            context!!.startActivity(Intent(context, NotePreviewActivity::class.java)
                    .putExtra(Constants.INTENT_ID, id))
        }
    }

    private fun showInStatusBar(note: Note?) {
        if (note != null) {
            Notifier(context!!).showNoteNotification(note)
        }
    }

    private fun selectColor(note: Note) {
        var items = arrayOf(getString(R.string.red), getString(R.string.purple), getString(R.string.green), getString(R.string.green_light), getString(R.string.blue), getString(R.string.blue_light), getString(R.string.yellow), getString(R.string.orange), getString(R.string.cyan), getString(R.string.pink), getString(R.string.teal), getString(R.string.amber))
        if (Module.isPro) {
            items = arrayOf(getString(R.string.red), getString(R.string.purple), getString(R.string.green), getString(R.string.green_light), getString(R.string.blue), getString(R.string.blue_light), getString(R.string.yellow), getString(R.string.orange), getString(R.string.cyan), getString(R.string.pink), getString(R.string.teal), getString(R.string.amber), getString(R.string.dark_purple), getString(R.string.dark_orange), getString(R.string.lime), getString(R.string.indigo))
        }
        Dialogues.showLCAM(context!!, { item ->
            note.color = item
            viewModel.saveNote(note)
        }, *items)
    }

    private fun refreshView() {
        if (mAdapter.itemCount == 0) {
            emptyItem.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyItem.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onChanged(result: List<Note>) {
        mAdapter.data = result
        recyclerView.smoothScrollToPosition(0)
        refreshView()
    }

    companion object {
        const val MENU_ITEM_DELETE = 12
    }
}