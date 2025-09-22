package com.esmanureral.pupilicahackathon.presentation.feature.reminder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.domain.model.WeeklyPlanItem
import com.esmanureral.pupilicahackathon.databinding.ItemWeeklyPlanBinding

class WeeklyPlanAdapter : RecyclerView.Adapter<WeeklyPlanAdapter.WeeklyPlanViewHolder>() {

    private var planItems: List<WeeklyPlanItem> = emptyList()

    fun updateData(newPlanItems: List<WeeklyPlanItem>) {
        planItems = newPlanItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyPlanViewHolder {
        val binding = ItemWeeklyPlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WeeklyPlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeeklyPlanViewHolder, position: Int) {
        android.util.Log.d(
            "WeeklyPlanAdapter",
            "onBindViewHolder: position=$position, itemCount=${planItems.size}"
        )
        val item = planItems[position]
        holder.showItem(item)
    }

    override fun getItemCount(): Int = planItems.size

    class WeeklyPlanViewHolder(private val binding: ItemWeeklyPlanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun showItem(item: WeeklyPlanItem) {
            binding.tvDay.text = item.day
            binding.tvTask.text = item.task

            val context = binding.root.context
            val colorRes = when (item.day) {
                context.getString(R.string.day_monday) -> R.color.light_blue
                context.getString(R.string.day_tuesday) -> R.color.light_orange
                context.getString(R.string.day_wednesday) -> R.color.light_red
                context.getString(R.string.day_thursday) -> R.color.light_green
                context.getString(R.string.day_friday) -> R.color.light_blue
                context.getString(R.string.day_saturday) -> R.color.light_orange
                context.getString(R.string.day_sunday) -> R.color.light_red
                else -> R.color.white
            }

            binding.root.setCardBackgroundColor(context.getColor(colorRes))

        }
    }
}