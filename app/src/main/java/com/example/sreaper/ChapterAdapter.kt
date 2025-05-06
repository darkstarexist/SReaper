package com.example.sreaper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChapterAdapter(
    private val chapterList: List<Chapter>,  // List<Chapter> instead of List<String>
    private val onItemClick: (Chapter) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    inner class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapterTitle: TextView = itemView.findViewById(R.id.FolderName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapterList[position]
        holder.chapterTitle.text = chapter.title  // Accessing chapter title
        holder.itemView.setOnClickListener { onItemClick(chapter) }
    }

    override fun getItemCount() = chapterList.size
}
