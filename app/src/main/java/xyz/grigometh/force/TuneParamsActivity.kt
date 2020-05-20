package xyz.grigometh.force

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_tune_params.*
import java.io.IOException
import java.io.InputStream

const val MESSAGE_READ: Int = 0
const val LOG_LENGTH = Int.MAX_VALUE
val NOT_USED_PARAMS = listOf(4)

class TuneParamsActivity : AppCompatActivity() {
    companion object {
        var selectedChipNum = -1
        var bluetoothSocket: BluetoothSocket? = null
        val chipNumToCodeMap = mapOf<Int, Byte>(
            0 to 0x03, // speed Kp
            1 to 0x04, // speed Ki
            2 to 0x05, // speed Kd
            3 to 0x01, // steer Kp
//            4 to 0x08, // steer Ki
            5 to 0x02, // steer Kd
            6 to 0x06, // max speed
            7 to 0x07 // min speed
        )
    }

    private lateinit var handler: Handler
    private var adjustmentValue = 0
    private var currentValue = 0
    private var has_chosen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tune_params)
        bluetoothSocket = ControlActivity.bluetoothSocket
        log_text.movementMethod = ScrollingMovementMethod()
        handler = object: Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MESSAGE_READ -> {
                        val length = msg.arg1
                        val text = String((msg.obj as ByteArray).slice(0 until length).toByteArray())
                        log_text.text = "${log_text.text}$text\n".takeLast(LOG_LENGTH)
                    }
                }
            }
        }
        InputMessageThread(bluetoothSocket!!).start()
        parametersChipGroup.setOnCheckedChangeListener { chipGroup, id ->
            for (i in 0 until chipGroup.childCount) {
                val child = chipGroup.getChildAt(i)
                child.isClickable = false
                if (child.id == id) {
                    selectedChipNum = i
                }
            }
            if (selectedChipNum in chipNumToCodeMap.keys) {
                val code = chipNumToCodeMap[selectedChipNum]
                // enter parameter-tuning process
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x03, 0x09))
                toast("0x66 0x03 0x09, start tuning")
                // select one of the parameters to tune
                sendCommand(bluetoothSocket, byteArrayOf(0x66, code!!))
                toast("0x66 0x%02d, chosen".format(code))
                has_chosen = true
            }
        }
        increase_button.setOnClickListener {
            if (has_chosen) {
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x06))
                toast("0x66 0x06, increase")
                adjustmentValue += 1
                adjustment_text.text = adjustmentValue.toString()
            } else {
                toast("choose one of the parameters above")
            }
        }
        decrease_button.setOnClickListener {
            if (has_chosen) {
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x04))
                toast("0x66 0x04, decrease")
                adjustmentValue -= 1
                adjustment_text.text = adjustmentValue.toString()
            } else {
                toast("choose one of the parameters above")
            }
        }
        confirm_button.setOnClickListener {
            if (has_chosen) {
                sendCommand(bluetoothSocket, byteArrayOf(0x66, 0x05))
                toast("0x66 0x05, confirmed")
                adjustmentValue = 0
                adjustment_text.text = adjustmentValue.toString()
                for (i in 0 until parametersChipGroup.childCount) {
                    if (i !in NOT_USED_PARAMS) {
                        val child = parametersChipGroup.getChildAt(i)
                        child.isClickable = true
                    }
                }
            } else {
                toast("choose one of the parameters above")
            }
        }
    }


    private inner class InputMessageThread(socket: BluetoothSocket) : Thread() {

        private val inStream: InputStream = socket.inputStream
        private val buffer: ByteArray = ByteArray(1024)

//        private val randomGenerator = Random(0)

        override fun run() {
            var numBytes: Int // bytes returned from read()


            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    inStream.read(buffer)
                } catch (e: IOException) {
                    Log.d("e", "Input stream was disconnected", e)
                    break
                }
                // Send the obtained bytes to the UI activity.

                // Simple Test
//                sleep(1000)
//                numBytes = 5
//                for (i in 0 until 100) {
//                    buffer[i] = (randomGenerator.nextInt() % 256).toByte()
//                }
                handler.obtainMessage(MESSAGE_READ, numBytes, 0, buffer).sendToTarget()
            }

        }
    }
}
