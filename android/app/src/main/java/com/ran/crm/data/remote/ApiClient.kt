package com.ran.crm.data.remote

import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://nc.ramzarznegaran.com/" // Android emulator localhost

    private var authToken: String? = null

    @Suppress("DEPRECATION") private val gson = GsonBuilder().setLenient().create()

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts =
                    arrayOf<javax.net.ssl.TrustManager>(
                            object : javax.net.ssl.X509TrustManager {
                                override fun checkClientTrusted(
                                        chain: Array<java.security.cert.X509Certificate>,
                                        authType: String
                                ) {}
                                override fun checkServerTrusted(
                                        chain: Array<java.security.cert.X509Certificate>,
                                        authType: String
                                ) {}
                                override fun getAcceptedIssuers():
                                        Array<java.security.cert.X509Certificate> = arrayOf()
                            }
                    )

            // Install the all-trusting trust manager
            val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                    .sslSocketFactory(
                            sslSocketFactory,
                            trustAllCerts[0] as javax.net.ssl.X509TrustManager
                    )
                    .hostnameVerifier { _, _ -> true }
                    .connectionSpecs(
                            listOf(
                                    okhttp3.ConnectionSpec.COMPATIBLE_TLS,
                                    okhttp3.ConnectionSpec.CLEARTEXT
                            )
                    )
                    .cookieJar(MemoryCookieJar())
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val requestBuilder =
                                original.newBuilder()
                                        .header(
                                                "User-Agent",
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                                        )

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
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
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
