package justin.apackage.com.hypemap

data class UserPosts (
    val profileUrl: String,
    val posts: Map<String, MarkerPostData>)
