package com.example.listadecompras

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import androidx.appcompat.app.AppCompatActivity
import com.example.listadecompras.databinding.ActivityCadastroBinding
import androidx.activity.viewModels
import com.example.listadecompras.viewmodel.UserViewModel
import android.util.Log

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCadastrar.setOnClickListener {
            val nome = binding.edtNome.text.toString().trim()
            val email = binding.edtEmailCadastro.text.toString().trim()
            val senha = binding.edtSenhaCadastro.text.toString().trim()
            val confirmar = binding.edtConfirmarSenha.text.toString().trim()

            if (!validateFields(nome, email, senha, confirmar)) {
                return@setOnClickListener
            }

            userViewModel.register(email, senha, nome)
        }

        userViewModel.registerResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                // Redireciona para a tela de Login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }.onFailure {
                val errorMessage = mapFirebaseAuthError(it)
                Log.e("CadastroActivity", "Erro de cadastro: ${it.message}", it)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateFields(nome: String, email: String, senha: String, confirmar: String): Boolean {
        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "E-mail inválido. Verifique o formato (ex: nome@dominio.com)", Toast.LENGTH_SHORT).show()
            return false
        }

        if (senha != confirmar) {
            Toast.makeText(this, "Senha e confirmação não conferem.", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (senha.length < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun mapFirebaseAuthError(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthUserCollisionException -> "Este e-mail já está cadastrado. Tente fazer login."
            is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Use pelo menos 6 caracteres, incluindo letras e números."
            is FirebaseAuthInvalidCredentialsException -> "O formato do e-mail é inválido."
            else -> "Erro ao cadastrar: ${exception.message}"
        }
    }
}
