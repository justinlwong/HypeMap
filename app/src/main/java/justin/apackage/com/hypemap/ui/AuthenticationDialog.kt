package justin.apackage.com.hypemap.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
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
    private lateinit var requestUrl: String

    companion object {
        const val TAG = "AuthenticationDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.BasicDialog)
        val res = resources
        val baseUrl = res.getString(R.string.base_url)
        val clientId = res.getString(R.string.client_id)
        redirectUrl = res.getString(R.string.redirect_url)
        requestUrl = "${baseUrl}oauth/authorize/?client_id=$clientId&redirect_uri=$redirectUrl" +
                "&response_type=token&display=touch&scope=basic"
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
        Log.d(TAG, "Requesting url: $requestUrl")
        webView.webViewClient = object: WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView,
                                        url: String) {
                super.onPageFinished(view, url)
                progressBar?.visibility = View.GONE
                if (url.contains("access_token=")) {
                    val uri = Uri.parse(url)
                    val accessToken = uri.encodedFragment
                    accessToken?.let {
                        listener.onTokenReceived(it.substring(it.lastIndexOf("=") + 1))
                    }
                } else if (url == redirectUrl) {
                    val cookie = CookieManager.getInstance().getCookie(url)
                    listener.onCookieReceived(cookie ?: "")
                    dismiss()
                }
            }
        }
        webView.loadUrl(requestUrl)
    }
}