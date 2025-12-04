package com.example.listadecompras.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.example.listadecompras.Item
import com.example.listadecompras.data.FirebaseRepository

/**
 * VIEWMODEL: Gerencia o estado da UI para a tela de Itens.
 * Atua como intermediário entre a View (ItemActivity) e o Model (FirebaseRepository).
 */
class ItemViewModel : ViewModel() {

    private val repository = FirebaseRepository()
    private var itensListenerRegistration: ListenerRegistration? = null

    // LiveData que expõe a lista de itens para a View (ItemActivity)
    private val _itens = MutableLiveData<List<Item>>()
    val itens: LiveData<List<Item>> = _itens

    private val _createResult = MutableLiveData<Result<String>>()
    val createResult: LiveData<Result<String>> = _createResult

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    /**
     * Inicia o Listener em tempo real do Firestore para os itens de uma lista específica.
     * O resultado é passado para o LiveData '_itens', que notifica a View.
     */
    fun startItensListener(listaId: String) {
        // Se o listener já estiver ativo, não faz nada.
        if (itensListenerRegistration != null) return

        itensListenerRegistration = repository.getItemsFromLista(
            listaId = listaId,
            onSuccess = { _itens.value = it },
            onFailure = { /* Tratar erro de carregamento */ }
        )
    }

    /**
     * CORREÇÃO: Para o Listener em tempo real.
     * Essencial para que a busca (consulta única) possa ser realizada sem conflito
     * e para que o listener possa ser reativado ao limpar a busca.
     */
    fun stopItensListener() {
        itensListenerRegistration?.remove()
        itensListenerRegistration = null
    }

    /**
     * Garante que o listener seja removido quando o ViewModel for destruído.
     */
    override fun onCleared() {
        super.onCleared()
        stopItensListener()
    }

    fun createItem(listaId: String, nome: String, quantidade: Int, unidade: String, categoria: String) {
        val novoItem = Item(nome = nome, quantidade = quantidade, unidade = unidade, categoria = categoria)
        repository.createItem(
            listaId = listaId,
            item = novoItem,
            onSuccess = { _createResult.value = Result.success(it) },
            onFailure = { _createResult.value = Result.failure(it) }
        )
    }

    fun updateItem(listaId: String, item: Item) {
        repository.updateItem(
            listaId = listaId,
            item = item,
            onSuccess = { _updateResult.value = Result.success(Unit) },
            onFailure = { _updateResult.value = Result.failure(it) }
        )
    }

    fun deleteItem(listaId: String, itemId: String) {
        repository.deleteItem(
            listaId = listaId,
            itemId = itemId,
            onSuccess = { _deleteResult.value = Result.success(Unit) },
            onFailure = { _deleteResult.value = Result.failure(it) }
        )
    }

    /**
     * RF005: Realiza a busca de itens.
     * O resultado (consulta única) é passado para o LiveData '_itens'.
     */
    fun searchItens(listaId: String, query: String) {
        repository.searchItens(
            listaId = listaId,
            query = query,
            onSuccess = { _itens.value = it },
            onFailure = { /* Tratar erro de busca */ }
        )
    }
}
