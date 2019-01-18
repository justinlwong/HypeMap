package justin.apackage.com.hypemap

data class MarkerPostData (
    val user: String,
    val locationId: String,
    val name: String,
    var latitude: Double?,
    var longitude: Double?,
    val postUrl: String,
    val linkUrl: String,
    val caption: String)