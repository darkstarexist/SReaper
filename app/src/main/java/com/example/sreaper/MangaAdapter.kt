package com.example.sreaper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MangaAdapter(
    private val mangaList: List<String>,  // Now using List<Manga>
    private val onItemClick: (String) -> Unit // Accepts Manga object
) : RecyclerView.Adapter<MangaAdapter.MangaViewHolder>() {

    inner class MangaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mangaName: TextView = itemView.findViewById(R.id.FolderName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return MangaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        val manga = mangaList[position] // This is a Manga object now
        holder.mangaName.text = manga.replace("_"," ") // Use the name field
        holder.itemView.setOnClickListener { onItemClick(manga) } // Pass the whole Manga object when clicked
    }

    override fun getItemCount() = mangaList.size
}
