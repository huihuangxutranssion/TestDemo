package com.example.testvpn.old

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration

object IpLocationUtils {
    private const val TAG = "IpLocationUtils"
    private const val IP_API_URL = "https://ipapi.co/json/"
    private val client = OkHttpClient()
    private val gson = Gson()

    // 回调接口
    interface OnIpLocationListener {
        fun onSuccess(location: IpLocation)
        fun onFailure(error: String)
    }

    // 获取公网IP及属地信息
    fun getPublicIpLocation(listener: OnIpLocationListener?) {
        val request = Request.Builder()
            .url(IP_API_URL)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(@NonNull call: Call, @NonNull e: IOException) {
                listener?.onFailure("获取IP信息失败: ${e.message}")
            }

            override fun onResponse(@NonNull call: Call, @NonNull response: Response) {
                try {
                    if (response.isSuccessful && response.body != null) {
                        val json = response.body!!.string()
                        val location = gson.fromJson(json, IpLocation::class.java)
                        listener?.onSuccess(location)
                    } else {
                        listener?.onFailure("获取IP信息失败，响应码: ${response.code}")
                    }
                } catch (e: Exception) {
                    listener?.onFailure("解析IP信息失败: ${e.message}")
                } finally {
                    response.close()
                }
            }
        })
    }

    // 获取本地IP地址
    fun getLocalIpAddress(): String? {
        return try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && !inetAddress.isLinkLocalAddress) {
                        return inetAddress.hostAddress
                    }
                }
            }
            null
        } catch (ex: SocketException) {
            Log.e(TAG, "获取本地IP地址失败", ex)
            null
        }
    }

    // 检查网络连接状态
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}

// 数据模型类
data class IpLocation(
    val ip: String,
    val city: String,
    val region: String,
    val country: String,
    val country_name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String
)
