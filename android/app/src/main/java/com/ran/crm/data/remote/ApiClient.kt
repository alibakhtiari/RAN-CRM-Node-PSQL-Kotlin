package com.ran.crm.data.remote

import com.google.gson.GsonBuilder
import com.ran.crm.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://nc.ramzarznegaran.com/"

    private var authToken: String? = null

    @Suppress("DEPRECATION") private val gson = GsonBuilder().setLenient().create()

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
                .cookieJar(MemoryCookieJar())
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()

                    authToken?.let { requestBuilder.header("Authorization", "Bearer $it") }

                    val response = chain.proceed(requestBuilder.build())

                    // Check for 401/403 (Unauthorized/Forbidden)
                    if (response.code == 401 || response.code == 403) {
                        android.util.Log.e(
                                "ApiClient",
                                "Auth error detected: ${response.code}. Triggering logout."
                        )
                        try {
                            val context = com.ran.crm.CrmApplication.instance
                            val intent = android.content.Intent("com.ran.crm.ACTION_LOGOUT")
                            intent.setPackage(context.packageName)
                            context.sendBroadcast(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("ApiClient", "Failed to broadcast logout", e)
                        }
                    }

                    response
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)

        // Only log HTTP bodies in debug builds
        if (BuildConfig.DEBUG) {
            val loggingInterceptor =
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
    }

    val apiService: CrmApiService by lazy { retrofit.create(CrmApiService::class.java) }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getAuthToken(): String? = authToken

    private class MemoryCookieJar : CookieJar {
        private val cookieStore = HashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: ArrayList()
        }
    }
}
