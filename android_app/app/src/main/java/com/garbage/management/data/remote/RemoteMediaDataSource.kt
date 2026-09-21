package com.garbage.management.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.garbage.management.data.model.MediaUploadResponseDto
import com.garbage.management.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Remote data source for uploading images and videos to the backend Cloudinary endpoint.
 */
class RemoteMediaDataSource(private val apiService: ApiService) {

    suspend fun uploadMedia(
        context: Context,
        uri: Uri,
        folder: String = "complaints",
        complaintId: String? = null
    ): Resource<MediaUploadResponseDto> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // Determine MIME type
            val mimeType = contentResolver.getType(uri) ?: when {
                uri.toString().endsWith(".jpg", true) || uri.toString().endsWith(".jpeg", true) -> "image/jpeg"
                uri.toString().endsWith(".png", true) -> "image/png"
                uri.toString().endsWith(".webp", true) -> "image/webp"
                uri.toString().endsWith(".mp4", true) -> "video/mp4"
                uri.toString().endsWith(".mov", true) -> "video/quicktime"
                uri.toString().endsWith(".webm", true) -> "video/webm"
                else -> "application/octet-stream"
            }

            // Determine file name
            var fileName = "media_upload_${System.currentTimeMillis()}"
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            fileName = name
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to timestamp filename
            }

            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Resource.Error("Unable to read media file content.")

            if (bytes.isEmpty()) {
                return@withContext Resource.Error("Selected media file is empty.")
            }

            val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
            val folderBody = folder.toRequestBody("text/plain".toMediaTypeOrNull())
            val complaintIdBody = complaintId?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.uploadMedia(filePart, folderBody, complaintIdBody)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to upload media.")
        }
    }

    private fun parseErrorMessage(errorBody: String?, fallbackMessage: String): String {
        if (errorBody.isNullOrBlank()) return fallbackMessage.ifBlank { "An unexpected error occurred." }
        return try {
            val json = JSONObject(errorBody)
            json.optString("message", fallbackMessage)
        } catch (_: Exception) {
            fallbackMessage.ifBlank { "An unexpected error occurred." }
        }
    }
}
