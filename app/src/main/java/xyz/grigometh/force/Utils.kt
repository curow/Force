package xyz.grigometh.force

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import java.io.IOException

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun sendCommand(socket: BluetoothSocket?, bytes: ByteArray) {
    if (socket!= null) {
        try{
            socket.outputStream.write(bytes)
            SystemClock.sleep(10)
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }
}
