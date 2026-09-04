package com.vexa.vpn

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HttpVpnProvisioningRepository(context:Context,private val baseUrl:String):VpnProvisioningRepository{
 private val prefs=context.applicationContext.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
 override suspend fun registerDevice(identity:DeviceIdentity):Result<DeviceProvisioningResponse>=runCatching{
  val cached=prefs.getString(KEY_DEVICE_TOKEN,null); val expiry=prefs.getString(KEY_EXPIRES_AT,null)
  if(!cached.isNullOrBlank()&&!expiry.isNullOrBlank()&&java.time.Instant.parse(expiry).isAfter(java.time.Instant.now())) return@runCatching DeviceProvisioningResponse(cached,identity.deviceId,expiry)
  val body=JSONObject().put("deviceId",identity.deviceId).put("publicKey",identity.publicKey).put("platform","android").put("appVersion",BuildConfig.VERSION_NAME).put("deviceName",Build.MODEL)
  val json=request("POST","/v1/devices",null,body); val response=DeviceProvisioningResponse(json.getString("deviceToken"),json.getString("deviceId"),json.getString("expiresAt"))
  prefs.edit().putString(KEY_DEVICE_TOKEN,response.deviceToken).putString(KEY_EXPIRES_AT,response.expiresAt).apply(); response
 }
 override suspend fun listServers(deviceToken:String):Result<List<VpnServer>>=runCatching{
  val array=request("GET","/v1/servers",deviceToken,null).optJSONArray("servers")?:JSONArray()
  buildList(array.length()){for(i in 0 until array.length()){val x=array.getJSONObject(i);add(server(x))}}
 }
 override suspend fun provisionConfig(deviceToken:String,request:ProvisioningRequest):Result<VpnConfigResponse>=runCatching{
  val body=JSONObject().put("deviceId",request.deviceId).put("publicKey",request.publicKey).put("fastest",request.fastest); request.serverId?.let{body.put("serverId",it)}
  val json=request("POST","/v1/vpn/config",deviceToken,body); val p=json.getJSONObject("peer")
  VpnConfigResponse(server(json.getJSONObject("server").let(::server)),peer=VpnPeerConfig(p.getString("serverPublicKey"),p.getString("address"),p.getString("dns"),p.getString("allowedIPs"),p.optInt("persistentKeepalive",25)),expiresAt=json.getString("expiresAt"))
 }
 private fun server(x:JSONObject)=VpnServer(x.getString("id"),x.getString("name"),x.getString("countryCode"),x.getString("city"),x.getString("hostname"),x.getInt("port"),x.getString("protocol"),x.optBoolean("premium",false),x.optBoolean("healthy",true),x.optInt("loadPercent",0),if(x.isNull("latencyMs"))null else x.optInt("latencyMs"))
 private fun request(method:String,path:String,deviceToken:String?,body:JSONObject?):JSONObject{
  val c=(URL(baseUrl.trimEnd('/')+path).openConnection() as HttpURLConnection).apply{requestMethod=method;connectTimeout=TIMEOUT_MS;readTimeout=TIMEOUT_MS;useCaches=false;setRequestProperty("Accept","application/json");setRequestProperty("User-Agent","VEXA-VPN/${BuildConfig.VERSION_NAME}");deviceToken?.let{setRequestProperty("Authorization","Bearer $it")};if(body!=null){doOutput=true;setRequestProperty("Content-Type","application/json")}}
  return try{if(body!=null)c.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))};val status=c.responseCode;val stream=if(status in 200..299)c.inputStream else c.errorStream;val text=stream?.bufferedReader()?.use{it.readText()}.orEmpty();if(status !in 200..299){val message=runCatching{JSONObject(text).optString("message")}.getOrNull().orEmpty();throw IllegalStateException(if(message.isNotBlank())message else "VEXA service returned HTTP $status.")};JSONObject(text)}finally{c.disconnect()}
 }
 companion object{private const val PREFS="vexa_provisioning";private const val KEY_DEVICE_TOKEN="device_token";private const val KEY_EXPIRES_AT="device_token_expires_at";private const val TIMEOUT_MS=15_000}
}
