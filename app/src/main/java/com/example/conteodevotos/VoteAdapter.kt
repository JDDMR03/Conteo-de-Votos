package com.example.conteodevotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import formatServerDateToLocalTime

class VoteAdapter(private val onClick: (Vote) -> Unit) :
    ListAdapter<Vote, VoteAdapter.VoteViewHolder>(DiffCallback()) {

    class VoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.tvTime)
    }

    class DiffCallback : DiffUtil.ItemCallback<Vote>() {
        override fun areItemsTheSame(oldItem: Vote, newItem: Vote) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Vote, newItem: Vote) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vote, parent, false)
        return VoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoteViewHolder, position: Int) {
        val vote = getItem(position)
        holder.timeText.text = "Conteo de ${vote.time.formatServerDateToLocalTime()}"
        holder.itemView.setOnClickListener { onClick(vote) }
    }
}