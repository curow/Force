package xyz.grigometh.force

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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
        log_text.movementMethod = ScrollingMovementMethod()

        joystick.setOnMoveListener { angle, strength ->
            var angle = angle
            if (strength == 0) {
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x01, 0x40))
                updateLog("send 0x66 0x04 0x01 0x00, vehicle angle zero\n")
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x03, 0x00))
                updateLog("send 0x66 0x04 0x03 0x00, vehicle speed zero\n")
            } else {
                val forward = angle <= 180
                if (angle > 180) {
                    angle = 180 - (angle % 180)
                }
                val angleHex = (angle.toFloat() / 180 * 127).toByte()

                if (forward) {
                    val speedRatio = (strength.toFloat() / 100)
                    val speedHex = (1 * (1 - speedRatio) + 7 * speedRatio).toByte()
                    sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x03, speedHex))
                    updateLog("send 0x66 0x04 0x03 $speedHex, vehicle forward\n")
                } else {
                    sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x03, 0x15))
                    updateLog("send 0x66 0x03 0x15, vehicle backward\n")
                }
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x01, angleHex))
                updateLog("send 0x66 0x04 0x01 $angleHex, vehicle change angle\n")
            }
        }

        ConnectToDevice(this).execute()
        left_button.setOnClickListener{
            sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x01, 0x60))
            updateLog("send 0x66 0x04 0x01 0x60, vehicle turn left\n")
        }
        right_button.setOnClickListener{
            sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x01, 0x20))
            updateLog("send 0x66 0x04 0x01 0x20, vehicle turn right\n")
        }
        forward_button.setOnClickListener{
            sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x03, 0x07))
            updateLog("send 0x66 0x04 0x03 0x07, vehicle forward\n")
        }
        backward_button.setOnClickListener{
            sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x03, 0x15))
            updateLog("send 0x66 0x03 0x15, vehicle backward\n")
        }
        on_off_button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x03, 0x13))
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x03, 0x00))
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04, 0x01, 0x40))
                updateLog("send 0x66 0x03 0x13, vehicle on\n")
                updateLog("send 0x66 0x04 0x01 0x40, vehicle angle zero\n")
                updateLog("send 0x66 0x04 0x03 0x00, vehicle speed zero\n")
            } else {
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x03, 0x11))
                updateLog("send 0x66 0x03 0x11, vehicle off\n")
            }
        }
        toggle_acc_button.setOnCheckedChangeListener { _, isChecked ->
            sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x03, 0x17))
            updateLog("send 0x66 0x03 0x17, acc ${if (isChecked) "on" else "off"}\n")
        }
        toggle_buzz_button.setOnCheckedChangeListener { _, isChecked ->
            sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x03, 0x07))
            updateLog("send 0x66 0x03 0x07, buzz ${if (isChecked) "on" else "off"}\n")
        }
        disconnect_button.setOnClickListener{
            disconnect()
            updateLog("disconnect from vehicle\n")
        }
        tune_parameter_button.setOnClickListener{ tuneParameter() }
    }

    private fun updateLog(s: String) {
        log_text.append(s)
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

