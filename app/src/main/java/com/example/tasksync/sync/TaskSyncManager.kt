package com.example.tasksync.sync

import android.content.Context
import android.util.Log
import com.example.tasksync.database.DatabaseHelper
import com.example.tasksync.models.Task
import com.example.tasksync.models.User
import com.google.firebase.database.*

class TaskSyncManager(private val context: Context) {

    private val databaseHelper = DatabaseHelper(context)
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    companion object {
        private const val TAG = "TaskSyncManager"
    }

    // Sincronizar usuário do Firebase para o banco local
    fun syncUserFromFirebase(userId: String, onComplete: (User?) -> Unit) {
        firebaseDatabase.getReference("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userMap = snapshot.value as? Map<String, Any>
                    if (userMap != null) {
                        val user = User.fromMap(userMap)
                        databaseHelper.insertUser(user)
                        onComplete(user)
                    } else {
                        onComplete(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Erro ao sincronizar usuário", error.toException())
                    onComplete(null)
                }
            })
    }

    // Sincronizar usuário do banco local para o Firebase
    fun syncUserToFirebase(user: User, onComplete: (Boolean) -> Unit) {
        firebaseDatabase.getReference("users").child(user.id)
            .setValue(user.toMap())
            .addOnSuccessListener {
                databaseHelper.updateUser(user)
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Erro ao sincronizar usuário para Firebase", exception)
                onComplete(false)
            }
    }

    // Sincronizar tarefas do Firebase para o banco local
    fun syncTasksFromFirebase(userId: String, onComplete: (List<Task>) -> Unit) {
        firebaseDatabase.getReference("tasks").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tasks = mutableListOf<Task>()
                    
                    for (taskSnapshot in snapshot.children) {
                        val taskMap = taskSnapshot.value as? Map<String, Any>
                        if (taskMap != null) {
                            val task = Task.fromMap(taskMap)
                            tasks.add(task)
                            
                            // Inserir ou atualizar no banco local
                            val existingTask = databaseHelper.getTask(task.id)
                            if (existingTask == null) {
                                databaseHelper.insertTask(task)
                            } else {
                                databaseHelper.updateTask(task)
                            }
                        }
                    }
                    
                    onComplete(tasks)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Erro ao sincronizar tarefas", error.toException())
                    onComplete(emptyList())
                }
            })
    }

    // Sincronizar tarefa específica do Firebase para o banco local
    fun syncTaskFromFirebase(userId: String, taskId: String, onComplete: (Task?) -> Unit) {
        firebaseDatabase.getReference("tasks").child(userId).child(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val taskMap = snapshot.value as? Map<String, Any>
                    if (taskMap != null) {
                        val task = Task.fromMap(taskMap)
                        
                        // Inserir ou atualizar no banco local
                        val existingTask = databaseHelper.getTask(task.id)
                        if (existingTask == null) {
                            databaseHelper.insertTask(task)
                        } else {
                            databaseHelper.updateTask(task)
                        }
                        
                        onComplete(task)
                    } else {
                        // Se não existe no Firebase, remover do banco local
                        databaseHelper.deleteTask(taskId)
                        onComplete(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Erro ao sincronizar tarefa", error.toException())
                    onComplete(null)
                }
            })
    }

    // Sincronizar tarefa do banco local para o Firebase
    fun syncTaskToFirebase(task: Task, onComplete: (Boolean) -> Unit) {
        firebaseDatabase.getReference("tasks").child(task.userId).child(task.id)
            .setValue(task.toMap())
            .addOnSuccessListener {
                databaseHelper.updateTask(task)
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Erro ao sincronizar tarefa para Firebase", exception)
                onComplete(false)
            }
    }

    // Excluir tarefa do Firebase e do banco local
    fun deleteTaskFromFirebase(userId: String, taskId: String, onComplete: (Boolean) -> Unit) {
        firebaseDatabase.getReference("tasks").child(userId).child(taskId)
            .removeValue()
            .addOnSuccessListener {
                databaseHelper.deleteTask(taskId)
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Erro ao excluir tarefa do Firebase", exception)
                onComplete(false)
            }
    }

    // Sincronização bidirecional completa
    fun performFullSync(userId: String, onComplete: (Boolean) -> Unit) {
        Log.d(TAG, "Iniciando sincronização completa para usuário: $userId")
        
        // Primeiro, obter tarefas locais
        val localTasks = databaseHelper.getTasks(userId)
        
        // Sincronizar do Firebase para o local
        syncTasksFromFirebase(userId) { firebaseTasks ->
            // Comparar e sincronizar tarefas locais que não existem no Firebase
            localTasks.forEach { localTask ->
                val existsInFirebase = firebaseTasks.any { it.id == localTask.id }
                if (!existsInFirebase) {
                    syncTaskToFirebase(localTask) { success ->
                        if (success) {
                            Log.d(TAG, "Tarefa ${localTask.id} sincronizada para Firebase")
                        } else {
                            Log.e(TAG, "Falha ao sincronizar tarefa ${localTask.id} para Firebase")
                        }
                    }
                }
            }
            
            Log.d(TAG, "Sincronização completa finalizada")
            onComplete(true)
        }
    }

    // Configurar listener para sincronização em tempo real
    fun setupRealtimeSync(userId: String) {
        firebaseDatabase.getReference("tasks").child(userId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleTaskChange(snapshot, "added")
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleTaskChange(snapshot, "changed")
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val taskMap = snapshot.value as? Map<String, Any>
                    if (taskMap != null) {
                        val task = Task.fromMap(taskMap)
                        databaseHelper.deleteTask(task.id)
                        Log.d(TAG, "Tarefa ${task.id} removida do banco local")
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // Não implementado para este caso
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Erro no listener em tempo real", error.toException())
                }
            })
    }

    private fun handleTaskChange(snapshot: DataSnapshot, action: String) {
        val taskMap = snapshot.value as? Map<String, Any>
        if (taskMap != null) {
            val task = Task.fromMap(taskMap)
            
            val existingTask = databaseHelper.getTask(task.id)
            if (existingTask == null) {
                databaseHelper.insertTask(task)
                Log.d(TAG, "Tarefa ${task.id} inserida no banco local ($action)")
            } else {
                // Atualizar apenas se a versão do Firebase for mais recente
                if (task.updatedAt > existingTask.updatedAt) {
                    databaseHelper.updateTask(task)
                    Log.d(TAG, "Tarefa ${task.id} atualizada no banco local ($action)")
                }
            }
        }
    }

    // Obter tarefas offline (do banco local)
    fun getOfflineTasks(userId: String): List<Task> {
        return databaseHelper.getTasks(userId)
    }

    // Salvar tarefa offline (no banco local)
    fun saveTaskOffline(task: Task): Boolean {
        return try {
            val existingTask = databaseHelper.getTask(task.id)
            if (existingTask == null) {
                databaseHelper.insertTask(task) > 0
            } else {
                databaseHelper.updateTask(task) > 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar tarefa offline", e)
            false
        }
    }

    // Verificar se há conexão com a internet (simplificado)
    fun isOnline(): Boolean {
        // Em uma implementação real, você verificaria a conectividade
        // Para este exemplo, vamos assumir que sempre está online
        return true
    }
}
