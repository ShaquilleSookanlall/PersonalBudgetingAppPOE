package com.example.personalbudgetingapp

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.personalbudgetingapp.databinding.ItemEntryBinding

class EntryAdapter(
    private var entries: List<ExpenseEntry>,
    private val categories: List<Category>,
    private val onEditClicked: (ExpenseEntry) -> Unit,
    private val onDeleteClicked: (ExpenseEntry) -> Unit
) : RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {

    inner class EntryViewHolder(val binding: ItemEntryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        val categoryName = categories.find { it.id == entry.categoryId.toString() }?.name ?: "Unknown"

        holder.binding.tvDescription.text = entry.description
        holder.binding.tvAmount.text = "R${entry.amount}"
        holder.binding.tvCategory.text = categoryName
        holder.binding.tvDate.text = entry.date

        if (!entry.photoUri.isNullOrEmpty()) {
            holder.binding.ivEntryPhoto.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(entry.photoUri)
                .into(holder.binding.ivEntryPhoto)
        } else {
            holder.binding.ivEntryPhoto.visibility = View.GONE
        }

        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Choose Action")
                .setItems(arrayOf("Edit", "Delete")) { _, which ->
                    when (which) {
                        0 -> onEditClicked(entry)
                        1 -> onDeleteClicked(entry)
                    }
                }
                .show()
            true
        }
    }

    override fun getItemCount() = entries.size

    fun updateData(newData: List<ExpenseEntry>) {
        entries = newData
        notifyDataSetChanged()
    }
}
