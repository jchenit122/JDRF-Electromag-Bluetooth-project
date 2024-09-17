package com.example.jdrfelectromag.adapter

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jdrfelectromag.databinding.ItemGattServiceBinding
import com.example.jdrfelectromag.ui.CharacteristicView

class GattServicesAdapter : RecyclerView.Adapter<GattServicesAdapter.GattServiceViewHolder>() {

    private val services = mutableListOf<BluetoothGattService>()

    fun submitList(newServices: List<BluetoothGattService>) {
        services.clear()
        services.addAll(newServices)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GattServiceViewHolder {
        val binding = ItemGattServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GattServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GattServiceViewHolder, position: Int) {
        val service = services[position]
        holder.bind(service)
    }

    override fun getItemCount(): Int = services.size

    class GattServiceViewHolder(private val binding: ItemGattServiceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(service: BluetoothGattService) {
            binding.serviceUuid.text = "Service: ${service.uuid}"

            // Loop through characteristics and display them
            service.characteristics.forEach { characteristic ->
                binding.characteristicsContainer.addView(createCharacteristicView(characteristic))
            }
        }

        private fun createCharacteristicView(characteristic: BluetoothGattCharacteristic): CharacteristicView {
            val view = CharacteristicView(binding.root.context)
            view.setCharacteristic(characteristic)
            return view
        }
    }
}