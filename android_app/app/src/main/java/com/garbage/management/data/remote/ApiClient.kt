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

        val hostFallbackInterceptor = Interceptor { chain ->
            val request = chain.request()
            try {
                chain.proceed(request)
            } catch (e: java.io.IOException) {
                val originalHost = request.url.host
                val candidateHosts = if (Constants.isEmulator()) {
                    listOf("10.0.2.2", "127.0.0.1", "10.138.253.78", "10.82.191.78", "192.168.1.5")
                } else {
                    listOf("127.0.0.1", "10.138.253.78", "10.82.191.78", "192.168.1.5", Constants.DEV_MACHINE_IP, "10.0.2.2")
                }.distinct().filter { it != originalHost }

                var lastException: java.io.IOException = e
                for (altHost in candidateHosts) {
                    val fallbackUrl = request.url.newBuilder().host(altHost).build()
                    val fallbackRequest = request.newBuilder().url(fallbackUrl).build()
                    try {
                        return@Interceptor chain.proceed(fallbackRequest)
                    } catch (altEx: java.io.IOException) {
                        lastException = altEx
                    }
                }
                throw lastException
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(hostFallbackInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(5L, TimeUnit.SECONDS)
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

