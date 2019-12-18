package justin.apackage.com.hypemap.model

/**
 * A wrapper class to store raw user data and posts
 *
 * @author Justin Wong
 */
data class UserWrapper(val user: User,
                       val posts: List<RawPost>)