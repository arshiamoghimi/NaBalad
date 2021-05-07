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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.droidnet.DroidNet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
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
import ir.sambal.nabalad.database.AppDatabase
import ir.sambal.nabalad.database.entities.Bookmark
import ir.sambal.nabalad.maps.Marker
import ir.sambal.nabalad.network.GeoCodingRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference


class MapsFragment(
    private var db: AppDatabase,
    private var changePageCallback: (Int) -> Unit
) : Fragment(),
    MaterialSearchBar.OnSearchActionListener {
    private var toShowBookmark: Bookmark? = null
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var searchBar: MaterialSearchBar? = null
    private lateinit var searchResults: ArrayList<Target>
    private var locationEngine: LocationEngine? = null
    private val locationCallback = LocationChangeListeningActivityLocationCallback(this)
    private var gpsMarker: Marker? = null
    private var addMarker: Marker? = null
    private var bookmarkMarker: Marker? = null
    private var mapStyle = Style.LIGHT
    private var lastLocation: Location? = null
        set(value) {
            field = value
            if (watchLocation && !showingBottomModal && value != null) {
                flyToLocation(LatLng(value.latitude, value.longitude), GPS_ZOOM)
            }
        }
    private var watchLocation = true
    private var showingBottomModal = false
    private lateinit var bookmarkBottomSheet: LinearLayout
    private lateinit var bookmarkBottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bookmarkCancelButton: Button
    private lateinit var bookmarkSaveButton: Button
    private lateinit var bookmarkTextView: TextView
    private lateinit var bookmarkName: TextInputEditText


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
                            style.addImage(GPS_MARKER_IMAGE, drawable)
                        }
                    ResourcesCompat.getDrawable(it.resources, R.drawable.marker_add, it.theme)
                        ?.let { drawable ->
                            style.addImage(ADD_BOOKMARK_MARKER_IMAGE, drawable)
                        }
                    ResourcesCompat.getDrawable(it.resources, R.drawable.marker_blue, it.theme)
                        ?.let { drawable ->
                            style.addImage(BOOKMARK_MARKER_IMAGE, drawable)
                        }
                }
                mapView?.let {
                    gpsMarker = Marker(
                        it, mapboxMap, style,
                        SymbolOptions()
                            .withIconImage(GPS_MARKER_IMAGE)
                            .withIconAnchor("bottom")
                            .withIconSize(1.5F)
                            .withTextOffset(arrayOf(0F, 0.4F))
                            .withTextSize(18.0F)
                    )
                    addMarker = Marker(
                        it, mapboxMap, style,
                        SymbolOptions()
                            .withIconImage(ADD_BOOKMARK_MARKER_IMAGE)
                            .withIconAnchor("bottom")
                            .withIconSize(2.0F)
                    )
                    bookmarkMarker = Marker(
                        it, mapboxMap, style,
                        SymbolOptions()
                            .withIconImage(BOOKMARK_MARKER_IMAGE)
                            .withIconAnchor("bottom")
                            .withIconSize(2.0F)
                            .withTextOffset(arrayOf(0F, 0.4F))
                            .withTextSize(18.0F)
                    )
                }

                showBookmark()
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

            mapboxMap.addOnMapLongClickListener { point ->
                showingBottomModal = true
                addMarker?.setLatLng(point.latitude, point.longitude)
                flyToLocation(LatLng(point.latitude, point.longitude))
                addMarker?.show()
                setBookmarkLocationTextView()
                bookmarkBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                true
            }
        }

        val gpsFabButton = view.findViewById<FloatingActionButton>(R.id.current_location_fab)
        gpsFabButton.setOnClickListener {
            lastLocation?.let { value ->
                flyToLocation(
                    LatLng(value.latitude, value.longitude),
                    GPS_ZOOM
                )
            }
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

        bookmarkBottomSheet = view.findViewById(R.id.bookmarkBottomSheet)
        bookmarkCancelButton = view.findViewById(R.id.bookmarkCancelButton)
        bookmarkSaveButton = view.findViewById(R.id.bookmarkSaveButton)
        bookmarkTextView = view.findViewById(R.id.bookmarkLocation)
        bookmarkName = view.findViewById(R.id.bookmarkName)
        bookmarkBottomSheetBehavior = BottomSheetBehavior.from(bookmarkBottomSheet)

        bookmarkBottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // handle onSlide
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        showingBottomModal = false
                        addMarker?.hide()
                        bookmarkName.text?.clear()
                        activity?.let { hideKeyboard(it) }
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        showingBottomModal = true
                        addMarker?.let {marker ->
                            if(!marker.isVisible()) {
                                mapboxMap?.let { map ->
                                    marker.setLatLng(
                                        map.cameraPosition.target.latitude,
                                        map.cameraPosition.target.longitude
                                    )
                                    marker.show()
                                }
                            }
                        }
                        setBookmarkLocationTextView()
                    }
                }
            }
        })

        bookmarkCancelButton.setOnClickListener {
            if (bookmarkBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                bookmarkBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        bookmarkSaveButton.setOnClickListener {
            saveBookmark()
        }
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
        flyToLocation(searchResults[position].getLatLng(), SEARCH_ZOOM)
        activity?.let { hideKeyboard(it) }
        searchBar?.closeSearch()
        if (searchBar?.isSuggestionsVisible!!) {
            searchBar?.hideSuggestionsList()
        }
        searchBar?.lastSuggestions = listOf<String>()
        searchResults = arrayListOf<Target>()
        watchLocation = false
    }

    private fun saveBookmark() {
        addMarker?.let {
            val lat = it.getLatitude()
            val lon = it.getLongitude()
            val name = bookmarkName.text.toString()
            bookmarkName.text?.clear()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                db.bookmarkDao().add(Bookmark(name = name, latitude = lat, longitude = lon))
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    bookmarkBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    Toast.makeText(
                        context,
                        getString(R.string.bookmark_is_added_msg),
                        Toast.LENGTH_LONG
                    ).show()
                    changePageCallback(R.id.bookmark_menu_item)
                }
            }
        }
    }

    fun setBookmarkLocationTextView() {
        val lat: Double? = addMarker?.getLatitude()
        val lon: Double? = addMarker?.getLongitude()
        bookmarkTextView.text = "(%.6f, %.6f)".format(lat, lon)
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
                    fragment.gpsMarker?.hide()
                    fragment.lastLocation = null
                    return
                }
                fragment.lastLocation = result.lastLocation
                result.lastLocation?.let { location ->
                    fragment.gpsMarker?.let {
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

    private fun flyToLocation(latLng: LatLng, zoom: Double? = null) {
        mapboxMap?.let {
            val cameraPosition = it.cameraPosition

            it.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(latLng)
                        .zoom(zoom ?: cameraPosition.zoom)
                        .build()
                ), 500
            )
        }
    }

    fun showBookmark(bookmark: Bookmark) {
        toShowBookmark = bookmark
    }

    private fun showBookmark() {
        toShowBookmark?.let { bookmark ->
            watchLocation = false
            bookmarkMarker?.let {
                it.setLatLng(bookmark.latitude, bookmark.longitude)
                it.setText(bookmark.name)
                it.show()
            }
            flyToLocation(LatLng(bookmark.latitude, bookmark.longitude), BOOKMARK_ZOOM)
            toShowBookmark = null
        }
    }


    companion object {

        const val GPS_MARKER_IMAGE = "gps-marker-image"
        const val ADD_BOOKMARK_MARKER_IMAGE = "add-marker-image"
        const val BOOKMARK_MARKER_IMAGE = "blue-marker-image"
        const private val SPEECH_REQUEST_CODE = 0
        const private val GPS_ZOOM = 17.0
        const private val BOOKMARK_ZOOM = 15.0
        const private val SEARCH_ZOOM = 10.0
    }
}