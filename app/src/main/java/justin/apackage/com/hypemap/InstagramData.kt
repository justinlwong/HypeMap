package justin.apackage.com.hypemap

import com.google.android.gms.maps.model.LatLng

data class InstagramData (
    val usersPostData: MutableMap<String, UserPosts>,
    val locationMap: MutableMap<String, LatLng>
)