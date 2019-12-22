package justin.apackage.com.hypemap.ui

/**
 * An interface for authentication
 *
 * @author Justin Wong
 */
interface AuthenticationListener {
    fun onCookieReceived(cookie: String)
    fun onTokenReceived(accessToken: String)
}