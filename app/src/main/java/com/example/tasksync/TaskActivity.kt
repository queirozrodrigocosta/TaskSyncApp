package com.example.tasksync

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tasksync.models.Task
import com.example.tasksync.utils.SessionManager
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.tasksync.databinding.ActivityTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskBinding
    private lateinit var database: DatabaseReference
    private lateinit var sessionManager: SessionManager
    private lateinit var tasksRef: DatabaseReference
    
    private var currentTask: Task? = null
    private var isEdit = false
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        database = Firebase.database.reference
        sessionManager = SessionManager(this)

        // Verificar se usuário está logado
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Referência para as tarefas do usuário
        val userId = sessionManager.getUserId()
        tasksRef = database.child("tasks").child(userId ?: "")

        // Verificar se é edição ou nova tarefa
        isEdit = intent.getBooleanExtra("is_edit", false)
        val taskId = intent.getStringExtra("task_id")

        setupToolbar()
        setupClickListeners()
        setupDateAndTimePickers()

        if (isEdit && taskId != null) {
            loadTask(taskId)
        } else {
            setupNewTask()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = if (isEdit) getString(R.string.edit_task_title) else getString(R.string.add_task_title)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveTask()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        binding.etTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun setupDateAndTimePickers() {
        // Configurar data atual como padrão
        binding.etDate.setText(dateFormat.format(calendar.time))
        binding.etTime.setText(timeFormat.format(calendar.time))
    }

    private fun setupNewTask() {
        binding.btnDelete.visibility = android.view.View.GONE
    }

    private fun loadTask(taskId: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE

        tasksRef.child(taskId).get()
            .addOnSuccessListener { snapshot ->
                val taskMap = snapshot.value as? Map<String, Any>
                if (taskMap != null) {
                    currentTask = Task.fromMap(taskMap)
                    populateFields()
                }
                binding.progressBar.visibility = android.view.View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar tarefa", Toast.LENGTH_SHORT).show()
                finish()
                binding.progressBar.visibility = android.view.View.GONE
            }
    }

    private fun populateFields() {
        currentTask?.let { task ->
            binding.etTitle.setText(task.title)
            binding.etDescription.setText(task.description)
            binding.etDate.setText(task.date)
            binding.etTime.setText(task.time)
            binding.btnDelete.visibility = android.view.View.VISIBLE
        }
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            binding.etDate.setText(dateFormat.format(calendar.time))
        }

        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            binding.etTime.setText(timeFormat.format(calendar.time))
        }

        TimePickerDialog(
            this,
            timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun saveTask() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val time = binding.etTime.text.toString().trim()

        if (validateForm(title, description, date, time)) {
            binding.progressBar.visibility = android.view.View.VISIBLE

            val userId = sessionManager.getUserId() ?: return

            if (isEdit && currentTask != null) {
                // Editar tarefa existente
                val updatedTask = currentTask!!.copy(
                    title = title,
                    description = description,
                    date = date,
                    time = time,
                    updatedAt = System.currentTimeMillis()
                )

                tasksRef.child(currentTask!!.id).setValue(updatedTask.toMap())
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.success_task_saved), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao atualizar tarefa", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = android.view.View.GONE
                    }
            } else {
                // Criar nova tarefa
                val newTask = Task(
                    title = title,
                    description = description,
                    date = date,
                    time = time,
                    userId = userId
                )

                val taskId = tasksRef.push().key ?: return
                val taskWithId = newTask.copy(id = taskId)

                tasksRef.child(taskId).setValue(taskWithId.toMap())
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.success_task_saved), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao salvar tarefa", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = android.view.View.GONE
                    }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Excluir Tarefa")
            .setMessage("Tem certeza que deseja excluir esta tarefa?")
            .setPositiveButton("Excluir") { _, _ ->
                deleteTask()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteTask() {
        currentTask?.let { task ->
            binding.progressBar.visibility = android.view.View.VISIBLE

            tasksRef.child(task.id).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.success_task_deleted), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao excluir tarefa", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = android.view.View.GONE
                }
        }
    }

    private fun validateForm(title: String, description: String, date: String, time: String): Boolean {
        var valid = true

        if (TextUtils.isEmpty(title)) {
            binding.tilTitle.error = getString(R.string.error_empty_fields)
            valid = false
        } else {
            binding.tilTitle.error = null
        }

        if (TextUtils.isEmpty(description)) {
            binding.tilDescription.error = getString(R.string.error_empty_fields)
            valid = false
        } else {
            binding.tilDescription.error = null
        }

        if (TextUtils.isEmpty(date)) {
            binding.tilDate.error = getString(R.string.error_empty_fields)
            valid = false
        } else {
            binding.tilDate.error = null
        }

        if (TextUtils.isEmpty(time)) {
            binding.tilTime.error = getString(R.string.error_empty_fields)
            valid = false
        } else {
            binding.tilTime.error = null
        }

        return valid
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
