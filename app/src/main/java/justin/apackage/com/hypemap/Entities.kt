package justin.apackage.com.hypemap

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "posts")
data class Post (@PrimaryKey var id: String,
    @ColumnInfo(name = "userName") var userName: String,
    @ColumnInfo(name  = "locationName") var locationName: String,
    @ColumnInfo(name = "locationId") var locationId: String,
    @ColumnInfo(name = "latitude") var latitude: Double,
    @ColumnInfo(name = "longitude") var longitude: Double,
    @ColumnInfo(name = "postUrl") var postUrl: String,
    @ColumnInfo(name = "linkUrl") var linkUrl: String,
    @ColumnInfo(name = "caption") var caption: String,
    @ColumnInfo(name = "timestamp") var timestamp: Long,
    @ColumnInfo(name = "visible") var visible: Boolean)

@Entity(tableName = "users")
data class User (@PrimaryKey var userName: String,
    @ColumnInfo(name = "profilePicUrl") var profilePicUrl: String,
    @ColumnInfo(name = "visible") var visible: Boolean)
