package com.example.tasksync

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tasksync.models.User
import com.example.tasksync.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.tasksync.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        auth = Firebase.auth
        database = Firebase.database.reference
        sessionManager = SessionManager(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (validateForm(name, email, password, confirmPassword)) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            // Salvar dados do usuário no Firebase Database
                            saveUserToDatabase(user.uid, name, email)
                            
                            // Salvar na sessão local
                            val userModel = User(user.uid, name, email)
                            sessionManager.saveUser(userModel)

                            Toast.makeText(this, getString(R.string.success_register), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }
                    } else {
                        val errorMessage = when {
                            task.exception?.message?.contains("email") == true -> "Email já está em uso ou inválido"
                            task.exception?.message?.contains("password") == true -> "Senha muito fraca (mínimo 6 caracteres)"
                            task.exception?.message?.contains("network") == true -> "Erro de conexão. Verifique sua internet"
                            else -> "Falha no registro: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        
                        println("ERRO REGISTRO: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun saveUserToDatabase(userId: String, name: String, email: String) {
        val user = User(userId, name, email)
        database.child("users").child(userId).setValue(user.toMap())
    }

    private fun validateForm(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var valid = true

        // Validar nome
        if (TextUtils.isEmpty(name)) {
            binding.tilName.error = getString(R.string.error_empty_fields)
            valid = false
        } else {
            binding.tilName.error = null
        }

        // Validar email
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.error = getString(R.string.error_empty_fields)
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        // Validar senha
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.error = getString(R.string.error_empty_fields)
            valid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "A senha deve ter pelo menos 6 caracteres"
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        // Validar confirmação de senha
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.tilConfirmPassword.error = getString(R.string.error_empty_fields)
            valid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            valid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return valid
    }
}
