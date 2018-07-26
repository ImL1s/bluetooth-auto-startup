package com.cbstudio.autostartup

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch


/**
 * Created by ImL1s on 2018/7/25.
 * Description:
 */

typealias BluetoothAddress = String

typealias ApplicationIntent = Intent

const val SHARE_PREFERENCES_NAME = "actionDevice"

const val SHARE_PREFERENCES_KEY_BLUETOOTH_ADDRESS = "actionDeviceAddress"

const val SHARE_PREFERENCES_KEY_APPLICATION_INTENT = "actionDeviceIntent"

class MainService : Service() {


    private val mActionDeviceMap = HashMap<BluetoothAddress, ApplicationIntent>()

    private val mBroadcastReceiver = BroadcastReceiver()

    private val binder = Binder()

    private lateinit var sharePreferences: SharedPreferences


    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sharePreferences = getSharedPreferences(SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE)
        restoreActionMap()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        registerBluetoothStateChangeReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @SuppressLint("ApplySharedPref")
    private fun restoreActionMap() {
        try {
            val bluetoothAddressSet = sharePreferences.getStringSet(SHARE_PREFERENCES_KEY_BLUETOOTH_ADDRESS, setOf()).toList()
            val applicationPackageSet = sharePreferences.getStringSet(SHARE_PREFERENCES_KEY_APPLICATION_INTENT, setOf()).toList()

            var index = 0
            bluetoothAddressSet.forEach { bluetoothAddress ->
                if (index >= applicationPackageSet.count()) return@forEach
                mActionDeviceMap[bluetoothAddress] = packageManager.getLaunchIntentForPackage(applicationPackageSet[index])
                ++index
            }
            Log.d(DEBUG_TAG, "action map count: ${mActionDeviceMap.count()}")
            Log.d(DEBUG_TAG, "----------")
            mActionDeviceMap.forEach { bluetoothAddress, appIntent ->
                Log.d(DEBUG_TAG, "bluetoothAddress: $bluetoothAddress appIntent: $appIntent")
            }
            Log.d(DEBUG_TAG, "----------")
        } catch (_: Exception) {
            sharePreferences.edit().remove(SHARE_PREFERENCES_KEY_APPLICATION_INTENT).commit()
            sharePreferences.edit().remove(SHARE_PREFERENCES_KEY_BLUETOOTH_ADDRESS).commit()
        }
    }

    private fun registerBluetoothStateChangeReceiver() {
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver, filter)
    }


    inner class BroadcastReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR)
            val stateChangeDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            Log.d(DEBUG_TAG, "bluetooth connection state has changed: device name is ${stateChangeDevice.name}, state is $state")
            when (state) {
                BluetoothAdapter.STATE_CONNECTED -> {
                    mActionDeviceMap.forEach { bluetoothAddress, applicationIntent ->
                        if (bluetoothAddress == stateChangeDevice.address) {
                            applicationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(applicationIntent)
                        }
                    }
                }
                BluetoothAdapter.STATE_DISCONNECTED -> {

                }
            }
        }
    }

    public inner class Binder : android.os.Binder() {

        fun addActionDevice(device: BluetoothDevice, applicationDesc: ApplicationDescription) {
            Log.d(DEBUG_TAG, "bind device with action, device ${device.name}(${device.address}) " +
                    "application name: ${applicationDesc.name}")

            mActionDeviceMap[device.address] = applicationDesc.intent!!
            writeToSharePreferences()
        }

        @SuppressLint("ApplySharedPref")
        private fun writeToSharePreferences() {
            val job = launch(CommonPool) {
                sharePreferences.edit()
                        .putStringSet(
                                SHARE_PREFERENCES_KEY_BLUETOOTH_ADDRESS,
                                mActionDeviceMap.keys.toSet()
                        )
                        .putStringSet(
                                SHARE_PREFERENCES_KEY_APPLICATION_INTENT,
                                mActionDeviceMap.values.map { it.`package` }
                                        .toSet()
                        ).commit()
            }
        }
    }
}