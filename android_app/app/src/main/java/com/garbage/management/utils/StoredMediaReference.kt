package com.garbage.management.utils

import com.garbage.management.domain.model.UploadedMedia
import java.net.URI

object StoredMediaReference {
    fun isStoredImage(media: UploadedMedia?): Boolean = media != null &&
        !media.publicId.isNullOrBlank() && media.resourceType == "image" &&
        runCatching { val uri = URI(media.url); uri.scheme == "https" && !uri.host.isNullOrBlank() }.getOrDefault(false)
}
