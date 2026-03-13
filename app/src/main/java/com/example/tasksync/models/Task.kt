package com.example.tasksync.models

import java.io.Serializable

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val completed: Boolean = false,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable {
    
    // Construtor secundário para facilitar a criação
    constructor(
        title: String,
        description: String,
        date: String,
        time: String,
        userId: String
    ) : this(
        id = "",
        title = title,
        description = description,
        date = date,
        time = time,
        completed = false,
        userId = userId
    )
    
    // Método para converter para Map (para Firebase)
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "date" to date,
            "time" to time,
            "completed" to completed,
            "userId" to userId,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
    
    companion object {
        // Método para criar Task a partir de Map (do Firebase)
        fun fromMap(map: Map<String, Any>): Task {
            return Task(
                id = map["id"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                date = map["date"] as? String ?: "",
                time = map["time"] as? String ?: "",
                completed = map["completed"] as? Boolean ?: false,
                userId = map["userId"] as? String ?: "",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}
