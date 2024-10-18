package com.example.visionpro

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.Manifest
import android.health.connect.datatypes.units.Length
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.ByteArrayOutputStream
import java.util.Locale

class CameraActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var lensFacing = CameraSelector.LENS_FACING_BACK // Corrected the camera selector value
    private val TAG = "CameraActivity"
    private val REQUEST_CODE_PERMISSIONS = 101
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA) // Correct permission constant
    private lateinit var tfLiteClassifier: TFLiteClassifier // Lazy initialization
    private var tts: TextToSpeech? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set content view before accessing views
        setContentView(R.layout.activity_camera)

        // Now you can find views
        val textureView = findViewById<TextureView>(R.id.textureview)
        val predictedTextView = findViewById<TextView>(R.id.predictedTextView)


        // Initialize TextToSpeech
        tts = TextToSpeech(this, this)

        // Initialize TFLiteClassifier
        tfLiteClassifier = TFLiteClassifier(this)

        // Check permissions and start the camera
        if (allPermissionsGranted()) {
            textureView.post { startCamera() }
            textureView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateTransform(textureView) }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set click listener for textureView
        textureView?.setOnClickListener() {
            // Use the predictedTextView text in TextToSpeech
            tts?.speak(predictedTextView.text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }

        // Initialize TFLiteClassifier asynchronously
        tfLiteClassifier
            .initialize()
            .addOnSuccessListener {
                // Handle success - setup done
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error in setting up the classifier", e)
            }
    }

    private fun startCamera() {
        val textureView = findViewById<TextureView>(R.id.textureview)
        val predictedTextView = findViewById<TextView>(R.id.predictedTextView)


        // Get the aspect ratio based on the textureView size (optional adjustment)
        val screenAspectRatio = AspectRatio.RATIO_4_3

        // Set up the preview use case
        val preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio) // 4:3 aspect ratio
            .setTargetRotation(textureView.display.rotation) // Rotation
            .build()

        // Set up the ImageAnalysis use case
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Backpressure strategy
            .build()

        // Analyzer for processing the image frames
        imageAnalyzer.setAnalyzer(
            ContextCompat.getMainExecutor(this),
            { imageProxy: ImageProxy ->
                // Convert ImageProxy to Bitmap
                val bitmap = imageProxy.toBitmap()

                // Pass the bitmap to your classifier
                tfLiteClassifier
                    .classifyAsync(bitmap)
                    .addOnSuccessListener { resultText ->
                        predictedTextView?.text = resultText
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Error during classification", error)
                    }

                // Close the image when done
                imageProxy.close()
            })

        // Get the camera provider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Select the camera (back or front)
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind preview and image analysis use cases to lifecycle
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

        // Set the surface provider for the TextureView
        preview.setSurfaceProvider { request ->
            val surfaceTexture = textureView.surfaceTexture
            if (surfaceTexture != null) {
                val surface = Surface(surfaceTexture)
                request.provideSurface(surface, ContextCompat.getMainExecutor(this)) {
                    // Handle any result here after the surface is no longer needed.
                }
            }
        }
    }

    // Converts ImageProxy to Bitmap
    fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun updateTransform(textureView: TextureView) {
        val matrix = Matrix()
        val centerX = textureView.width / 2f
        val centerY = textureView.height / 2f

        val rotationDegrees = when (textureView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(rotationDegrees.toFloat(), centerX, centerY)
        textureView.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permission not granted ", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                this, permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    public override fun onDestroy() {
        tfLiteClassifier.close()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.speak("Currency Recognizer opened", TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }
}
