package com.esmanureral.pupilicahackathon.ui.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.esmanureral.pupilicahackathon.data.model.WeeklyPlanItem
import com.esmanureral.pupilicahackathon.databinding.ItemWeeklyPlanBinding

class WeeklyPlanAdapter(
    private val planItems: List<WeeklyPlanItem>
) : RecyclerView.Adapter<WeeklyPlanAdapter.WeeklyPlanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyPlanViewHolder {
        val binding = ItemWeeklyPlanBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return WeeklyPlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeeklyPlanViewHolder, position: Int) {
        val item = planItems[position]
        holder.showItem(item)
    }

    override fun getItemCount(): Int = planItems.size

    class WeeklyPlanViewHolder(private val binding: ItemWeeklyPlanBinding) : RecyclerView.ViewHolder(binding.root) {

        fun showItem(item: WeeklyPlanItem) {
            binding.tvDay.text = item.day
            binding.tvTask.text = item.task
        }
    }
}
