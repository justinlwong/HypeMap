package justin.apackage.com.hypemap.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import justin.apackage.com.hypemap.R
import kotlinx.android.synthetic.main.auth_dialog.*

/**
 * A dialog to prompt authentication
 *
 * @author Justin Wong
 */
class AuthenticationDialog(val listener: AuthenticationListener): DialogFragment() {

    private lateinit var redirectUrl: String
    private lateinit var loginUrl: String

    companion object {
        const val TAG = "AuthenticationDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.BasicDialog)
        val res = resources
        redirectUrl = res.getString(R.string.redirect_url)
        loginUrl = "${redirectUrl}accounts/login"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.auth_dialog, container)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onResume() {
        super.onResume()

        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        Log.d(TAG, "Requesting url: $loginUrl")
        webView.webViewClient = object: WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                swipeRefreshLayout?.isRefreshing = true
            }

            override fun onPageFinished(view: WebView,
                                        url: String) {
                super.onPageFinished(view, url)
                swipeRefreshLayout?.isRefreshing = false
                if (url == redirectUrl) {
                    val cookie = CookieManager.getInstance().getCookie(url)
                    listener.onCookieReceived(cookie ?: "")
                    dismiss()
                }
            }
        }
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }
        webView.loadUrl(loginUrl)
    }
}