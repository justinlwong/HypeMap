package justin.apackage.com.hypemap

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class AuthDialog (var listener: AuthListener, context: Context): Dialog(context) {
    private lateinit var  webView : WebView
    private val requestUrl = HypeMapConstants.IG_BASE_URL +
            "oauth/authorize/?client_id=" +
            HypeMapConstants.IG_CLIENT_ID +
            "&redirect_uri=" +
            HypeMapConstants.IG_REDIRECT_URL +
            "&response_type=token" +
            "&display=touch&scope=public_content"

    companion object {
        private const val TAG = "AuthDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.auth_dialog)
        initializeWebView()
    }

    private fun initializeWebView() {
        webView = WebView(context) // findViewById<View>(R.id.web_view) as WebView
        webView.webViewClient = object : WebViewClient() {

            var authComplete = false
            lateinit var access_token: String

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (url.contains("#access_token=") && !authComplete) {
                    val uri = Uri.parse(url)
                    access_token = uri.encodedFragment ?: ""
                    // get the whole token after the '=' sign
                    access_token = access_token.substring(access_token.lastIndexOf("=") + 1)
                    Log.d(TAG, "CODE : $access_token")
                    authComplete = true
                    listener.onCodeReceived(access_token)
                    dismiss()

                } else if (url.contains("?error")) {
                    Toast.makeText(context, "Error Occurred", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
        setContentView(webView)
        webView.getSettings().setJavaScriptEnabled(true)
        Log.d(TAG, requestUrl)
        webView.loadUrl(requestUrl)
    }
}