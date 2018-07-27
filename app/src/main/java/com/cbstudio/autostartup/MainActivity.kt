package com.cbstudio.autostartup

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.core.widget.toast
import com.google.android.gms.ads.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


const val DEBUG_TAG = "cb_debug"


class MainActivity : AppCompatActivity() {


    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private lateinit var mInterstitialAd: InterstitialAd

    private val mDeviceMap = HashMap<String, BluetoothDevice>()

    private val mApplicationList = ArrayList<ApplicationDescription>()

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mServiceBinder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mServiceBinder = service as MainService.Binder
        }
    }

    private var mServiceBinder: MainService.Binder? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupAd()

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            mBluetoothAdapter = bluetoothAdapter

            if (!mBluetoothAdapter.isEnabled) {
                toast(getString(R.string.open_bluetooth))
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(intent)

            } else {
                initBluetoothDeviceListSource().subscribe()
                initApplicationListSource().subscribe()
                setupDeviceListView()
            }

            val serviceIntent = Intent(this, MainService::class.java)
            startService(serviceIntent)
            bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun setupAd() {
        //        ad_view.adSize = AdSize.BANNER
        //        ad_view.adUnitId = "ca-app-pub-7147633290594636/8980134535"
        MobileAds.initialize(this, "ca-app-pub-7147633290594636~9171706224")

        val adView = AdView(this)
        adView.adSize = AdSize.BANNER
        adView.adUnitId = getString(R.string.banner_ad_unit_id)
        ll_main.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
//        adView.loadAd(AdRequest.Builder().addTestDevice("FA2D5325B1C6818FE2EA2FCB18578281").build())
        //        ad_view.loadAd(AdRequest.Builder().addTestDevice("FA2D5325B1C6818FE2EA2FCB18578281").build())


        // for interstitial AD test
//        btn_test.setOnClickListener {
//            mInterstitialAd = InterstitialAd(this)
//            mInterstitialAd.adUnitId = "ca-app-pub-7147633290594636/461912165"
//            mInterstitialAd.loadAd(AdRequest.Builder().addTestDevice("FA2D5325B1C6818FE2EA2FCB18578281").build())
//            mInterstitialAd.show()
//        }
    }

    private fun setupDeviceListView() {
        lv_devices.onItemClickListener = OnItemClickListener { adapter, _, position, _ ->
            val deviceName = adapter.getItemAtPosition(position) as String
            val device = mDeviceMap[deviceName]

            AlertDialog.Builder(this)
                    .setTitle(R.string.chose_bluetooth_connected_start_app)
                    .setItems(
                            mApplicationList.map { it.name }.toTypedArray(),
                            { _, which ->
                                Log.d(DEBUG_TAG, "your chose ${mApplicationList[which].name}")
                                device?.let {
                                    mServiceBinder?.addActionDevice(it, mApplicationList[which])
                                }
                            })
                    .create()
                    .show()

        }
    }

    private fun initBluetoothDeviceListSource(): Observable<BluetoothDevice> {
        val arrayList = ArrayList<String>()
        return mBluetoothAdapter.bondedDevices.toObservable()
                .observeOn(Schedulers.io())
                .doOnNext {
                    val name = it.name
                    Log.d(DEBUG_TAG, name)
                    arrayList.add(name)
                    mDeviceMap[name] = it
                }.observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    lv_devices.adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_list_item_1,
                            arrayList
                    )
                }
    }

    private fun initApplicationListSource(): Observable<ArrayList<ApplicationDescription>> {
        return packageManager.getInstalledPackages(0).toObservable()
                .observeOn(Schedulers.io())
                .map { packageInfo ->
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageInfo.packageName)
                    launchIntent?.let {
                        val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                        val appDesc = ApplicationDescription(
                                appName,
                                packageInfo.packageName,
                                packageInfo.applicationInfo.loadIcon(packageManager),
                                it
                        )
                        mApplicationList.add(appDesc)
                    }
                    mApplicationList
                }.doOnNext { mApplicationList.sortBy { it.name } }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
    }
}


