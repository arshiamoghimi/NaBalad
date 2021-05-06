package ir.sambal.nabalad

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.droidnet.DroidNet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mancj.materialsearchbar.MaterialSearchBar
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter
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
import ir.sambal.nabalad.network.GeoCodingRequest
import java.lang.ref.WeakReference


class MapsFragment : Fragment(), MaterialSearchBar.OnSearchActionListener {
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var searchBar: MaterialSearchBar? = null
    private lateinit var searchResults: ArrayList<Target>
    private var locationEngine: LocationEngine? = null
    private val locationCallback = LocationChangeListeningActivityLocationCallback(this)
    private var marker: Marker? = null
    private var mapStyle = Style.LIGHT
    private var lastLocation: Location? = null
        set(value) {
            field = value
            if (watchLocation && value != null) {
                flyToLocation(value, GPS_ZOOM)
            }
        }
    private var watchLocation = true
    private val SPEECH_REQUEST_CODE = 0
    private val GPS_ZOOM = 17.0
    private val SEARCH_ZOOM = 10.0


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

        when (ThemeHelper.currentMode) {
            ThemeHelper.DARK_MODE -> {
                mapStyle = Style.DARK
            }
            ThemeHelper.LIGHT_MODE -> {
                mapStyle = Style.LIGHT
            }
        }
        mapView = view.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            mapboxMap.setStyle(mapStyle) { style ->
                enableLocationComponent(style)
                context?.let {
                    ResourcesCompat.getDrawable(it.resources, R.drawable.marker_red, it.theme)
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
            lastLocation?.let { it1 -> flyToLocation(it1, GPS_ZOOM) }
            watchLocation = true
        }

        val searchBar = view.findViewById<MaterialSearchBar>(R.id.search_bar)
        searchBar.setOnSearchActionListener(this)
        this.searchBar = searchBar
//
        var textWatcher = SearchBarTextWatcher(this)
//
        searchBar.addTextChangeListener(textWatcher)
        searchBar.setSuggestionsClickListener(onSuggestionClickListener(this))
        //TODO: optional
        //lastSearches = loadSearchSuggestionFromDisk();
        // searchBar.setLastSuggestions(lastSearches);
        //Inflate menu and setup OnMenuItemClickListener
    }

    override fun onSearchStateChanged(enabled: Boolean) {
    }

    override fun onSearchConfirmed(text: CharSequence?) {
        fun onComplete() {
            gotoSearchedLocation(0)
        }

        getSearchResults(text.toString(), ::onComplete)
    }

    fun getSearchResults(text: String?, onCompleted: (() -> Unit)?) {
        val handler: Handler = Handler(Looper.getMainLooper());

        fun callback(results: ArrayList<Target>) {
            handler.post(java.lang.Runnable {
                val resultsNames = results.map { target: Target -> target.name }
                searchBar?.lastSuggestions = resultsNames
                if (!searchBar?.isSuggestionsVisible!!) {
                    searchBar?.showSuggestionsList()
                }

                searchResults = results
                onCompleted?.let { it() }
            })

        }
        if (text != null) {
            GeoCodingRequest.requestData(text, ::callback)
        }
    }

    fun gotoSearchedLocation(position: Int) {
        flyToLocation(searchResults[position].getLocation(), SEARCH_ZOOM)
        activity?.let { hideKeyboard(it) }
        searchBar?.closeSearch()
        if (searchBar?.isSuggestionsVisible!!) {
            searchBar?.hideSuggestionsList()
        }
        searchBar?.lastSuggestions = listOf<String>()
        searchResults = arrayListOf<Target>()
        watchLocation = false
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onButtonClicked(buttonCode: Int) {
//        Log.v("TEST", buttonCode.toString())
        when (buttonCode) {
            MaterialSearchBar.BUTTON_SPEECH -> displaySpeechRecognizer()
        }
    }

    private fun displaySpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        }
        startActivityForResult(intent, SPEECH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results!![0]
                }
            if (spokenText != null) {
                getSearchResults(spokenText, null)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
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


    class SearchBarTextWatcher(private var mapsFragment: MapsFragment) : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            return
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            return
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            mapsFragment.getSearchResults(s.toString(), null)
            return
        }

    }

    class onSuggestionClickListener(private var mapsFragment: MapsFragment): SuggestionsAdapter.OnItemViewClickListener {
        override fun OnItemDeleteListener(position: Int, v: View?) {
            return
        }

        override fun OnItemClickListener(position: Int, v: View?) {
            mapsFragment.gotoSearchedLocation(position)
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
        mapboxMap?.locationComponent?.onDestroy()
        mapView?.onDestroy()
        mapboxMap = null
    }

    private fun flyToLocation(location: Location, zoom: Double) {
        mapboxMap?.let {
            val cameraPosition = it.cameraPosition

            it.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(zoom)
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