package ir.sambal.nabalad

import android.location.Location

class Target(val name: String, val longitude: Double, val latitude: Double) {

    fun getLocation(): Location {
        val location = Location("")
        location.latitude = latitude
        location.longitude = longitude
        return location
    }

}