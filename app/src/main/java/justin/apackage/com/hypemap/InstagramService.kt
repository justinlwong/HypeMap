package justin.apackage.com.hypemap

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface InstagramService {
    @GET("/{user_name}")
    fun getUserPage(@Path("user_name") userName: String) : Call<ResponseBody>
}