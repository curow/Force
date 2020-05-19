package xyz.grigometh.force

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_connect.*

class ConnectActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val EXTRA_ADDRESS: String = "Device_address"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val deviceList = ArrayList<BluetoothItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        if (bluetoothAdapter == null) {
            toast("this device doesn't support bluetooth")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, REQUEST_ENABLE_BT)
        }

        device_recycler.layoutManager = LinearLayoutManager(this)
        device_recycler.addItemDecoration(
            DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        )
        device_recycler.adapter = BluetoothItemAdapter(deviceList) {
            for (i in 0 until device_recycler.childCount) {
                val child = device_recycler.getChildAt(i)
                val viewHolder = device_recycler.getChildViewHolder(child)
                val childPosition = viewHolder.adapterPosition
                if (it == childPosition)
                    viewHolder.itemView.setBackgroundResource(R.color.colorPrimaryLight)
                else
                    viewHolder.itemView.setBackgroundColor(Color.WHITE)
            }

            val address = deviceList[it].address
            val intent = Intent(this, ControlActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            startActivity(intent)
        }

        refresh_button.setOnClickListener {
            getPairedDeviceList()
            (device_recycler.adapter as BluetoothItemAdapter).notifyDataSetChanged()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                toast("bluetooth enabled")
            } else {
                toast("bluetooth not enabled")
            }
        }
    }

    private fun getPairedDeviceList() {
        val pairedBluetoothDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (pairedBluetoothDevices?.isEmpty() == false) {
            connect_text.text = getString(R.string.connect_text)
            for (device in pairedBluetoothDevices) {
                val item = BluetoothItem(device.name, device.address)
                if (item !in deviceList)
                    deviceList.add(BluetoothItem(device.name, device.address))
            }
        } else {
            toast("no paired bluetooth device found")
        }
    }
}
