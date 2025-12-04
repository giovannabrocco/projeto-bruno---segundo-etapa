package com.example.listadecompras

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.listadecompras.databinding.ItemListaBinding
import com.example.listadecompras.viewmodel.ListaViewModel

/**
 * ADAPTER: Responsável por exibir as listas de compras no RecyclerView.
 * Utiliza DiffUtil para otimizar as atualizações da UI.
 */
class ListaAdapter(
    private var listaDeCompras: MutableList<ListaCompra>,
    private val abrirItens: (ListaCompra) -> Unit,
    private val viewModel: ListaViewModel
) : RecyclerView.Adapter<ListaAdapter.ListaViewHolder>() {

    /**
     * Atualiza a lista de compras usando DiffUtil para animações e performance.
     */
    fun updateList(newList: List<ListaCompra>) {
        val diffCallback = ListaDiffCallback(this.listaDeCompras, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.listaDeCompras.clear()
        this.listaDeCompras.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListaViewHolder {
        val binding = ItemListaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListaViewHolder, position: Int) {
        val listaCompra = listaDeCompras[position]
        holder.bind(listaCompra)
    }

    override fun getItemCount(): Int = listaDeCompras.size

    inner class ListaViewHolder(private val binding: ItemListaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(listaCompra: ListaCompra) {
            binding.titulo.text = listaCompra.titulo

            // Imagem removida conforme solicitação. Exibindo apenas o placeholder.
            binding.imgLista.setImageResource(R.drawable.placeholder)

            binding.root.setOnClickListener {
                abrirItens(listaCompra)
            }

            // Implementação do clique longo para edição/exclusão (RF003)
            binding.root.setOnLongClickListener {
                showEditDeleteDialog(listaCompra)
                true
            }
        }

        private fun showEditDeleteDialog(listaCompra: ListaCompra) {
            val context = binding.root.context
            val options = arrayOf("Editar Título", "Excluir Lista")

            AlertDialog.Builder(context)
                .setTitle("Opções para ${listaCompra.titulo}")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> showEditTitleDialog(listaCompra)
                        1 -> showDeleteConfirmationDialog(listaCompra)
                    }
                }
                .show()
        }

        private fun showEditTitleDialog(listaCompra: ListaCompra) {
            val context = binding.root.context
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Editar Título da Lista")

            val input = EditText(context)
            input.setText(listaCompra.titulo)
            builder.setView(input)

            builder.setPositiveButton("Salvar") { dialog, _ ->
                val novoTitulo = input.text.toString().trim()
                if (novoTitulo.isNotEmpty()) {
                    // Implementar a lógica de edição do título no ViewModel
                    listaCompra.titulo = novoTitulo
                    viewModel.updateLista(listaCompra)
                    // CORREÇÃO: Força a atualização imediata da UI para refletir a mudança de título.
                    // Isso garante a reatividade instantânea, mesmo que o Listener do Firestore demore um pouco.
                    notifyItemChanged(adapterPosition)
                } else {
                    Toast.makeText(context, "O título não pode ser vazio!", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        private fun showDeleteConfirmationDialog(listaCompra: ListaCompra) {
            val context = binding.root.context
            AlertDialog.Builder(context)
                .setTitle("Excluir Lista")
                .setMessage("Tem certeza que deseja excluir a lista '${listaCompra.titulo}' e todos os seus itens?")
                .setPositiveButton("Excluir") { _, _ ->
                    viewModel.deleteLista(listaCompra.id)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}

/**
 * DiffUtil.Callback: Usado para calcular as diferenças entre duas listas de ListaCompra.
 * Essencial para a performance do RecyclerView.
 */
class ListaDiffCallback(
    private val oldList: List<ListaCompra>,
    private val newList: List<ListaCompra>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // CORREÇÃO: Compara o conteúdo dos itens.
        // A comparação do título é crucial para que o DiffUtil detecte a edição do nome da lista.
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.titulo == newItem.titulo
    }
}
