package justin.apackage.com.hypemap.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.GoogleMap

class HypeMapViewModel (application: Application) : AndroidViewModel(application) {

    private val instaRepo: HypeMapRepository =
        HypeMapRepository(application)
    lateinit var mMap: GoogleMap

    fun getPostLocations(): LiveData<List<PostLocation>?> {
        return instaRepo.getPosts()
    }

    fun showUserMarkers(userName: String, visible: Boolean) {
        instaRepo.showUserMarkers(userName, visible)
    }

    fun filterMarkersByTime(timeThreshold: Long) {
        instaRepo.filterMarkersByTime(timeThreshold)
    }

    fun updateInstaData() {
        instaRepo.updatePosts()
    }

    fun addUser(userName: String) {
        instaRepo.addUser(userName)
    }

    fun getUsers(): LiveData<List<User>> {
        return instaRepo.getUsers()
    }

    fun removeAll() {
        instaRepo.removeAll()
    }

    fun removeUser(userName: String) {
        instaRepo.removeUser(userName)
    }

}