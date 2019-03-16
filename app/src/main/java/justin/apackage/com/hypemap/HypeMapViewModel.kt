package justin.apackage.com.hypemap

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import com.google.android.gms.maps.GoogleMap

class HypeMapViewModel (application: Application) : AndroidViewModel(application) {

    private val instaRepo: InstagramRepository = InstagramRepository(application)
    lateinit var mMap: GoogleMap

    fun getPosts(): LiveData<List<Post>?> {
        return instaRepo.getPosts()
    }

    fun showUserMarkers(userName: String, visible: Boolean) {
        instaRepo.showUserMarkers(userName, visible)
    }

    fun filterMarkersByTime(timeThreshold: Long) {
        instaRepo.filterMarkersByTime(timeThreshold)
    }

    fun updateInstaData() {
        instaRepo.updatePostsPeriodically()
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