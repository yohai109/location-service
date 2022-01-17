package com.example.locationapplication

import android.content.Context
import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import timber.log.Timber
import java.security.KeyStore
import java.util.*
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class HttpHandler(context: Context, config: Config) {
    private var client: OkHttpClient
    private val gson = Gson()
    private val url = config.url

    init {
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        ).apply {
            init(getKeyStore(context))
        }

        val trustManagers = trustManagerFactory.trustManagers
        if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) {
            throw IllegalStateException(
                "Unexpected default trust managers:" + Arrays.toString(
                    trustManagers
                )
            )
        }
        val trustManager = trustManagers[0] as X509TrustManager

        client = OkHttpClient.Builder().run {
            sslSocketFactory(TLSSocketFactory(trustManagers), trustManager)
            build()
        }
    }

    private fun getKeyStore(context: Context): KeyStore {
        val rawStore = context.resources.openRawResource(R.raw.store)

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            .apply {
                load(rawStore, "rocket".toCharArray())
            }

        rawStore.close()

        return keyStore
    }

    fun sendLocation(requestPayload: RequestPayload) {
        val mediaType = MediaType.get("application/json; charset=utf-8")
        val payloadString = gson.toJson(requestPayload)
        val body = RequestBody.create(mediaType, payloadString)

        val request = Request.Builder().run {
            url(url) // TODO get the url from config file like in rocket
            post(body)
            build()
        }

        try {
            val res = client.newCall(request).execute()
            val statusCode = res.code()
            res.close()
            when (statusCode) {
                200 -> Timber.d("everything is fine")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}