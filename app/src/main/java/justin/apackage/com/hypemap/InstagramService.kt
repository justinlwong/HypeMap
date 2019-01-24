package justin.apackage.com.hypemap

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface InstagramService {
    @GET("/{user_name}")
    fun getUserPage(@Path("user_name") userName: String) : Observable<ResponseBody>

    @GET("/explore/locations/{location_id}/?__a=1")
    fun getCoordinates(@Path("location_id") locationId: String) : Observable<ResponseBody>
}