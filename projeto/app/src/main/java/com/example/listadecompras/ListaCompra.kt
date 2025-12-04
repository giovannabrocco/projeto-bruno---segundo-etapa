package com.example.listadecompras

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class ListaCompra(
    var id: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("titulo") @set:PropertyName("titulo") var titulo: String = "",
    @Exclude val itens: MutableList<Item> = mutableListOf()
) {
    // Construtor vazio para o Firestore
    constructor() : this("", "", "", mutableListOf())
}