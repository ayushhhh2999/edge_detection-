package com.div.edgedetectionapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.opengl.GLSurfaceView

class MainActivity : AppCompatActivity() {

    private val width = 640
    private val height = 480

    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private var sensorOrientation: Int = 0

    private lateinit var renderer: GLRenderer
    private lateinit var wsServer: FrameWebSocketServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wsServer = FrameWebSocketServer(8080)
        wsServer.start()


        val glSurfaceView = findViewById<GLSurfaceView>(R.id.gl_surface_view)
        renderer = GLRenderer()
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY


        val toggleButton = findViewById<Button>(R.id.toggleButton)
        toggleButton.setOnClickListener {
            renderer.showEdges = !renderer.showEdges
        }


        val fpsText = findViewById<TextView>(R.id.fpsText)
        var lastTime = System.currentTimeMillis()
        var frameCount = 0

        // ImageReader to get camera frames
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val yPlane = image.planes[0]
            val buffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val padded = ByteArray(rowStride * height)
            buffer.get(padded)

            val inputArray = ByteArray(width * height)
            for (r in 0 until height) {
                System.arraycopy(padded, r * rowStride, inputArray, r * width, width)
            }

            val outputArray = ByteArray(width * height)
            NativeLib.processFrame(inputArray, width, height, outputArray)

            val rotation = calculateRotationDegrees()
            renderer.rotationDegrees = rotation
            renderer.updateTexCoords()

            val displayArray = if (renderer.showEdges) outputArray else inputArray
            renderer.updateFrame(displayArray, width, height)

            // FPS
            frameCount++
            val now = System.currentTimeMillis()
            if (now - lastTime >= 1000) {
                val fps = frameCount * 1000 / (now - lastTime)
                runOnUiThread { fpsText.text = "FPS: $fps" }
                frameCount = 0
                lastTime = now
            }

            wsServer.sendFrame(outputArray)

            glSurfaceView.requestRender()
            image.close()
        }, null)

        // request permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            openCamera()
        }
    }




    private fun calculateRotationDegrees(): Int {
        val windowRotation = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        val rotationDegreesFromDevice = when (windowRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        return (sensorOrientation - rotationDegreesFromDevice + 360) % 360
    }

    private fun openCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0] // back camera

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }, null)
    }

    private fun startPreview() {
        val requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        requestBuilder.addTarget(imageReader.surface)

        cameraDevice.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                captureSession.setRepeatingRequest(requestBuilder.build(), null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }, null)
    }
}