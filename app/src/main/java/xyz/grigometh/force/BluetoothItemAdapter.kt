package xyz.grigometh.force

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.bluetooth_item.view.*

class BluetoothItemAdapter(private val bluetoothList: List<BluetoothItem>, private val listener: (Int) -> Unit) :
    RecyclerView.Adapter<BluetoothItemAdapter.BluetoothItemViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothItemViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(
                R.layout.bluetooth_item, parent,
                false
            )
        return BluetoothItemViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return bluetoothList.size
    }

    override fun onBindViewHolder(holder: BluetoothItemViewHolder, position: Int) {
        holder.bind(bluetoothList[position], position, listener)
    }

    class BluetoothItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: BluetoothItem, position: Int, listener: (Int) -> Unit) = with(itemView) {
            bluetooth_name.text = item.name
            bluetooth_address.text = item.address
            setOnClickListener {
                listener(position)
            }
        }
    }
}