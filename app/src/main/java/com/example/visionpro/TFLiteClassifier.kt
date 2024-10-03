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
 //check for gpudelegate
    var gpuDelegate = GpuDelegate()
    options.addDelegate(gpuDelegate)
    val interpreter = Interpreter(model,options)

    val inputShape = interpreter.getInputTensor(0).shape()
     var inputImageWidth:Int =0
     var inputImageHeight:Int =0
     var modelInputSize:Int =0
    inputImageWidth = inputShape[1]
    inputImageHeight = inputShape[2]
    modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * CHANNEL_SIZE
    this.interpreter = interpreter
    isInitialized = true

}

@Throws(IOException::class)
private fun loadModelFile(assestManager: AssetManager,filename:String):ByteBuffer{
    val fileDescriptor = assestManager.openFd(filename)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength)
}

@Throws(IOException::class)
fun loadLines(context: Context,filename: String):ArrayList<String>{
    val s = Scanner(InputStreamReader(context.assets.open(filename)))
    val labels = ArrayList<String>()
    while (s.hasNextLine()){
        labels.add(s.nextLine())
    }
    s.close()
    return labels
}
private fun