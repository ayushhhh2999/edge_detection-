package com.div.edgedetectionapp

import org.java_websocket.server.WebSocketServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import java.net.InetSocketAddress
import android.util.Base64
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.util.Log

class FrameWebSocketServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {
    private val TAG = "FrameWebSocketServer"
    private val clients = mutableSetOf<WebSocket>()

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        clients.add(conn)
        Log.d(TAG, "Client connected: ${conn.remoteSocketAddress}")
        conn.send("Connected to Edge Detection Server")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        clients.remove(conn)
        Log.d(TAG, "Client disconnected: ${conn.remoteSocketAddress}, reason: $reason")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        Log.d(TAG, "Message received: $message")
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.e(TAG, "WebSocket error", ex)
        ex?.printStackTrace()
    }

    override fun onStart() {
        Log.d(TAG, "WebSocket server started on port: $port")
    }

    fun sendFrame(byteArray: ByteArray, width: Int = 640, height: Int = 480) {
        try {
            if (clients.isEmpty()) {
                Log.w(TAG, "No clients connected to send frame to")
                return
            }

            if (byteArray.size < width * height) {
                Log.e(TAG, "ByteArray size ${byteArray.size} is less than expected ${width * height}")
                return
            }

            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)

            // Convert grayscale bytes to ARGB pixels
            for (i in 0 until width * height) {
                val v = byteArray[i].toInt() and 0xFF
                pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
            bmp.setPixels(pixels, 0, width, 0, 0, width, height)

            // compress
            val baos = ByteArrayOutputStream()
            if (!bmp.compress(Bitmap.CompressFormat.PNG, 80, baos)) {
                Log.e(TAG, "Failed to compress bitmap")
                return
            }

            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            val msg = "data:image/png;base64,$base64"

            val iterator = clients.iterator()
            while (iterator.hasNext()) {
                val client = iterator.next()
                try {
                    if (client.isOpen) {
                        client.send(msg)
                    } else {
                        Log.w(TAG, "Client connection is not open, removing")
                        iterator.remove()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending to client", e)
                    iterator.remove()
                }
            }

            Log.d(TAG, "Frame sent to ${clients.size} clients")

        } catch (e: Exception) {
            Log.e(TAG, "Error in sendFrame", e)
        }
    }

}