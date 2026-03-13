package com.example.tasksync.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.tasksync.models.User
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor = prefs.edit()
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "TaskSyncPrefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER = "user"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // Salvar usuário logado
    fun saveUser(user: User) {
        editor.putString(KEY_USER_ID, user.id)
        editor.putString(KEY_USER, gson.toJson(user))
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    // Obter usuário logado
    fun getUser(): User? {
        val userJson = prefs.getString(KEY_USER, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else {
            null
        }
    }

    // Obter ID do usuário logado
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    // Verificar se usuário está logado
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Fazer logout
    fun logout() {
        editor.clear()
        editor.apply()
    }

    // Salvar token do Firebase (se necessário)
    fun saveFirebaseToken(token: String) {
        editor.putString("firebase_token", token)
        editor.apply()
    }

    // Obter token do Firebase
    fun getFirebaseToken(): String? {
        return prefs.getString("firebase_token", null)
    }
}
