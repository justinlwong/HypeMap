package justin.apackage.com.hypemap.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MarkerTag(val id: String,
                     val userName: String,
                     val locationId: String,
                     val locationName: String,
                     val postUrl: String,
                     val linkUrl: String,
                     val caption: String,
                     val timestamp: Long) : Parcelable