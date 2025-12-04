package com.example.listadecompras.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log
import com.example.listadecompras.ListaCompra
import com.example.listadecompras.Item

/**
 * REPOSITORY: Camada de acesso a dados.
 * Esta classe encapsula toda a lógica de comunicação com o Firebase (Auth e Firestore),
 * isolando o ViewModel e mantendo a arquitetura limpa (MVVM).
 */
class FirebaseRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Coleções
    private val usersCollection = firestore.collection("users")
    private val listsCollection = firestore.collection("shopping_lists")

    // --- Autenticação (RF001: Login, Cadastro, Logout) ---

    fun getCurrentUser() = auth.currentUser

    /**
     * RF001: Realiza o cadastro do usuário no Firebase Auth.
     * Após o sucesso, salva o nome do usuário no Firestore (coleção 'users').
     */
    fun registerUser(email: String, password: String, name: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    // Salva o nome do usuário no Firestore para uso posterior (ex: personalização)
                    saveUserName(userId, name, onSuccess = { onSuccess(userId) }, onFailure = onFailure)
                } else {
                    onFailure(task.exception ?: Exception("Erro desconhecido ao cadastrar"))
                }
            }
    }

    /**
     * Salva o nome do usuário no Firestore.
     */
    private fun saveUserName(userId: String, name: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userMap = hashMapOf("name" to name)
        usersCollection.document(userId).set(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Erro desconhecido ao logar"))
                }
            }
    }

    fun logoutUser() {
        auth.signOut()
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Erro desconhecido ao enviar e-mail de recuperação"))
                }
            }
    }

    // --- Firestore - Listas de Compras (RF003: CRUD de Listas) ---

    fun createLista(lista: ListaCompra, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("Usuário não logado"))
        lista.userId = userId
        listsCollection.add(lista)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener { onSuccess(documentReference.id) }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * RF003: Obtém a lista de compras do usuário em TEMPO REAL.
     * Utiliza addSnapshotListener para reatividade (atualização instantânea da UI).
     */
    fun getListas(onSuccess: (List<ListaCompra>) -> Unit, onFailure: (Exception) -> Unit): ListenerRegistration {
        val userId = auth.currentUser?.uid ?: run {
            onFailure(Exception("Usuário não logado"))
            // Retorna um ListenerRegistration que não faz nada para evitar erro de compilação
            return object : ListenerRegistration {
                override fun remove() {}
            }
        }
        return listsCollection
            .whereEqualTo("userId", userId)
            .orderBy("titulo") // RF003: A listagem deve ser ordenada alfabeticamente
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FirebaseRepository", "Erro ao ouvir listas: ${firebaseFirestoreException.message}", firebaseFirestoreException)
                    onFailure(firebaseFirestoreException)
                    return@addSnapshotListener
                }

                val listas = querySnapshot?.documents?.mapNotNull { document ->
                    // Mapeia o documento para o objeto ListaCompra e define o ID
                    document.toObject(ListaCompra::class.java)?.apply { id = document.id }
                } ?: emptyList()
                Log.d("FirebaseRepository", "Listas recebidas: ${listas.size}")
                onSuccess(listas)
            }
    }

    fun updateLista(lista: ListaCompra, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        listsCollection.document(lista.id).set(lista)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * RF003: Exclui a lista e chama a função para excluir os itens associados (Exclusão em Cascata).
     */
    fun deleteLista(listaId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        listsCollection.document(listaId).delete()
            .addOnSuccessListener {
                // RF003: Ao excluir uma lista, todos os itens associados também devem ser removidos do Firestore
                deleteItensFromLista(listaId, onSuccess, onFailure)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // --- Firestore - Itens da Lista (RF004: CRUD de Itens) ---

    private fun getItemsCollection(listaId: String) = listsCollection.document(listaId).collection("itens")

    fun createItem(listaId: String, item: Item, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        item.listaId = listaId
        getItemsCollection(listaId).add(item)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener { onSuccess(documentReference.id) }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * RF004: Obtém os itens de uma lista em TEMPO REAL.
     * Utiliza addSnapshotListener e as cláusulas orderBy para atender aos requisitos de ordenação.
     */
    fun getItemsFromLista(listaId: String, onSuccess: (List<Item>) -> Unit, onFailure: (Exception) -> Unit): ListenerRegistration {
        return getItemsCollection(listaId)
            .orderBy("categoria") // RF004: agrupados por categoria
            .orderBy("nome") // RF004: ordenados alfabeticamente
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FirebaseRepository", "Erro ao ouvir itens: ${firebaseFirestoreException.message}", firebaseFirestoreException)
                    onFailure(firebaseFirestoreException)
                    return@addSnapshotListener
                }

                val itens = querySnapshot?.documents?.mapNotNull { document ->
                    // Mapeia o documento para o objeto Item e define o ID
                    document.toObject(Item::class.java)?.apply { id = document.id }
                } ?: emptyList()
                Log.d("FirebaseRepository", "Itens recebidos para lista $listaId: ${itens.size}")
                onSuccess(itens)
            }
    }

    fun updateItem(listaId: String, item: Item, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        getItemsCollection(listaId).document(item.id).set(item)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteItem(listaId: String, itemId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        getItemsCollection(listaId).document(itemId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * RF003: Implementa a Exclusão em Cascata de forma eficiente.
     * Utiliza um WriteBatch para deletar todos os itens da subcoleção em uma única operação atômica.
     */
    private fun deleteItensFromLista(listaId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        getItemsCollection(listaId).get()
            .addOnSuccessListener { querySnapshot ->
                val batch = firestore.batch()
                querySnapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // --- Firestore - Buscas (RF005: Pesquisa) ---

    /**
     * RF005: Realiza a busca de listas por prefixo no título.
     * Utiliza consulta única (.get()) e a técnica startAt/endAt para simular o 'startsWith'.
     * NOTA: Esta não é uma consulta em tempo real, por isso o listener precisa ser parado antes de chamá-la.
     */
    fun searchListas(query: String, onSuccess: (List<ListaCompra>) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("Usuário não logado"))
        listsCollection
            .whereEqualTo("userId", userId)
            .orderBy("titulo")
            .startAt(query)
            .endAt(query + "\uf8ff") // Técnica para busca por prefixo
            .get()
            .addOnSuccessListener { querySnapshot ->
                val listas = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(ListaCompra::class.java)?.apply { id = document.id }
                }
                onSuccess(listas)
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * RF005: Realiza a busca de itens por prefixo no nome dentro de uma lista específica.
     * Utiliza consulta única (.get()) e a técnica startAt/endAt.
     */
    fun searchItens(listaId: String, query: String, onSuccess: (List<Item>) -> Unit, onFailure: (Exception) -> Unit) {
        getItemsCollection(listaId)
            .orderBy("nome")
            .startAt(query)
            .endAt(query + "\uf8ff") // Técnica para busca por prefixo
            .get()
            .addOnSuccessListener { querySnapshot ->
                val itens = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Item::class.java)?.apply { id = document.id }
                }
                onSuccess(itens)
            }
            .addOnFailureListener { onFailure(it) }
    }
}
