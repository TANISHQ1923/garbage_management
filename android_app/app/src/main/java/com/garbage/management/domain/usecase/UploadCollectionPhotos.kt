package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.UploadedMedia
import com.garbage.management.utils.Resource
import com.garbage.management.utils.StoredMediaReference

data class CollectionPhotoUrls(val before: String?, val after: String)

object UploadCollectionPhotos {
    suspend fun upload(
        before: String?, after: String,
        uploader: suspend (String, String) -> Resource<UploadedMedia>,
        onStored: (String, String) -> Unit = { _, _ -> }
    ): Resource<CollectionPhotoUrls> {
        suspend fun one(uri: String, folder: String): Resource<String> {
            // Server validates cached URLs against its task/driver-bound upload ledger.
            if (uri.startsWith("https://")) return Resource.Success(uri)
            val label = if (folder == "before-cleaning") "Before-photo" else "After-photo"
            return when (val result = uploader(folder, uri)) {
                is Resource.Success -> if (StoredMediaReference.isStoredImage(result.data)) {
                    val url = result.data!!.url
                    onStored(folder, url)
                    Resource.Success(url)
                } else Resource.Error("$label upload returned no valid stored-media reference. Retry upload.")
                is Resource.Error -> Resource.Error("$label upload failed: ${result.message}")
                is Resource.Loading -> Resource.Error("$label upload did not finish. Retry upload before completing pickup.")
            }
        }
        if (after.isBlank()) return Resource.Error("After-cleaning photo evidence is required.")
        val beforeResult = if (before.isNullOrBlank()) null else one(before, "before-cleaning")
        if (beforeResult is Resource.Error) return Resource.Error(beforeResult.message ?: "Before-photo upload failed.")
        val afterResult = one(after, "after-cleaning")
        if (afterResult is Resource.Error) return Resource.Error(afterResult.message ?: "After-photo upload failed.")
        return Resource.Success(CollectionPhotoUrls(beforeResult?.data, afterResult.data!!))
    }
}
