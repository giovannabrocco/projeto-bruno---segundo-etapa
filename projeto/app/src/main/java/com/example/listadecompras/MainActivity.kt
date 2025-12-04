package com.example.listadecompras

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.listadecompras.databinding.ActivityMainBinding
import androidx.activity.viewModels
import com.example.listadecompras.viewmodel.UserViewModel
import com.example.listadecompras.viewmodel.ListaViewModel

/**
 * VIEW: Tela principal que exibe a lista de compras.
 * Observa o LiveData do ListaViewModel para atualizar a UI.
 */
class MainActivity : AppCompatActivity() {

    private val userViewModel: UserViewModel by viewModels()
    private val listaViewModel: ListaViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var listaAdapter: ListaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // RF001: Verifica se há usuário logado. Se não houver, volta para o login.
        if (!userViewModel.isUserLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        setupObservers()
        // Inicia a escuta em tempo real do Firestore
        listaViewModel.startListasListener()
    }

    

    private fun setupRecyclerView() {
        // Inicializa o adapter com uma lista vazia
        listaAdapter = ListaAdapter(mutableListOf(), { listaCompra -> abrirItensDaLista(listaCompra) }, listaViewModel)
        binding.recyclerView.apply {
            // Layout de Grid para o novo design
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = listaAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddLista.setOnClickListener {
            showCreateListDialog()
        }

        binding.btnLogout.setOnClickListener {
            userViewModel.logout() // Realiza o logout no Firebase

            // Redireciona para a tela de login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Toast.makeText(this, "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show()
        }
        
        // RF005: Implementação da busca
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                // CORREÇÃO: Gerenciamento do Listener para busca
                if (query.isEmpty()) {
                    // Se a busca estiver vazia, reativa o listener em tempo real
                    listaViewModel.startListasListener()
                } else {
                    // Se a busca for iniciada, para o listener em tempo real para evitar conflito
                    listaViewModel.stopListasListener()
                    // E realiza a busca (consulta única)
                    listaViewModel.searchListas(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers() {
        // Observa o LiveData de listas. Qualquer mudança no Firestore notifica a UI.
        listaViewModel.listas.observe(this) { listas ->
            listaAdapter.updateList(listas.toMutableList())
        }

        listaViewModel.createResult.observe(this) { result ->
            result.onSuccess { listaId ->
                Toast.makeText(this, "Lista criada com sucesso!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "Erro ao criar lista: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        listaViewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Lista atualizada com sucesso!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "Erro ao atualizar lista: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        listaViewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Lista excluída com sucesso!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "Erro ao excluir lista: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showCreateListDialog() {
// Diálogo simples de criação de lista (sem seleção de imagem)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Criar Nova Lista de Compras")

        val input = EditText(this)
        input.hint = "Título da Lista"
        builder.setView(input)

        builder.setPositiveButton("Criar") { dialog, _ ->
            val titulo = input.text.toString().trim()
            if (titulo.isNotEmpty()) {
                listaViewModel.createLista(titulo)
            } else {
                Toast.makeText(this, "O título não pode ser vazio!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // A busca agora é feita diretamente no ViewModel/Repository
    // private fun filterLists(query: String) { ... }

    private fun abrirItensDaLista(listaCompra: ListaCompra) {
        val intent = Intent(this, ItemActivity::class.java)
        intent.putExtra("LISTA_ID", listaCompra.id)
        intent.putExtra("TITULO_LISTA", listaCompra.titulo)
        startActivity(intent)
    }
}
