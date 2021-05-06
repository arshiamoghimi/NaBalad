package ir.sambal.nabalad

import com.mapbox.mapboxsdk.geometry.LatLng

class Target(val name: String, val longitude: Double, val latitude: Double) {

    fun getLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

}