package justin.apackage.com.hypemap

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class HypeMapViewModel : ViewModel() {

    private val instaRepo: InstagramRepository = InstagramRepository()
    lateinit var mMap: GoogleMap

    fun getLatestPost(): MutableLiveData<Pair<String, MarkerPostData>> {
        return instaRepo.getLatestPost()
    }

    fun getLocationMap(): Map<String, LatLng> {
        return instaRepo.getLocationMap()
    }

    fun hideUserMarkers(userName: String) {
        instaRepo.hideUserMarkers(userName)
    }

    fun addMarker(userName: String, mkr: Marker) {
        instaRepo.addMarker(userName, mkr)
    }

    // These methods will get repository to update
    // Observers on the getUsersMarkers() method will be
    // notified of new changes to it

    fun updateInstaData() {
        instaRepo.updatePosts()
    }

    fun addUser(userName: String) {
        instaRepo.addUser(userName)
    }

    fun removeAll() {
        instaRepo.removeAll()
    }

}