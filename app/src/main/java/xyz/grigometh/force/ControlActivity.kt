package xyz.grigometh.force

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_control.*
import java.io.IOException
import java.util.*


class ControlActivity : AppCompatActivity() {

    companion object {
        val serialUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        var bluetoothSocket: BluetoothSocket? = null
        lateinit var progress: ProgressDialog
        lateinit var bluetoothAdapter: BluetoothAdapter
        var isConnected: Boolean = false
        lateinit var address: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        address = intent.getStringExtra(ConnectActivity.EXTRA_ADDRESS)!!

        ConnectToDevice(this).execute()
        left_button.setOnClickListener{ sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x01, 0x60)) }
        right_button.setOnClickListener{ sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x01, 0x20)) }
        forward_button.setOnClickListener{ sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x03, 0x07))}
        on_button.setOnClickListener{ sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x03, 0x13))}
        off_button.setOnClickListener{ sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x03, 0x11)) }
        disconnect_button.setOnClickListener{ disconnect() }
        tune_parameter_button.setOnClickListener{ tuneParameter() }
    }

    private fun tuneParameter() {
        val intent = Intent(this, TuneParamsActivity::class.java)
        startActivity(intent)
    }


    private fun disconnect() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket!!.close()
                bluetoothSocket = null
                isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private class ConnectToDevice(val context: Context) : AsyncTask<Void, Void, String>() {
        var connectSuccess: Boolean = true

        override fun onPreExecute() {
            super.onPreExecute()
            progress = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg params: Void?): String? {
            try {
                if (bluetoothSocket == null || !isConnected) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(serialUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.i("data", "couldn't connect")
            } else {
                isConnected = true
            }
            progress.dismiss()
        }
    }
}

