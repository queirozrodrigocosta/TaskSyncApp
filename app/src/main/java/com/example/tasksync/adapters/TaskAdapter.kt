package com.example.tasksync.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.tasksync.R
import com.example.tasksync.models.Task
import com.example.tasksync.databinding.ItemTaskBinding

class TaskAdapter(
    private val context: Context,
    var tasks: List<Task>,
    private val onItemClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onCompleteToggle: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(task: Task) {
            binding.apply {
                tvTitle.text = task.title
                tvDescription.text = task.description
                tvDate.text = task.date
                tvTime.text = task.time
                
                // Marcar checkbox como concluída
                cbCompleted.isChecked = task.completed
                
                // Aplicar estilo de texto para tarefas concluídas
                if (task.completed) {
                    tvTitle.alpha = 0.6f
                    tvDescription.alpha = 0.6f
                    tvTitle.paintFlags = tvTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    tvDescription.paintFlags = tvDescription.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    tvTitle.alpha = 1.0f
                    tvDescription.alpha = 1.0f
                    tvTitle.paintFlags = tvTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    tvDescription.paintFlags = tvDescription.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                
                // Click listener para o item inteiro
                root.setOnClickListener {
                    onItemClick(task)
                }
                
                // Click listener para o checkbox
                cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                    onCompleteToggle(task, isChecked)
                }
                
                // Click listener para o menu de opções
                ivMenu.setOnClickListener { view ->
                    showPopupMenu(view, task)
                }
            }
        }
        
        private fun showPopupMenu(view: View, task: Task) {
            val popup = PopupMenu(context, view)
            popup.menuInflater.inflate(R.menu.task_menu, popup.menu)
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onEditClick(task)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(task)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}
