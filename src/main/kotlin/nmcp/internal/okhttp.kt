package nmcp.internal

import java.time.Duration
import okhttp3.OkHttpClient

internal val client = OkHttpClient.Builder()
    .connectTimeout(Duration.ofSeconds(30))
    .writeTimeout(Duration.ofSeconds(30))
    .readTimeout(Duration.ofSeconds(60))
    .build()
