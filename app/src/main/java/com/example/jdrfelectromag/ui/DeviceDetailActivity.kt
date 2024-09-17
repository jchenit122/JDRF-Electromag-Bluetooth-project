package com.example.jdrfelectromag.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jdrfelectromag.adapter.GattServicesAdapter
import com.example.jdrfelectromag.databinding.ActivityDeviceDetailBinding

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceDetailBinding
    lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var device: BluetoothDevice
    private val gattServicesAdapter = GattServicesAdapter()

    @SuppressLint("MissingPermission", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve Bluetooth device from intent
        device = intent.getParcelableExtra("bluetoothDevice", BluetoothDevice::class.java)?: return finish()

        // Set device name and MAC address in the UI
        binding.deviceName.text = device.name
        binding.deviceMac.text = device.address

        setupRecyclerView()

        // Start connection to the Bluetooth device
        connectToDevice()
    }

    private fun setupRecyclerView() {
        binding.servicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DeviceDetailActivity)
            adapter = gattServicesAdapter
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.services?.let { services ->
                    runOnUiThread {
                        gattServicesAdapter.submitList(services)
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                bluetoothGatt.discoverServices()
                runOnUiThread {
                    Toast.makeText(this@DeviceDetailActivity, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@DeviceDetailActivity, "Disconnected from ${device.name}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt.close() // Clean up resources
    }
}