package com.example.jdrfelectromag.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class CharacteristicView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var characteristic: BluetoothGattCharacteristic
    private val valueInput: EditText = EditText(context).apply {
        inputType = InputType.TYPE_CLASS_TEXT
    }
    private val writeButton: Button = Button(context).apply {
        text = "Write"
        setOnClickListener { writeValue() }
    }
    private val descriptorsLayout: LinearLayout = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    init {
        orientation = VERTICAL
        addView(valueInput)
        addView(writeButton)
        addView(descriptorsLayout)  // Add the layout to show descriptors
    }

    fun setCharacteristic(characteristic: BluetoothGattCharacteristic) {
        this.characteristic = characteristic
        writeButton.isEnabled = isWritable(characteristic)
        displayDescriptors(characteristic)
    }

    @SuppressLint("MissingPermission")
    private fun writeValue() {
        val value = valueInput.text.toString().toByteArray()
        characteristic.value = value
        if (!isWritable(characteristic)) {
            Toast.makeText(context, "Characteristic is not writable", Toast.LENGTH_SHORT).show()
            return
        }

        val success = (context as DeviceDetailActivity).bluetoothGatt.writeCharacteristic(characteristic)
        if (success) {
            Toast.makeText(context, "Value written", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to write value", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isWritable(characteristic: BluetoothGattCharacteristic): Boolean {
        return (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) > 0
    }

    // Function to display descriptors
    private fun displayDescriptors(characteristic: BluetoothGattCharacteristic) {
        descriptorsLayout.removeAllViews()  // Clear any previous descriptor views

        for (descriptor in characteristic.descriptors) {
            val descriptorView = TextView(context).apply {
                text = "Descriptor: ${descriptor.uuid}"
            }
            descriptorsLayout.addView(descriptorView)
        }
    }
}