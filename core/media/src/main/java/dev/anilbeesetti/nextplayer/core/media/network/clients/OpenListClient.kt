package dev.anilbeesetti.nextplayer.core.media.network.clients

import dev.anilbeesetti.nextplayer.core.media.network.NetworkClient
import dev.anilbeesetti.nextplayer.core.model.NetworkConnection
import dev.anilbeesetti.nextplayer.core.model.NetworkFile
import java.io.InputStream
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl

/** OpenList/AList v3 API client with fresh raw-link resolution for every streamed request. */
class OpenListClient(private val connection: NetworkConnection) : NetworkClient {

    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private var token: String? = null

    override val rootPath: String = ""

    private val serverBase: HttpUrl by lazy {
        val host = connection.host.trim().trimEnd('/')
        val hasScheme = host.startsWith("http://") || host.startsWith("https://")
        val parsed = (if (hasScheme) host else "${if (connection.useHttps) "https" else "http"}://$host")
            .plus("/")
            .toHttpUrl()
        if (hasScheme || parsed.port == connection.effectivePort) {
            parsed
        } else {
            parsed.newBuilder().port(connection.effectivePort).build()
        }
    }

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            token = null
            if (!connection.isAnonymous) login()
            listFiles("").getOrThrow()
            Unit
        }
    }

    override suspend fun disconnect() {
        token = null
    }

    override fun isConnected(): Boolean = connection.isAnonymous || token != null

    override suspend fun listFiles(path: String): Result<List<NetworkFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val data = apiPost(
                endpoint = "fs/list",
                body = buildJsonObject {
                    put("path", absolutePath(path))
                    put("password", connection.directoryPassword)
                    put("page", 1)
                    put("per_page", 0)
                    put("refresh", false)
                },
            )
            data["content"]?.jsonArray.orEmpty().map { item ->
                val value = item.jsonObject
                val name = value.string("name")
                NetworkFile(
                    name = name,
                    path = if (path.isBlank()) name else "${path.trimEnd('/')}/$name",
                    isDirectory = value.boolean("is_dir"),
                    size = value.long("size"),
                    modified = value.stringOrNull("modified")?.let(::parseModifiedTime),
                )
            }
        }
    }

    override suspend fun fileSize(path: String): Long = withContext(Dispatchers.IO) {
        runCatching { getFile(path).long("size") }.getOrDefault(-1L)
    }

    override suspend fun openStream(path: String, offset: Long): InputStream = withContext(Dispatchers.IO) {
        val rawUrl = getFile(path).string("raw_url")
        val resolvedUrl = serverBase.resolve(rawUrl) ?: rawUrl.toHttpUrl()
        val request = Request.Builder().url(resolvedUrl).apply {
            if (offset > 0) header("Range", "bytes=$offset-")
            if (resolvedUrl.scheme == serverBase.scheme &&
                resolvedUrl.host == serverBase.host &&
                resolvedUrl.port == serverBase.port
            ) {
                token?.let { header("Authorization", it) }
            }
        }.build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            error("OpenList download failed: HTTP ${response.code}")
        }
        response.body?.byteStream() ?: run {
            response.close()
            error("OpenList returned an empty download response")
        }
    }

    private fun getFile(path: String): JsonObject = apiPost(
        endpoint = "fs/get",
        body = buildJsonObject {
            put("path", absolutePath(path))
            put("password", connection.directoryPassword)
        },
    )

    private fun login() {
        val data = apiPost(
            endpoint = "auth/login",
            body = buildJsonObject {
                put("username", connection.username)
                put("password", connection.password)
            },
            allowRelogin = false,
        )
        token = data.string("token")
    }

    private fun apiPost(endpoint: String, body: JsonObject, allowRelogin: Boolean = true): JsonObject {
        val response = executeApiPost(endpoint, body)
        if (response.code == 401 && allowRelogin && !connection.isAnonymous) {
            response.close()
            login()
            return parseApiResponse(executeApiPost(endpoint, body))
        }
        return parseApiResponse(response)
    }

    private fun executeApiPost(endpoint: String, body: JsonObject) = httpClient.newCall(
        Request.Builder()
            .url(serverBase.newBuilder().addPathSegments("api/$endpoint").build())
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .apply { token?.let { header("Authorization", it) } }
            .build(),
    ).execute()

    private fun parseApiResponse(response: okhttp3.Response): JsonObject = response.use {
        val text = it.body?.string().orEmpty()
        val envelope = runCatching { json.parseToJsonElement(text).jsonObject }
            .getOrElse { error("OpenList returned an invalid response (HTTP ${response.code})") }
        val code = envelope["code"]?.jsonPrimitive?.content?.toIntOrNull() ?: response.code
        if (!response.isSuccessful || code != 200) {
            val message = envelope["message"]?.jsonPrimitive?.contentOrNull
            error(message ?: "OpenList request failed: HTTP ${response.code}, code $code")
        }
        envelope["data"]?.jsonObject ?: error("OpenList response has no data")
    }

    private fun absolutePath(path: String): String {
        val root = connection.path.trim('/').takeIf { it.isNotEmpty() }
        val child = path.trim('/').takeIf { it.isNotEmpty() }
        return listOfNotNull(root, child).joinToString("/", prefix = "/")
    }

    private fun parseModifiedTime(value: String): Long? = runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()

    private fun JsonObject.string(name: String): String =
        get(name)?.jsonPrimitive?.contentOrNull ?: error("OpenList response is missing $name")

    private fun JsonObject.stringOrNull(name: String): String? = get(name)?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(name: String): Long = get(name)?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

    private fun JsonObject.boolean(name: String): Boolean = get(name)?.jsonPrimitive?.content?.toBoolean() ?: false

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
