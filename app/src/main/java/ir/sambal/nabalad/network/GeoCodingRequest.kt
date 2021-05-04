package ir.sambal.nabalad.network

import android.util.Log
import ir.sambal.nabalad.BuildConfig
import ir.sambal.nabalad.Target
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


object GeoCodingRequest {

    fun requestData(data: String): ArrayList<Target> {
        val results = arrayListOf<Target>()

        val urlBuilder = HttpUrl.parse(
            "https://api.mapbox.com/geocoding/v5/mapbox.places/".plus(
                data
            ).plus(".json?access_token=").plus(BuildConfig.MAPBOX_PUBLIC_KEY)
        )
            ?.newBuilder()
        val url = urlBuilder?.build().toString()
        val request: Request = Request.Builder().url(url)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call?, response: Response) {
                if (response.code() >= 400) {
                    return
                }
                val myResponse: String = response.body()!!.string()
                try {
                    val json = JSONObject(myResponse).getJSONArray("features")

                    for (i in 0 until json.length()) {
                        val item = json.getJSONObject(i)
                        val name = item.getString("place_name")
                        val center =
                            item.get("center").toString().removeSurrounding("[", "]").split(",")
                                .map { it.toDouble() }
                        results.add(Target(name, center[0], center[1]))
                    }

                } catch (ignored: JSONException) {
                }
            }
        })
        return results
    }
}