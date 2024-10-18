package com.example.visionpro

import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tasks.Tasks.call
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Executors.callable
import kotlin.collections.ArrayList



class TFLiteClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    var isInitialized = false
        private set

    private var gpuDelegate: GpuDelegate? = null
    var labels = ArrayList<String>()

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0
    private var modelInputSize: Int = 0

    fun initialize(): Task<Void> {
        return Tasks.call(executorService, Callable<Void> {
            initializeInterpreter()
            null
        })
    }

    @Throws(IOException::class)
    private fun initializeInterpreter() {
        val assetManager = context.assets
        val model = loadModelFile(assetManager, "converted_model.tflite")
        labels = loadLines(context, "labels.txt")
        val options = Interpreter.Options()

        // Initialize GPU delegate
        gpuDelegate = GpuDelegate()
        options.addDelegate(gpuDelegate)

        // Create interpreter with the model and options
        interpreter = Interpreter(model, options)

        // Get input shape and set image size and model input size
        val inputShape = interpreter!!.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * CHANNEL_SIZE

        // Mark as initialized
        isInitialized = true
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @Throws(IOException::class)
    private fun loadLines(context: Context, filename: String): ArrayList<String> {
        val scanner = Scanner(InputStreamReader(context.assets.open(filename)))
        val labels = ArrayList<String>()
        while (scanner.hasNextLine()) {
            labels.add(scanner.nextLine())
        }
        scanner.close()
        return labels
    }

    private fun getMaxResult(result: FloatArray): Int {
        var maxProbability = result[0]
        var index = 0
        for (i in result.indices) {
            if (result[i] > maxProbability) {
                maxProbability = result[i]
                index = i
            }
        }
        return index
    }

    private fun classify(bitmap: Bitmap): String {
        check(isInitialized) { "TF Lite Interpreter is not initialized yet." }
        val resizedImage = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedImage)
        val output = Array(1) { FloatArray(labels.size) }
        val startTime = SystemClock.uptimeMillis()
        interpreter?.run(byteBuffer, output)
        val endTime = SystemClock.uptimeMillis()
        val index = getMaxResult(output[0])
        val result = "Prediction is ${labels[index]}"
        return result
    }

    fun classifyAsync(bitmap: Bitmap): Task<String> {
        return Tasks.call(executorService, Callable<String> { classify(bitmap) })
    }

    fun close() {
        Tasks.call(executorService, Callable<Void> {
            interpreter?.close()
            gpuDelegate?.close()
            gpuDelegate = null
            interpreter = null
            isInitialized = false
            Log.d(TAG, "Closed TFLite interpreter")
            null
        })
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0

        for (i in 0 until inputImageWidth) {
            for (j in 0 until inputImageHeight) {
                val pixelVal = pixels[pixel++]

                // Extract RGB values and normalize them
                byteBuffer.putFloat(((pixelVal shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((pixelVal shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((pixelVal and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        bitmap.recycle()

        return byteBuffer
    }

    companion object {
        private const val TAG = "TfliteClassifier"
        private const val FLOAT_TYPE_SIZE = 4
        private const val CHANNEL_SIZE = 3
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
    }
}
