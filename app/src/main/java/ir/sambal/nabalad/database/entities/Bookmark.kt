package ir.sambal.nabalad.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double
) {
    fun latLng(): String {
        val latString = "%.4f".format(latitude)
        val lngString = "%.4f".format(longitude)
        return "$latString, $lngString"
    }
}