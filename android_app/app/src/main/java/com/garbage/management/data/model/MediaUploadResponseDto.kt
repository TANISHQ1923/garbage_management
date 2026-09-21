package com.garbage.management.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data transfer object returned by POST /api/media/upload
 */
data class MediaUploadResponseDto(
    @SerializedName("url")
    val url: String,
    @SerializedName("publicId")
    val publicId: String? = null,
    @SerializedName("resourceType")
    val resourceType: String? = null
)
