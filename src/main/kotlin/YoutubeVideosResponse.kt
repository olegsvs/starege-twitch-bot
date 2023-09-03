import kotlinx.serialization.Serializable

@Serializable
data class YoutubeVideosResponse(
    val items: List<VideoItem>
) {}

@Serializable
data class VideoItem(
    val snippet: VideoSnippet
) {}

@Serializable
data class VideoSnippet(
    val title: String,
    val description: String,
    val resourceId: ResourceId

) {}

@Serializable
data class ResourceId(
    val videoId: String
) {}