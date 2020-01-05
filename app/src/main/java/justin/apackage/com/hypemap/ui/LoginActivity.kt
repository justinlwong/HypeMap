package justin.apackage.com.hypemap.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import justin.apackage.com.hypemap.HypeMapConstants.Companion.HYPEMAP_SHARED_PREF
import justin.apackage.com.hypemap.HypeMapConstants.Companion.INSTAGRAM_ACCESS_TOKEN
import justin.apackage.com.hypemap.HypeMapConstants.Companion.INSTAGRAM_COOKIE_KEY

/**
 * A login activity to start instagram session
 *
 * @author Justin Wong
 */
class LoginActivity : AppCompatActivity(), AuthenticationListener {
    companion object {
        private const val TAG = "LoginActivity"
    }

    private val preferences: SharedPreferences
            by lazy{ applicationContext.getSharedPreferences(HYPEMAP_SHARED_PREF, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(justin.apackage.com.hypemap.R.layout.activity_login)
    }

    fun onClick(view: View) {
        val dialog = AuthenticationDialog(this)
        dialog.isCancelable = true
        dialog.show(supportFragmentManager, "AuthDialog")
    }

    override fun onCookieReceived(cookie: String) {
        Log.d(TAG, "Received cookie: $cookie")
        preferences.edit().putString(INSTAGRAM_COOKIE_KEY, cookie).apply()
        val myIntent = Intent(this, MapsActivity::class.java)
        startActivity(myIntent)
    }

    override fun onTokenReceived(accessToken: String) {
        Log.d(TAG, "Received access_token: $accessToken")
        val preferences = applicationContext.getSharedPreferences(HYPEMAP_SHARED_PREF, Context.MODE_PRIVATE)
        preferences.edit().putString(INSTAGRAM_ACCESS_TOKEN, accessToken).apply()
    }
}
