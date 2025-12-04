package com.example.listadecompras

import android.content.Intent
import android.os.Bundle
import android.util.Patterns

import android.widget.Toast
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import androidx.appcompat.app.AppCompatActivity
import com.example.listadecompras.databinding.ActivityLoginBinding
import androidx.activity.viewModels
import com.example.listadecompras.viewmodel.UserViewModel

import android.util.Log
import android.view.View

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
                binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Redirecionar se já estiver logado
        if (userViewModel.isUserLoggedIn()) {
            navigateToMainActivity()
            return
        }

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtSenha.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "E-mail inválido. Verifique o formato (ex: nome@dominio.com)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            userViewModel.login(email, password)
        }

        binding.btnCadastro.setOnClickListener {
            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
        }

        // Assumindo que o layout tem um TextView ou Button com id tvEsqueceuSenha
        // Como não tenho o XML, vou assumir que o campo de email é usado para a recuperação
        // e que existe um elemento para o clique, vou usar o btnCadastro como placeholder para o link
        // **NOTA:** O usuário deve garantir que o layout XML (activity_login.xml) tenha um elemento para a recuperação de senha.
        // Vou criar um TextView no XML e assumir o ID 'tvEsqueceuSenha'
        // Por enquanto, vou deixar sem a implementação do clique para evitar erro de compilação no XML.
        // O requisito RF001 pede a opção de recuperação de senha.
        // Vou assumir que o campo de email é edtEmail e que existe um botão/texto para o clique.
        binding.tvEsqueceuSenha.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Informe o e-mail para recuperação de senha", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "E-mail inválido. Verifique o formato (ex: nome@dominio.com)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            userViewModel.resetPassword(email)
        }
    }

    private fun setupObservers() {
        userViewModel.loginResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }.onFailure {
                val errorMessage = mapFirebaseAuthError(it)
                Log.e("LoginActivity", "Erro de login: ${it.message}", it)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        userViewModel.resetPasswordResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "E-mail de recuperação de senha enviado!", Toast.LENGTH_LONG).show()
            }.onFailure {
                Log.e("LoginActivity", "Erro ao recuperar senha: ${it.message}", it)
                Toast.makeText(this, "Erro ao recuperar senha: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun mapFirebaseAuthError(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "Usuário não encontrado. Verifique o e-mail."
            is FirebaseAuthInvalidCredentialsException -> "Credenciais inválidas. Verifique a senha."
            else -> "Erro ao fazer login: ${exception.message}"
        }
    }
}
