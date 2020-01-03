package justin.apackage.com.hypemap.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class HypeMapViewModel (application: Application) : AndroidViewModel(application) {

    private val instaRepo: HypeMapRepository =
        HypeMapRepository(application)
    lateinit var mMap: GoogleMap
    private var userMarkers: Map<String, MutableList<Marker>?> = mutableMapOf()

    fun getPostLocations(): LiveData<List<PostLocation>?> {
        val posts: List<PostLocation>? = instaRepo.getPosts().value
        if (posts == null || posts.isEmpty()) {
            instaRepo.updatePosts()
        }
        return instaRepo.getPosts()
    }

    fun addUser(userName: String) {
        instaRepo.addUser(userName)
    }

    fun getUsers(): LiveData<List<User>> {
        return instaRepo.getUsers()
    }

    fun getUserMarkers(): Map<String, MutableList<Marker>?> {
        return userMarkers
    }
}