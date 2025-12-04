package com.example.listadecompras

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Item(
    var id: String = "",
    @get:PropertyName("listaId") @set:PropertyName("listaId") var listaId: String = "",
    @get:PropertyName("nome") @set:PropertyName("nome") var nome: String = "",
    @get:PropertyName("quantidade") @set:PropertyName("quantidade") var quantidade: Int = 0,
    @get:PropertyName("unidade") @set:PropertyName("unidade") var unidade: String = "",
    @get:PropertyName("categoria") @set:PropertyName("categoria") var categoria: String = "",
    @get:PropertyName("comprado") @set:PropertyName("comprado") var comprado: Boolean = false
) {
    // Construtor vazio para o Firestore
    constructor() : this("", "", "", 0, "", "", false)
}

