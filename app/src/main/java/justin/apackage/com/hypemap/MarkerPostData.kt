package justin.apackage.com.hypemap

data class MarkerPostData (
    val locationId: Int,
    val name: String,
    var latitude: Double?,
    var longitude: Double?,
    val postUrl: String)