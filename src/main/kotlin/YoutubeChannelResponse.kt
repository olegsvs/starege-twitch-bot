import kotlinx.serialization.Serializable

@Serializable
data class YoutubeChannelResponse(
    val items: List<Playlist>
) {}

@Serializable
data class Playlist(
    val contentDetails: ContentDetails
) {}

@Serializable
data class ContentDetails(
    val relatedPlaylists: RelatedPlaylists
) {}

@Serializable
data class RelatedPlaylists(
    val uploads: String
) {}