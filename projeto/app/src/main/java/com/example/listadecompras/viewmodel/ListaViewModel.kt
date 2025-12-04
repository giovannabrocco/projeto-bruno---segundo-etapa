package com.example.listadecompras.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.example.listadecompras.ListaCompra
import com.example.listadecompras.data.FirebaseRepository

/**
 * VIEWMODEL: Gerencia o estado da UI para a tela de Listas.
 * Atua como intermediário entre a View (MainActivity) e o Model (FirebaseRepository).
 */
class ListaViewModel : ViewModel() {

    private val repository = FirebaseRepository()
    private var listasListenerRegistration: ListenerRegistration? = null

    // LiveData que expõe a lista de compras para a View (MainActivity)
    private val _listas = MutableLiveData<List<ListaCompra>>()
    val listas: LiveData<List<ListaCompra>> = _listas

    private val _createResult = MutableLiveData<Result<String>>()
    val createResult: LiveData<Result<String>> = _createResult

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    /**
     * Inicia o Listener em tempo real do Firestore.
     * O resultado é passado para o LiveData '_listas', que notifica a View.
     */
    fun startListasListener() {
        // Se o listener já estiver ativo, não faz nada.
        if (listasListenerRegistration != null) return

        listasListenerRegistration = repository.getListas(
            onSuccess = {
                Log.d("ListaViewModel", "Atualização de listas recebida. Tamanho: ${it.size}")
                _listas.value = it
            },
            onFailure = { Log.e("ListaViewModel", "Erro ao carregar listas: ${it.message}") }
        )
    }

    /**
     * CORREÇÃO: Para o Listener em tempo real.
     * Essencial para que a busca (consulta única) possa ser realizada sem conflito
     * e para que o listener possa ser reativado ao limpar a busca.
     */
    fun stopListasListener() {
        listasListenerRegistration?.remove()
        listasListenerRegistration = null
    }

    /**
     * Garante que o listener seja removido quando o ViewModel for destruído.
     */
    override fun onCleared() {
        super.onCleared()
        stopListasListener()
    }

    fun updateLista(lista: ListaCompra) {
        repository.updateLista(
            lista = lista,
            onSuccess = { _updateResult.value = Result.success(Unit) },
            onFailure = { _updateResult.value = Result.failure(it) }
        )
    }

    fun createLista(titulo: String) {
        val novaLista = ListaCompra(titulo = titulo)
        repository.createLista(
            lista = novaLista,
            onSuccess = { _createResult.value = Result.success(it) },
            onFailure = { _createResult.value = Result.failure(it) }
        )
    }

    fun deleteLista(listaId: String) {
        repository.deleteLista(
            listaId = listaId,
            onSuccess = { _deleteResult.value = Result.success(Unit) },
            onFailure = { _deleteResult.value = Result.failure(it) }
        )
    }

    /**
     * RF005: Realiza a busca de listas.
     * O resultado (consulta única) é passado para o LiveData '_listas'.
     */
    fun searchListas(query: String) {
        repository.searchListas(
            query = query,
            onSuccess = { _listas.value = it },
            onFailure = { /* Tratar erro de busca */ }
        )
    }
}
