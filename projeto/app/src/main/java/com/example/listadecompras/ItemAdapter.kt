package com.example.listadecompras

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.listadecompras.databinding.ItemBinding

class ItemAdapter(
    private var itens: MutableList<Item>,
    private val onEditClick: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit,
    private val onCheckedChange: (Item, Boolean) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    fun updateList(newList: List<Item>) {
        // RF004: Os itens marcados devem ficar separados (abaixo) dos demais.
        val sortedList = newList.sortedWith(compareBy<Item> { it.comprado }.thenBy { it.categoria }.thenBy { it.nome })
        
        val diffCallback = ItemDiffCallback(this.itens, sortedList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.itens.clear()
        this.itens.addAll(sortedList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itens[position]
        holder.bind(item)
    }

    override fun getItemCount() = itens.size

    inner class ItemViewHolder(private val binding: ItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.checkBoxComprado.setOnCheckedChangeListener(null) // Remove listener para evitar chamadas indesejadas
            binding.checkBoxComprado.isChecked = item.comprado

            binding.txtNomeItem.text = item.nome
            binding.txtQuantidadeUnidade.text = "${item.quantidade} ${item.unidade}"
            binding.txtCategoria.text = item.categoria

            // Aplica ou remove o risco (strike-through)
            val paintFlags = if (item.comprado) {
                binding.txtNomeItem.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.txtNomeItem.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.txtNomeItem.paintFlags = paintFlags
            binding.txtQuantidadeUnidade.paintFlags = paintFlags
            binding.txtCategoria.paintFlags = paintFlags

            binding.checkBoxComprado.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(item, isChecked)
            }

            binding.btnEditarItem.setOnClickListener {
                onEditClick(item)
            }

            binding.btnExcluirItem.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }
}

class ItemDiffCallback(
    private val oldList: List<Item>,
    private val newList: List<Item>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // A comparação do data class deve funcionar corretamente.
        // A comparação do data class deve funcionar corretamente.
        // No entanto, para garantir que o DiffUtil detecte a mudança de posição após a edição (devido à ordenação por 'comprado'),
        // a comparação deve ser mais rigorosa.
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem == newItem
    }
}
