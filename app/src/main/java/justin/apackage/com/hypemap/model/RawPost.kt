package justin.apackage.com.hypemap.model

/**
 * A data class to store raw post data
 *
 * @author Justin Wong
 */
data class RawPost(var id: String,
                   var userName: String,
                   var locationName: String,
                   var locationId: String,
                   var postUrl: String,
                   var linkUrl: String,
                   var caption: String,
                   var timestamp: Long)