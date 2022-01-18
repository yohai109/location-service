package idf.lotem.locationapplication

import android.content.Context
import com.google.gson.Gson
import okhttp3.*
import timber.log.Timber
import java.io.IOException
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
        Timber.d("request Body: $payloadString")

        val request = Request.Builder().run {
            url(url) // TODO get the url from config file like in rocket
            post(body)
            build()
        }

        try {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.e(e, "sending location failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    val statusCode = response.code()
                    response.close()
                    when (statusCode) {
                        200 -> Timber.d("everything is fine")
                        else -> Timber.e("sending location failed with code: $statusCode")
                    }
                }
            })
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun getConfiguration(callback: (ServiceConfig) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e, "sending location failed")
            }

            override fun onResponse(call: Call, response: Response) {
                Timber.d("getConfiguration onResponse")
                when (val statusCode = response.code()) {
                    200 -> {
                        Timber.d("everything is fine")
                        val responseBody = response.body()?.string()
                        response.close()

                        callback(gson.fromJson(responseBody ?: "", ServiceConfig::class.java))
                    }
                    else -> Timber.e("sending location failed with code: $statusCode")
                }
            }
        })
    }
}