package com.garbage.management.domain.model

/**
 * Domain representation of an uploaded media asset on Cloudinary.
 */
data class UploadedMedia(
    val url: String,
    val publicId: String? = null,
    val resourceType: String? = null
)
