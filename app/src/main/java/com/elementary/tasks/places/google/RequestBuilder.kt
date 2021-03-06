package com.elementary.tasks.places.google

import com.elementary.tasks.core.network.PlacesApi
import com.elementary.tasks.core.network.RetrofitBuilder
import com.elementary.tasks.core.network.places.PlacesResponse
import com.elementary.tasks.core.utils.Module
import retrofit2.Call
import java.util.*

object RequestBuilder {

    private val language: String
        get() {
            val locale = Locale.getDefault()
            return locale.language
        }

    val key: String
        get() {
            return if (Module.isPro) {
                "AIzaSyD80IRgaabOQoZ_mRP_RL36CJKeDO96yKw"
            } else {
                "AIzaSyCMrJF6bn1Mt6n2uyLLLN85h-PGAtotT3Q"
            }
        }

    fun getNearby(lat: Double, lng: Double, name: String): Call<PlacesResponse> {
        val req = name.replace("\\s+".toRegex(), "+")
        val params = LinkedHashMap<String, String>()
        params["location"] = "$lat,$lng"
        params["radius"] = "50000"
        params["name"] = req
        params["language"] = language
        params["key"] = key
        return RetrofitBuilder.placesApi.getNearbyPlaces(params)
    }

    fun getSearch(name: String): Call<PlacesResponse> {
        val req = name.replace("\\s+".toRegex(), "+")
        var url = PlacesApi.BASE_URL + "textsearch/json?"
        url += "query=$req"
        url += "&inputtype=textquery"
        url += "&language=$language"
        url += "&key=$key"
        return RetrofitBuilder.placesApi.getPlaces(url)
    }
}
