package com.unlone.app.model

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.unlone.app.R
import com.unlone.app.databinding.CategoryRowPostsListItemBinding
import com.unlone.app.databinding.ListItemTipsBinding
import com.unlone.app.ui.lounge.ChildPostsAdapter
import com.unlone.app.ui.lounge.HomeParentAdapter

class HomeTipsViewHolder private constructor(
    val binding: ListItemTipsBinding,
    private val lifecycleOwner: LifecycleOwner,
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: HomeUiModel.Tips) {
        binding.tips = item
        binding.lifecycleOwner = lifecycleOwner
        binding.actionTv.setOnClickListener {
            item.actionOnClick()
        }
        binding.executePendingBindings()
    }

    companion object {
        fun from(
            parent: ViewGroup,
            lifecycleOwner: LifecycleOwner,
        ): HomeTipsViewHolder {
            Log.d("TAG", "view holder creating...")
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemTipsBinding.inflate(layoutInflater, parent, false)
            return HomeTipsViewHolder(binding, lifecycleOwner)
        }
    }
}