package com.garbage.management

import com.garbage.management.domain.model.UploadedMedia
import com.garbage.management.domain.usecase.UploadCollectionPhotos
import com.garbage.management.utils.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CollectionPhotoUploadTest {
    @Test fun beforeFailureStopsAfterUploadAndCompletion() = runBlocking {
        val calls = mutableListOf<String>()
        val result = UploadCollectionPhotos.upload("content://before", "content://after", { folder, _ ->
            calls.add(folder); Resource.Error("Photo storage is not configured")
        })
        assertTrue(result is Resource.Error)
        assertTrue(result.message!!.contains("Before-photo"))
        assertEquals(listOf("before-cleaning"), calls)
    }
    @Test fun localPreviewIsNeverAcceptedAsUploadSuccess() = runBlocking {
        val result = UploadCollectionPhotos.upload(null, "content://after", { _, _ ->
            Resource.Success(UploadedMedia("content://after", null, "image"))
        })
        assertTrue(result is Resource.Error)
    }
    @Test fun validUploadsKeepBothStoredReferences() = runBlocking {
        val stored = mutableListOf<String>()
        val result = UploadCollectionPhotos.upload("content://before", "content://after", { folder, _ ->
            Resource.Success(UploadedMedia("https://example.invalid/$folder.jpg", "test/$folder", "image"))
        }, { folder, _ -> stored.add(folder) })
        assertTrue(result is Resource.Success)
        assertEquals(2, stored.size)
        assertTrue(result.data!!.after.startsWith("https://"))
    }
    @Test fun incompleteUploadCannotAdvanceLifecycle() = runBlocking {
        val result = UploadCollectionPhotos.upload(null, "content://after", { _, _ -> Resource.Loading() })
        assertTrue(result is Resource.Error)
    }
}
