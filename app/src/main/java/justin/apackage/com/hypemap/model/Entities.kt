package justin.apackage.com.hypemap.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "posts")
data class PostLocation (@PrimaryKey var id: String,
                         @ColumnInfo(name = "userName") var userName: String,
                         @ColumnInfo(name  = "locationName") var locationName: String,
                         @ColumnInfo(name = "locationId") var locationId: String,
                         @ColumnInfo(name = "latitude") var latitude: Double,
                         @ColumnInfo(name = "longitude") var longitude: Double,
                         @ColumnInfo(name = "postUrl") var postUrl: String,
                         @ColumnInfo(name = "linkUrl") var linkUrl: String,
                         @ColumnInfo(name = "caption") var caption: String,
                         @ColumnInfo(name = "timestamp") var timestamp: Long) : Parcelable

@Entity(tableName = "users")
data class User (@PrimaryKey var userName: String,
    @ColumnInfo(name = "profilePicUrl") var profilePicUrl: String,
    @ColumnInfo(name ="colour") var colour: Float)

@Entity(tableName = "locations")
data class Location (@PrimaryKey var locationId: String,
    @ColumnInfo(name = "latitude") var latitude: Double,
    @ColumnInfo(name = "longitude") var longitude: Double)
