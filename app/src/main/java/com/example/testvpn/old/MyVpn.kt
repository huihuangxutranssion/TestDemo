package com.example.testvpn.old

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MyVpn : VpnService() {

    private val SERVER_ADDR = "45.76.180.205"
    private val SERVER_PORT = 31368
    private val PASSWORD = "2981511c-becb-4b53-8a89-57cd3c97b328"
    private val METHOD = "chacha20-ietf-poly1305"

    private var vpnInterface: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    private var udpSocket: DatagramSocket? = null
    private var isRunning = false

    // 加密工具
    private val encryptCipher by lazy {
        Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
            val key = SecretKeySpec(PASSWORD.toByteArray(), "AES")
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    // 解密工具
    private val decryptCipher by lazy {
        Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
            val key = SecretKeySpec(PASSWORD.toByteArray(), "AES")
            init(Cipher.DECRYPT_MODE, key)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!setupVpnInterface()) {
            Log.e("MyVpn", "VPN接口创建失败")
            return START_NOT_STICKY
        }

        isRunning = true
        startDataForwarding()
        return START_STICKY
    }

    private fun setupVpnInterface(): Boolean {
        val builder = Builder()
            .setSession("SimpleVPN")
            .addAddress("45.76.180.205", 24)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish() ?: return false
        inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
        outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
        return true
    }

    private fun startDataForwarding() {
        udpSocket = DatagramSocket()

        // 本地 -> VPN -> 服务器
        Thread {
            val buffer = ByteArray(65535)
            while (isRunning) {
                try {
                    val len = inputStream?.read(buffer) ?: -1
                    if (len <= 0) break

                    val encryptedData = encrypt(buffer, len)
                    val packet = DatagramPacket(
                        encryptedData, encryptedData.size,
                        InetSocketAddress(SERVER_ADDR, SERVER_PORT)
                    )
                    udpSocket?.send(packet)
                } catch (e: Exception) {
                    Log.e("MyVpn", "发送失败: ${e.message}")
                }
            }
        }.start()

        // 服务器 -> VPN -> 本地
        Thread {
            val buffer = ByteArray(65535)
            while (isRunning) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)

                    val decryptedData = decrypt(packet.data, packet.length)
                    outputStream?.write(decryptedData)
                } catch (e: Exception) {
                    Log.e("MyVpn", "接收失败: ${e.message}")
                }
            }
        }.start()
    }

    private fun encrypt(data: ByteArray, len: Int): ByteArray {
        return try {
            encryptCipher.doFinal(data.copyOf(len))
        } catch (e: Exception) {
            Log.e("MyVpn", "加密失败: ${e.message}")
            data.copyOf(len)
        }
    }

    private fun decrypt(data: ByteArray, len: Int): ByteArray {
        return try {
            decryptCipher.doFinal(data.copyOf(len))
        } catch (e: Exception) {
            Log.e("MyVpn", "解密失败: ${e.message}")
            data.copyOf(len)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        udpSocket?.close()
        vpnInterface?.close()
        inputStream?.close()
        outputStream?.close()
    }
}
