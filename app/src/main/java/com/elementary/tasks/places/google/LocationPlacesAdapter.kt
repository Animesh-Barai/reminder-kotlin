package com.elementary.tasks.places.google

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elementary.tasks.R
import com.elementary.tasks.core.data.models.Reminder
import com.elementary.tasks.core.interfaces.ActionsListener
import com.elementary.tasks.core.utils.ListActions
import com.elementary.tasks.places.list.PlacesRecyclerAdapter
import kotlinx.android.synthetic.main.list_item_location.view.*
import java.util.*

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
class LocationPlacesAdapter : RecyclerView.Adapter<LocationPlacesAdapter.ViewHolder>() {

    private val mDataList = ArrayList<Reminder>()
    var actionsListener: ActionsListener<Reminder>? = null

    fun setData(list: List<Reminder>) {
        this.mDataList.clear()
        this.mDataList.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        fun bind(item: Reminder) {
            val place = item.places[0]
            var name = place.name
            if (item.places.size > 1) {
                name = item.summary + " (" + item.places.size + ")"
            }
            itemView.textView.text = name
            PlacesRecyclerAdapter.loadMarker(itemView.markerImage, place.marker)
        }

        init {
            itemView.setOnClickListener { view ->
                actionsListener?.onAction(view, adapterPosition, getItem(adapterPosition), ListActions.OPEN)
            }
            itemView.setOnLongClickListener { view ->
                actionsListener?.onAction(view, adapterPosition, getItem(adapterPosition), ListActions.MORE)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_location, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mDataList[position])
    }

    fun getItem(position: Int): Reminder {
        return mDataList[position]
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }
}