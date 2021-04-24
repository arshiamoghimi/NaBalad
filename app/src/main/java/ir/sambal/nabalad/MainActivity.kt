package ir.sambal.nabalad

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.droidnet.DroidListener
import com.droidnet.DroidNet
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style


class MainActivity : AppCompatActivity(), DroidListener {
    private var mapView: MapView? = null

    private var mDroidNet: DroidNet? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DroidNet.init(this)
        mDroidNet = DroidNet.getInstance()
        mDroidNet?.addInternetConnectivityListener(this)

        askRequiredPermissions()

        Mapbox.getInstance(this, BuildConfig.MAPBOX_PUBLIC_KEY)

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {

            }

        }
    }

    private fun askRequiredPermissions() {
        val permissions: Array<String> = getNotGivenPermissions().toTypedArray()
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions, 0)
        }
    }

    private fun getNotGivenPermissions(): MutableList<String> {
        var permissions = mutableListOf<String>()
        if (!isPermissionGiven(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (!isPermissionGiven(android.Manifest.permission.INTERNET)) {
            permissions.add(android.Manifest.permission.INTERNET)
        }
        return permissions
    }

    private fun isPermissionGiven(permission: String): Boolean {
        val result = ActivityCompat.checkSelfPermission(this.applicationContext, permission)
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
        DroidNet.getInstance().removeAllInternetConnectivityChangeListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        mDroidNet?.removeInternetConnectivityChangeListener(this)
    }

    override fun onInternetConnectivityChanged(isConnected: Boolean) {
        if (!isConnected) {
            TODO("Ask user for internet")
        }
    }
}