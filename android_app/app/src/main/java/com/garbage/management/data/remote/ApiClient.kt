package com.garbage.management.data.remote

import com.garbage.management.data.local.SessionManager
import com.garbage.management.utils.Constants
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory providing the Retrofit instance configured with logging, timeouts,
 * and automatic Bearer JWT token injection.
 */
object ApiClient {

    fun create(
        baseUrl: String = Constants.BASE_URL,
        sessionManager: SessionManager? = null
    ): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Login requests/responses contain passwords and tokens; never log their bodies.
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = sessionManager?.let {
                try {
                    runBlocking {
                        val saved = it.getToken()
                        if (!saved.isNullOrBlank()) {
                            saved
                        } else {
                            val user = it.getSavedSession()
                            if (user != null) "mock-token-${user.id.ifBlank { "mock-citizen-001" }}" else null
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val requestBuilder = original.newBuilder()
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10L, TimeUnit.SECONDS)
            .readTimeout(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

