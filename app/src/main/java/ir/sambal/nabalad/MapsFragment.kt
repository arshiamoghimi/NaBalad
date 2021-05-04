package ir.sambal.nabalad

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.droidnet.DroidNet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import ir.sambal.nabalad.maps.Marker
import java.lang.ref.WeakReference


class MapsFragment : Fragment() {
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var locationEngine: LocationEngine? = null
    private val locationCallback = LocationChangeListeningActivityLocationCallback(this)
    private var marker: Marker? = null
    private var lastLocation: Location? = null
        set(value) {
            field = value
            if (watchLocation && value != null) {
                flyToLocation(value)
            }
        }
    private var watchLocation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(context!!, BuildConfig.MAPBOX_PUBLIC_KEY)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                enableLocationComponent(style)
                context?.let {
                    ResourcesCompat.getDrawable(it.resources, R.drawable.marker_black, it.theme)
                        ?.let { drawable ->
                            style.addImage(MARKER_IMAGE, drawable)
                        }
                }
                marker = Marker(
                    mapView!!, mapboxMap, style,
                    SymbolOptions()
                        .withIconImage(MARKER_IMAGE)
                        .withIconAnchor("bottom")
                        .withIconSize(2.0F)
                        .withTextOffset(arrayOf(0F, 0.4F))
                        .withTextSize(18.0F)
                )
            }


            mapboxMap.addOnMoveListener(object : MapboxMap.OnMoveListener {
                override fun onMoveBegin(detector: MoveGestureDetector) {
                    watchLocation = false
                }

                override fun onMove(detector: MoveGestureDetector) {

                }

                override fun onMoveEnd(detector: MoveGestureDetector) {

                }
            })
        }

        val gpsFabButton = view.findViewById<FloatingActionButton>(R.id.current_location_fab)
        gpsFabButton.setOnClickListener {
            lastLocation?.let { it1 -> flyToLocation(it1) }
            watchLocation = true
        }
    }

    fun locationPermissionGiven() {
        mapboxMap?.style?.let {
            enableLocationComponent(it)
        }
    }

    @SuppressLint("MissingPermission")
    fun enableLocationComponent(loadedMapStyle: Style) {
        if (!PermissionsManager.areLocationPermissionsGranted(this.context)) {
            return
        }
        val locationComponent = mapboxMap?.locationComponent
        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(this.context!!, loadedMapStyle)
                .useDefaultLocationEngine(false)
                .build()
        locationComponent?.apply {
            activateLocationComponent(locationComponentActivationOptions)
            isLocationComponentEnabled = true
            renderMode = RenderMode.COMPASS
        }
        initLocationEngine()
    }


    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        this.context?.let {
            locationEngine = LocationEngineProvider.getBestLocationEngine(it)
            val request = LocationEngineRequest.Builder(3_000L)
                .setPriority(LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxWaitTime(30_000L).build()
            locationEngine?.apply {
                requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                getLastLocation(locationCallback)
            }
        }
    }

    private class LocationChangeListeningActivityLocationCallback internal constructor(fragment: MapsFragment) :
        LocationEngineCallback<LocationEngineResult?> {
        private val fragmentWeakReference: WeakReference<MapsFragment> = WeakReference(fragment)

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        override fun onSuccess(result: LocationEngineResult?) {
            val fragment = fragmentWeakReference.get()
            if (fragment != null) {
                if (result == null) {
                    fragment.marker?.hide()
                    fragment.lastLocation = null
                    return
                }
                fragment.lastLocation = result.lastLocation
                result.lastLocation?.let { location ->
                    fragment.marker?.let {
                        it.setLatLng(location.latitude, location.longitude)
                        if (System.currentTimeMillis() - location.time > 20_000) {
                            it.setText("")
                        } else {
                            it.setText("%.2f m/s".format(location.speed))
                        }
                        it.show()
                    }
                }
            }
        }

        override fun onFailure(exception: Exception) {
            val fragment = fragmentWeakReference.get()
            Toast.makeText(
                fragment?.context, exception.localizedMessage,
                Toast.LENGTH_SHORT
            ).show()
        }

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
    }

    private fun flyToLocation(location: Location) {
        mapboxMap?.let {
            val cameraPosition = it.cameraPosition

            it.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(cameraPosition.zoom.coerceAtLeast(17.0))
                        .build()
                ), 500
            )
        }
    }


    companion object {

        const val MARKER_IMAGE = "marker-image"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MapsFragment.
         */
        @JvmStatic
        fun newInstance() =
            MapsFragment().apply {

            }
    }
}