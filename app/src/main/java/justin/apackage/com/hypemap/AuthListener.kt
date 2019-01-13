package justin.apackage.com.hypemap

interface AuthListener {
    fun onCodeReceived(authToken : String?)
}