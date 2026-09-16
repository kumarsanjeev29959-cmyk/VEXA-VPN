package com.vexa.vpn

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class HttpVpnProvisioningRepository(context: Context, private val baseUrl: String) : VpnProvisioningRepository {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        require(baseUrl.startsWith("https://", ignoreCase = true)) { "VEXA API base URL must use HTTPS." }
    }

    override suspend fun registerDevice(identity: DeviceIdentity): Result<DeviceProvisioningResponse> = runCatching {
        val cached = loadCachedToken()
        if (cached != null) return@runCatching cached
        val body = JSONObject()
            .put("deviceId", identity.deviceId)
            .put("publicKey", identity.publicKey)
            .put("platform", "android")
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("deviceName", Build.MODEL)
        val json = request("POST", "/v1/devices", null, body)
        val response = DeviceProvisioningResponse(
            json.getString("deviceToken"),
            json.getString("deviceId"),
            json.getString("expiresAt")
        )
        cacheToken(response)
        response
    }

    override suspend fun listServers(deviceToken: String): Result<List<VpnServer>> = runCatching {
        val array = requestWithTokenRetry(deviceToken) { request("GET", "/v1/servers", it, null) }
            .optJSONArray("servers") ?: JSONArray()
        buildList(array.length()) {
            for (i in 0 until array.length()) add(server(array.getJSONObject(i)))
        }
    }

    override suspend fun provisionConfig(deviceToken: String, request: ProvisioningRequest): Result<VpnConfigResponse> = runCatching {
        val body = JSONObject()
            .put("deviceId", request.deviceId)
            .put("publicKey", request.publicKey)
            .put("fastest", request.fastest)
        request.serverId?.let { body.put("serverId", it) }
        val json = requestWithTokenRetry(deviceToken) { request("POST", "/v1/vpn/config", it, body) }
        val p = json.getJSONObject("peer")
        VpnConfigResponse(
            server = json.getJSONObject("server").let(::server),
            peer = VpnPeerConfig(
                p.getString("serverPublicKey"),
                p.getString("address"),
                p.getString("dns"),
                p.getString("allowedIPs"),
                p.optInt("persistentKeepalive", 25)
            ),
            expiresAt = json.getString("expiresAt")
        )
    }

    private suspend fun requestWithTokenRetry(token: String, call: (String) -> JSONObject): JSONObject {
        return try {
            call(token)
        } catch (error: ApiException) {
            if (error.statusCode != 401) throw error
            clearToken()
            val fresh = registerDevice(DeviceIdentity(appContext)).getOrThrow().deviceToken
            call(fresh)
        }
    }

    private fun loadCachedToken(): DeviceProvisioningResponse? {
        val encrypted = prefs.getString(KEY_DEVICE_TOKEN, null)
        val expiry = prefs.getString(KEY_EXPIRES_AT, null)
        if (encrypted.isNullOrBlank() || expiry.isNullOrBlank()) return null
        val valid = runCatching { java.time.Instant.parse(expiry).isAfter(java.time.Instant.now()) }.getOrDefault(false)
        if (!valid) {
            clearToken()
            return null
        }
        val token = runCatching { decrypt(encrypted) }.getOrElse {
            // Older builds stored the token as plaintext. Never reuse that value; re-provision securely.
            clearToken()
            return null
        }
        return DeviceProvisioningResponse(token, DeviceIdentity(appContext).deviceId, expiry)
    }

    private fun cacheToken(response: DeviceProvisioningResponse) {
        prefs.edit()
            .putString(KEY_DEVICE_TOKEN, encrypt(response.deviceToken))
            .putString(KEY_EXPIRES_AT, response.expiresAt)
            .apply()
    }

    private fun clearToken() {
        prefs.edit().remove(KEY_DEVICE_TOKEN).remove(KEY_EXPIRES_AT).apply()
    }

    private fun server(x: JSONObject) = VpnServer(
        x.getString("id"), x.getString("name"), x.getString("countryCode"), x.getString("city"),
        x.getString("hostname"), x.getInt("port"), x.getString("protocol"),
        x.optBoolean("premium", false), x.optBoolean("healthy", true),
        x.optInt("loadPercent", 0), if (x.isNull("latencyMs")) null else x.optInt("latencyMs")
    )

    private fun request(method: String, path: String, deviceToken: String?, body: JSONObject?): JSONObject {
        val c = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
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
            if (body != null) c.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull().orEmpty()
                throw ApiException(status, if (message.isNotBlank()) message else "VEXA service returned HTTP $status.")
            }
            JSONObject(text)
        } finally {
            c.disconnect()
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > GCM_IV_LENGTH) { "Stored device token is invalid" }
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEYSTORE_ALIAS, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private class ApiException(val statusCode: Int, message: String) : IllegalStateException(message)

    companion object {
        private const val PREFS = "vexa_provisioning"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_EXPIRES_AT = "device_token_expires_at"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "vexa_provisioning_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val TIMEOUT_MS = 15_000
    }
}
