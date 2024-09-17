package com.example.jdrfelectromag.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jdrfelectromag.adapter.BluetoothDeviceAdapter
import com.example.jdrfelectromag.DeviceDetailActivity
import com.example.jdrfelectromag.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val scannedDevices = MutableLiveData<MutableList<ScanResult>>()
    private val deviceAdapter = BluetoothDeviceAdapter(
        onDeviceClick = { device -> showDeviceDetails(device) },
        onStatusClick = { device -> toggleConnection(device) }
    )

    private lateinit var bluetoothEnableLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // Register for Bluetooth permissions
    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.BLUETOOTH_CONNECT] == true) {
                enableBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permission is required.", Toast.LENGTH_SHORT).show()
            }
        }

    // Sorting options
    enum class SortOption {
        NAME, MAC_ADDRESS, SCAN_TIME
    }

    private var currentSortOption = SortOption.NAME
    private val connectedDevices = mutableMapOf<String, BluetoothGatt>() // Map to track connected devices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize BluetoothAdapter before using it
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setupLaunchers() // Ensure launchers are set up first
        checkBluetoothPermissionsAndEnable()
        setupRecyclerView()
        setupButtonListeners()
        observeScannedDevices()
    }

    private fun setupRecyclerView() {
        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }
    }

    // Function to check for Bluetooth permissions and enable Bluetooth
    private fun checkBluetoothPermissionsAndEnable() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the necessary permissions if they aren't granted
            requestBluetoothPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            // Permissions are granted, enable Bluetooth
            enableBluetooth()
        }
    }

    // Function to launch Bluetooth enable intent
    private fun enableBluetooth() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableLauncher.launch(enableBtIntent)
            } else {
                Toast.makeText(this, "Bluetooth is already enabled.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Bluetooth CONNECT permission is required to enable Bluetooth.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLaunchers() {
        bluetoothEnableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth is required for scanning", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startBluetoothScan()
            } else {
                Toast.makeText(this, "Location permission is required for Bluetooth scanning", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupButtonListeners() {
        binding.startScanButton.setOnClickListener {
            checkAndRequestBluetoothAndPermissions()
        }

        binding.stopScanButton.setOnClickListener {
            stopBluetoothScan()
        }

        binding.sortByNameButton.setOnClickListener {
            currentSortOption = SortOption.NAME
            sortDevices()
        }

        binding.sortByMacButton.setOnClickListener {
            currentSortOption = SortOption.MAC_ADDRESS
            sortDevices()
        }

        binding.sortByScanTimeButton.setOnClickListener {
            currentSortOption = SortOption.SCAN_TIME
            sortDevices()
        }
    }

    private fun observeScannedDevices() {
        scannedDevices.observe(this) { devices ->
            sortDevices()
        }
    }

    private fun checkAndRequestBluetoothAndPermissions() {
        if (hasBluetoothPermissions()) {
            startBluetoothScan()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothScan() {
        if (hasBluetoothPermissions()) {
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            bluetoothLeScanner.startScan(scanCallback)
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth permissions are required for scanning.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBluetoothScan() {
        if (hasBluetoothPermissions()) {
            bluetoothLeScanner.stopScan(scanCallback)
            Toast.makeText(this, "Scan stopped", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth permissions are required to stop scanning.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("InlinedApi")
    private fun hasBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun sortDevices() {
        if (hasBluetoothPermissions()) {
            val devices = scannedDevices.value ?: return
            deviceAdapter.submitList(
                when (currentSortOption) {
                    SortOption.NAME -> devices.sortedBy { it.device.name }
                    SortOption.MAC_ADDRESS -> devices.sortedBy { it.device.address }
                    SortOption.SCAN_TIME -> devices.sortedByDescending { it.timestampNanos }
                }
            )
        } else {
            checkAndRequestBluetoothAndPermissions()
            Toast.makeText(this, "Bluetooth permissions are required to sort devices.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeviceDetails(device: BluetoothDevice) {
        val intent = Intent(this, DeviceDetailActivity::class.java).apply {
            putExtra("bluetoothDevice", device)
        }
        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    private fun toggleConnection(device: BluetoothDevice) {
        val deviceAddress = device.address
        val isConnected = connectedDevices.containsKey(deviceAddress)

        if (isConnected) {
            // Disconnect
            val gatt = connectedDevices.remove(deviceAddress)
            gatt?.disconnect()
            gatt?.close()
            deviceAdapter.updateDeviceStatus(deviceAddress, false)
            Toast.makeText(this, "Disconnected from: ${device.name}", Toast.LENGTH_SHORT).show()
        } else {
            // Connect
            val gattConnection = device.connectGatt(this, false, gattCallback)
            connectedDevices[deviceAddress] = gattConnection
            deviceAdapter.updateDeviceStatus(deviceAddress, true)
            Toast.makeText(this, "Connecting to: ${device.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val updatedResults = scannedDevices.value ?: mutableListOf()
            if (!updatedResults.any { it.device.address == result.device.address }) {
                updatedResults.add(result)
                scannedDevices.postValue(updatedResults)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(this@MainActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    deviceAdapter.updateDeviceStatus(gatt.device.address, true)
                    Toast.makeText(this@MainActivity, "Connected to ${gatt.device.name}", Toast.LENGTH_SHORT).show()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    deviceAdapter.updateDeviceStatus(gatt.device.address, false)
                    Toast.makeText(this@MainActivity, "Disconnected from ${gatt.device.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}