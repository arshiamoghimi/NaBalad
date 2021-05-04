package ir.sambal.nabalad.maps

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import ir.sambal.nabalad.ThemeHelper

class Marker(mapView: MapView, map: MapboxMap, style: Style, private val options: SymbolOptions) {
    private val symbolManager: SymbolManager = SymbolManager(mapView, map, style)
    private var symbol: Symbol? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var text: String = ""


    init {
        symbolManager.iconAllowOverlap = true
    }

    fun hide() {
        synchronized(this) {
            symbolManager.delete(symbol)
            symbol = null
        }
    }

    fun show() {
        val mode = ThemeHelper.currentMode
        var color = "black"
        if (mode == ThemeHelper.DARK_MODE) {
            color = "white"
        }
        val symbolOptions = options
            .withLatLng(LatLng(latitude, longitude))
            .withTextField(text)
            .withTextColor(color)
        synchronized(this) {
            if (symbol == null) {
                symbol = symbolManager.create(symbolOptions)
            }
        }
    }

    fun setLatLng(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
        symbol?.let {
            it.latLng = LatLng(latitude, longitude)
            symbolManager.update(it)
        }
    }

    fun setText(text: String) {
        this.text = text
        symbol?.let {
            it.textField = text
            symbolManager.update(it)
        }
    }

}