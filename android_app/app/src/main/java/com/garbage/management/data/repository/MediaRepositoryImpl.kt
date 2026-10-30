package com.garbage.management.data.repository

import android.content.Context
import android.net.Uri
import com.garbage.management.data.remote.RemoteMediaDataSource
import com.garbage.management.domain.model.UploadedMedia
import com.garbage.management.domain.repository.MediaRepository
import com.garbage.management.utils.Resource

/**
 * Concrete implementation of MediaRepository using RemoteMediaDataSource.
 */
class MediaRepositoryImpl(
    private val remoteMediaDataSource: RemoteMediaDataSource
) : MediaRepository {

    override suspend fun uploadMedia(
        context: Context,
        uri: Uri,
        folder: String,
        complaintId: String?
    ): Resource<UploadedMedia> {
        return when (val result = remoteMediaDataSource.uploadMedia(context, uri, folder, complaintId)) {
            is Resource.Success -> {
                val data = result.data!!
                val stored = UploadedMedia(url = data.url, publicId = data.publicId, resourceType = data.resourceType)
                if (!com.garbage.management.utils.StoredMediaReference.isStoredImage(stored) && folder != "videos") {
                    Resource.Error("Upload returned no valid stored image reference. Photo preview does not prove upload success.")
                } else Resource.Success(stored)
            }
            is Resource.Error -> Resource.Error(result.message ?: "Failed to upload media.")
            is Resource.Loading -> Resource.Loading()
        }
    }
}
