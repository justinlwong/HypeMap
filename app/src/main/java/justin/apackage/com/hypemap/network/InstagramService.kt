package justin.apackage.com.hypemap.network

import io.reactivex.Single
import justin.apackage.com.hypemap.model.Location
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * A retrofit service for requesting user and location data
 *
 * @author Justin Wong
 */
interface InstagramService {
    @GET("/{user_name}?__a=1")
    fun getUserPage(@Header("Cookie") cookie: String, @Path("user_name") userName: String) : Single<Response<ResponseBody>>

    @GET("/explore/locations/{location_id}/?__a=1")
    fun getCoordinates(@Header("Cookie") cookie: String, @Path("location_id") locationId: String) : Single<Response<ResponseBody>>
}