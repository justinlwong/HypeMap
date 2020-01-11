package justin.apackage.com.hypemap.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class HypeMapViewModel (application: Application) : AndroidViewModel(application) {

    private val instaRepo: HypeMapRepository = HypeMapRepository(application)
    lateinit var mMap: GoogleMap
    private val infoMkrsMap: MutableMap<String, Marker> = mutableMapOf()

    fun getPosts(): LiveData<List<Post>> {
        return instaRepo.getPosts()
    }

    fun getPostLocationsBlocking(): List<Post>? {
        return instaRepo.getPostsBlocking()
    }

    fun addUser(userName: String) {
        instaRepo.addUser(userName)
    }

    fun getUsers(): LiveData<List<User>> {
        return instaRepo.getUsers()
    }

    fun getUser(userId: String): User? {
        return instaRepo.getUser(userId)
    }

    fun getInfoMarkersMap(): MutableMap<String, Marker> {
        return infoMkrsMap
    }

    fun getLocation(locationId: String): Location? {
        return instaRepo.getLocation(locationId)
    }
}