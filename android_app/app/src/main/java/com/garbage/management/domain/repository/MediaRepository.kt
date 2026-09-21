package com.garbage.management.domain.repository

import android.content.Context
import android.net.Uri
import com.garbage.management.domain.model.UploadedMedia
import com.garbage.management.utils.Resource

/**
 * Repository interface for uploading media to cloud storage.
 */
interface MediaRepository {
    suspend fun uploadMedia(
        context: Context,
        uri: Uri,
        folder: String = "complaints",
        complaintId: String? = null
    ): Resource<UploadedMedia>
}
