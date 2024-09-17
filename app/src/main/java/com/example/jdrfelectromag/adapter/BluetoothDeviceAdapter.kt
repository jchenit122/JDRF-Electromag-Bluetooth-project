package com.example.jdrfelectromag.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jdrfelectromag.databinding.ItemBluetoothDeviceBinding

class BluetoothDeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit,
    private val onStatusClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.BluetoothDeviceViewHolder>() {

    private val scanResults = mutableListOf<ScanResult>()
    private val deviceStatusMap = mutableMapOf<String, Boolean>() // True for connected, false for disconnected

    fun submitList(newResults: List<ScanResult>) {
        scanResults.clear()
        scanResults.addAll(newResults)
        // Update the status for each device if it already exists in the map
        scanResults.forEach { scanResult ->
            deviceStatusMap[scanResult.device.address] = deviceStatusMap[scanResult.device.address] ?: false
        }
        notifyDataSetChanged()
    }

    fun updateDeviceStatus(deviceAddress: String, isConnected: Boolean) {
        deviceStatusMap[deviceAddress] = isConnected
        notifyDataSetChanged() // Refresh the list to show updated status
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothDeviceViewHolder {
        val binding = ItemBluetoothDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BluetoothDeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BluetoothDeviceViewHolder, position: Int) {
        val scanResult = scanResults[position]
        val device = scanResult.device
        val isConnected = deviceStatusMap[device.address] ?: false
        holder.bind(device, isConnected, onDeviceClick, onStatusClick)
    }

    override fun getItemCount(): Int = scanResults.size

    class BluetoothDeviceViewHolder(private val binding: ItemBluetoothDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("MissingPermission")
        fun bind(
            device: BluetoothDevice,
            isConnected: Boolean,
            onDeviceClick: (BluetoothDevice) -> Unit,
            onStatusClick: (BluetoothDevice) -> Unit
        ) {
            binding.deviceName.text = device.name
            binding.deviceMac.text = device.address
            binding.deviceStatus.text = if (isConnected) "Connected" else "Disconnected"

            // Handle device row click
            binding.root.setOnClickListener {
                onDeviceClick(device)
            }

            // Handle status click to connect/disconnect
            binding.deviceStatus.setOnClickListener {
                onStatusClick(device)
            }
        }
    }
}