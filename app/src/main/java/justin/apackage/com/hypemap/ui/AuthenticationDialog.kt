package justin.apackage.com.hypemap.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import justin.apackage.com.hypemap.R
import kotlinx.android.synthetic.main.auth_dialog.*

/**
 * A dialog to prompt authentication
 *
 * @author Justin Wong
 */
class AuthenticationDialog(context: Context,
                           val listener: AuthenticationListener): Dialog(context) {

    val redirectUrl: String
    val requestUrl: String

    companion object {
        const val TAG = "AuthenticationDialog"
    }

    init {
        val res = context.resources
        val baseUrl = res.getString(R.string.base_url)
        val clientId = res.getString(R.string.client_id)
        redirectUrl = context.resources.getString(R.string.redirect_url)
        requestUrl = "${baseUrl}oauth/authorize/?client_id=$clientId&redirect_uri=$redirectUrl" +
                "&response_type=token&display=touch&scope=basic"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_dialog)

        webView.settings.javaScriptEnabled = true
        Log.d(TAG, "Requesting url: $requestUrl")
        webView.loadUrl(requestUrl)
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView,
                                        url: String) {
                super.onPageFinished(view, url)
                val cookie = CookieManager.getInstance().getCookie(url)
                if (url == redirectUrl) {
                    listener.onCookieReceived(cookie ?: "")
                    dismiss()
                }
            }
        }
    }
}