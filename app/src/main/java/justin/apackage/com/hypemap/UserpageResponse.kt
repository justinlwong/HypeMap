package justin.apackage.com.hypemap

import com.google.gson.annotations.SerializedName

data class UserpageResponse (@SerializedName("entry_data") val entry_data: EntryData) {
    data class EntryData (@SerializedName("ProfilePage") val ProfilePage: ArrayList<Profile>) {
        data class Profile(@SerializedName("graphql") val graphql: Wrapper?) {
            data class Wrapper(@SerializedName("user") val user: User) {
                data class User(@SerializedName("edge_owner_timeline_media") val edge_owner_timeline_media: Timeline) {
                    data class Timeline(@SerializedName("edges") val edges: ArrayList<PostWrapper>) {
                        data class PostWrapper(@SerializedName("node") val node: Post) {
                            data class Post(@SerializedName("location") val location: Location) {
                                data class Location(
                                    @SerializedName("id") val id: Int = 0,
                                    @SerializedName("has_public_page") val has_public_page: Boolean = false,
                                    @SerializedName("name") val name: String? = null,
                                    @SerializedName("slug") val slug: String? = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}