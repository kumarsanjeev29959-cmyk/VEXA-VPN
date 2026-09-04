package com.vexa.vpn

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Device-only HTTPS client for the VEXA control-plane API. No account or password is used. */
class HttpVpnProvisioningRepository(
    context: Context,
    private val baseUrl: String
) : VpnProvisioningRepository {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override suspend fun registerDevice(identity: DeviceIdentity): Result<DeviceProvisioningResponse> = runCatching {
        val cachedToken = prefs.getString(KEY_DEVICE_TOKEN, null)
        val cachedExpiry = prefs.getString(KEY_EXPIRES_AT, null)
        if (!cachedToken.isNullOrBlank() && !cachedExpiry.isNullOrBlank()) {
            return@runCatching DeviceProvisioningResponse(cachedToken, identity.deviceId, cachedExpiry)
        }

        val body = JSONObject()
            .put("deviceId", identity.deviceId)
            .put("publicKey", identity.publicKey)
            .put("platform", "android")
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("deviceName", Build.MODEL)
        val json = request("POST", "/v1/devices", null, body)
        val response = DeviceProvisioningResponse(
            deviceToken = json.getString("deviceToken"),
            deviceId = json.getString("deviceId"),
            expiresAt = json.getString("expiresAt")
        )
        prefs.edit()
            .putString(KEY_DEVICE_TOKEN, response.deviceToken)
            .putString(KEY_EXPIRES_AT, response.expiresAt)
            .apply()
        response
    }

    override suspend fun listServers(deviceToken: String): Result<List<VpnServer>> = runCatching {
        val json = request("GET", "/v1/servers", deviceToken, null)
        val array = json.optJSONArray("servers") ?: JSONArray()
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    VpnServer(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        countryCode = item.getString("countryCode"),
                        city = item.getString("city"),
                        hostname = item.getString("hostname"),
                        port = item.getInt("port"),
                        protocol = item.getString("protocol"),
                        premium = item.optBoolean("premium", false),
                        healthy = item.optBoolean("healthy", false),
                        loadPercent = item.optInt("loadPercent", 100),
                        latencyMs = if (item.isNull("latencyMs")) null else item.optInt("latencyMs")
                    )
                )
            }
        }
    }

    override suspend fun provisionConfig(deviceToken: String, request: ProvisioningRequest): Result<VpnConfigResponse> = runCatching {
        val body = JSONObject()
            .put("deviceId", request.deviceId)
            .put("publicKey", request.publicKey)
            .put("fastest", request.fastest)
        request.serverId?.let { body.put("serverId", it) }
        val json = request("POST", "/v1/vpn/config", deviceToken, body)
        val serverJson = json.getJSONObject("server")
        VpnConfigResponse(
            server = VpnServer(
                id = serverJson.getString("id"),
                name = serverJson.getString("name"),
                countryCode = serverJson.getString("countryCode"),
                city = serverJson.getString("city"),
                hostname = serverJson.getString("hostname"),
                port = serverJson.getInt("port"),
                protocol = serverJson.getString("protocol"),
                premium = serverJson.optBoolean("premium", false),
                healthy = serverJson.optBoolean("healthy", true),
                loadPercent = serverJson.optInt("loadPercent", 0),
                latencyMs = if (serverJson.isNull("latencyMs")) null else serverJson.optInt("latencyMs")
            ),
            config = json.getString("config"),
            expiresAt = json.getString("expiresAt")
        )
    }

    private fun request(method: String, path: String, deviceToken: String?, body: JSONObject?): JSONObject {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VEXA-VPN/${BuildConfig.VERSION_NAME}")
            deviceToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull().orEmpty()
                throw IllegalStateException(if (message.isNotBlank()) message else "VEXA service returned HTTP $status.")
            }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val PREFS = "vexa_provisioning"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_EXPIRES_AT = "device_token_expires_at"
        private const val TIMEOUT_MS = 15_000
    }
}
