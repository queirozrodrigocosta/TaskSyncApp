package com.example.tasksync

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasksync.adapters.TaskAdapter
import com.example.tasksync.models.Task
import com.example.tasksync.sync.TaskSyncManager
import com.example.tasksync.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.tasksync.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sessionManager: SessionManager
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var tasksRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        auth = Firebase.auth
        database = Firebase.database.reference
        sessionManager = SessionManager(this)

        // Verificar se usuário está logado
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupFirebaseListener()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.home_title)
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            context = this,
            tasks = emptyList(),
            onItemClick = { task ->
                // Abrir tela de edição quando clicar no item
                val intent = Intent(this, TaskActivity::class.java)
                intent.putExtra("task_id", task.id)
                intent.putExtra("is_edit", true)
                startActivity(intent)
            },
            onEditClick = { task ->
                // Abrir tela de edição
                val intent = Intent(this, TaskActivity::class.java)
                intent.putExtra("task_id", task.id)
                intent.putExtra("is_edit", true)
                startActivity(intent)
            },
            onDeleteClick = { task ->
                // Mostrar diálogo de confirmação
                showDeleteConfirmationDialog(task)
            },
            onCompleteToggle = { task, isCompleted ->
                // Atualizar status da tarefa
                updateTaskStatus(task, isCompleted)
            }
        )

        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = taskAdapter
        }

        // Referência para as tarefas do usuário
        val userId = sessionManager.getUserId()
        tasksRef = database.child("tasks").child(userId ?: "")
    }

    private fun setupClickListeners() {
        binding.fabAddTask.setOnClickListener {
            val intent = Intent(this, TaskActivity::class.java)
            intent.putExtra("is_edit", false)
            startActivity(intent)
        }
    }

    private fun setupFirebaseListener() {
        binding.progressBar.visibility = View.VISIBLE

        tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                
                for (taskSnapshot in snapshot.children) {
                    val taskMap = taskSnapshot.value as? Map<String, Any>
                    if (taskMap != null) {
                        val task = Task.fromMap(taskMap)
                        tasks.add(task)
                    }
                }

                // Ordenar por data de criação (mais recentes primeiro)
                tasks.sortByDescending { it.createdAt }

                taskAdapter.updateTasks(tasks)
                
                // Mostrar/ocultar estado vazio
                if (tasks.isEmpty()) {
                    binding.llEmptyState.visibility = View.VISIBLE
                    binding.rvTasks.visibility = View.GONE
                } else {
                    binding.llEmptyState.visibility = View.GONE
                    binding.rvTasks.visibility = View.VISIBLE
                }

                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@HomeActivity, "Erro ao carregar tarefas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateTaskStatus(task: Task, isCompleted: Boolean) {
        val updatedTask = task.copy(
            completed = isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        
        tasksRef.child(task.id).setValue(updatedTask.toMap())
            .addOnSuccessListener {
                // Sucesso
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao atualizar tarefa", Toast.LENGTH_SHORT).show()
                // Reverter o checkbox em caso de erro
                taskAdapter.notifyItemChanged(taskAdapter.tasks.indexOf(task))
            }
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Tarefa")
            .setMessage("Tem certeza que deseja excluir a tarefa \"${task.title}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        tasksRef.child(task.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.success_task_deleted), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao excluir tarefa", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Tem certeza que deseja sair?")
            .setPositiveButton("Sair") { _, _ ->
                auth.signOut()
                sessionManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Verificar se usuário ainda está logado
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
