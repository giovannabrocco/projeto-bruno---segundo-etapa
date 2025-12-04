package com.example.listadecompras

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.listadecompras.databinding.ActivityAddItemBinding
import androidx.activity.viewModels
import com.example.listadecompras.viewmodel.ItemViewModel

class AddItemActivity : AppCompatActivity() {

    private val itemViewModel: ItemViewModel by viewModels()

    private lateinit var binding: ActivityAddItemBinding
    private var listaId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadListData()
        setupUI()
        setupListeners()
    }

    private fun loadListData() {
        listaId = intent.getStringExtra("LISTA_ID")
        val listaTitulo = intent.getStringExtra("TITULO_LISTA")
        title = "Adicionar Item a $listaTitulo"

        if (listaId.isNullOrEmpty()) {
            Toast.makeText(this, "ID da Lista não encontrado!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun setupUI() {
        // Configuração do Spinner de Categoria
        ArrayAdapter.createFromResource(
            this,
            R.array.categorias_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategoriaAddItem.adapter = adapter
        }
    }

    private fun setupListeners() {
        binding.btnAdicionarItem.setOnClickListener {
            addItem()
        }
    }

    private fun addItem() {
        val nome = binding.edtNomeAddItem.text.toString().trim()
        val quantidade = binding.edtQuantidadeAddItem.text.toString().toIntOrNull() ?: 0
        val unidade = binding.edtUnidadeAddItem.text.toString().trim()
        val categoria = binding.spinnerCategoriaAddItem.selectedItem.toString()

        if (nome.isNotEmpty() && quantidade > 0 && unidade.isNotEmpty()) {
            listaId?.let {
                itemViewModel.createItem(it, nome, quantidade, unidade, categoria)
            }
            
            // O ItemActivity será atualizado pelo Observer do LiveData
            Toast.makeText(this, "Item adicionado com sucesso!", Toast.LENGTH_SHORT).show()
            finish() // Retorna para a ItemActivity
        } else {
            Toast.makeText(this, "Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
