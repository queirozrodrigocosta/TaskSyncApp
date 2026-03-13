package com.example.tasksync

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tasksync.models.User
import com.example.tasksync.sync.TaskSyncManager
import com.example.tasksync.utils.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.tasksync.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var database: DatabaseReference
    private lateinit var sessionManager: SessionManager
    private lateinit var syncManager: TaskSyncManager

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        auth = Firebase.auth
        database = Firebase.database.reference
        sessionManager = SessionManager(this)
        syncManager = TaskSyncManager(this)

        // Configurar Google Sign In
        configureGoogleSignIn()

        // Verificar se usuário já está logado
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        setupClickListeners()
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (validateForm(email, password)) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            // Sincronizar usuário e tarefas
                            syncManager.syncUserFromFirebase(user.uid) { syncedUser ->
                                if (syncedUser != null) {
                                    sessionManager.saveUser(syncedUser)
                                    syncManager.syncTasksFromFirebase(user.uid) { _ ->
                                        syncManager.setupRealtimeSync(user.uid)
                                        startActivity(Intent(this, HomeActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    Toast.makeText(this, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Mostrar erro específico
                val errorMessage = when (e.statusCode) {
                    7 -> "Conexão com Google interrompida. Tente novamente."
                    10 -> "Operação cancelada pelo usuário."
                    8 -> "Dispositivo offline. Verifique sua conexão."
                    12501 -> "Google Play Services não instalado ou desatualizado."
                    12502 -> "Google Sign-In desativado para este app."
                    else -> "Falha na autenticação com Google (código: ${e.statusCode})"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                
                // Log para debug
                println("ERRO GOOGLE SIGN-IN: ${e.statusCode} - ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Salvar dados do usuário no Firebase Database
                        saveUserToDatabase(user.uid, account.displayName ?: "", user.email ?: "")
                        
                        // Sincronizar usuário e tarefas
                        syncManager.syncUserFromFirebase(user.uid) { syncedUser ->
                            if (syncedUser != null) {
                                sessionManager.saveUser(syncedUser)
                                syncManager.syncTasksFromFirebase(user.uid) { _ ->
                                    syncManager.setupRealtimeSync(user.uid)
                                    startActivity(Intent(this, HomeActivity::class.java))
                                    finish()
                                }
                            } else {
                                Toast.makeText(this, "Falha na autenticação", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Falha na autenticação", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase(userId: String, name: String, email: String) {
        val user = User(userId, name, email)
        database.child("users").child(userId).setValue(user.toMap())
    }

    private fun saveUserToSession(userId: String) {
        database.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val userMap = snapshot.value as? Map<String, Any>
                if (userMap != null) {
                    val user = User.fromMap(userMap)
                    sessionManager.saveUser(user)
                }
            }
    }

    private fun validateForm(email: String, password: String): Boolean {
        var valid = true

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.error = getString(R.string.error_empty_fields)
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.error = getString(R.string.error_empty_fields)
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        return valid
    }
}
