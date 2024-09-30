package com.example.visionpro

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Task
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
import kotlin.collections.ArrayList


class TFLiteClassifier(private val context:Context) {
    private var interpreter:Interpreter? = null
    var isInitialized = false
        private set

    private var gpuDelegate: GpuDelegate? =null
    var labels = ArrayList<String>()

    private val executorService:ExecutorService = Executors.newCachedThreadPool()

    private var inputImageWidth:Int =0
    private var inputImageHeight:Int =0
    private var modelInputSize:Int =0

    fun initialize():Task<Void>{
        return call(executorService, Callable<Void> { initializeInterpreter()
            null }
        )
    }

}
@Throws(IOException::class)
private fun initializeInterpreter(){

    val assetManager = context.assets
    val model = loadModelFile(assetManager,"converted_model.tfLite")
    labels = loadLines(context,"labels.txt")
    val options = Interpreter.Options()
    gpuDelegate = GpuDelegate()
    options.addDelegate(gpuDelegate)
    val interpreter = Interpreter(model,options)

    val inputShape = interpreter.getInputTensor(0).shape()
    inputImageWidth = inputShape[1]
    inputImageHeight = inputShape[2]
    modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * CHANNEL_SIZE
    this.interpreter = interpreter
    isInitialized = true

}

@Throws(IOException::class)
private fun