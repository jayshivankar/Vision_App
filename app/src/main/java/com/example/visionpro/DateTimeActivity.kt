package com.example.visionpro

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

val tag = "SPEECH"

class DateTimeActivity : AppCompatActivity(),View.OnClickListener,TextToSpeech.OnInitListener {
    private var tts:TextToSpeech? = null
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_date_time)
        tts = TextToSpeech(this,this)
        val batteryCard = findViewById<ImageView>(R.id.batteryCard)
        batteryCard.setOnClickListener(this)

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onClick(view: View?) {
        if (view?.id == R.id.batteryCard){
            val currDate = findViewById<TextView>(R.id.currDateTime)
            val date = getCurrentDateTime()
            val dateInString = date.toString("E,dd MMMM yyyy HH:mm:ss")
            currDate.text = dateInString
            tts?.speak(dateInString,TextToSpeech.QUEUE_FLUSH,null,null)
            GlobalScope.launch(Dispatchers.Main){
                delay(3000)
            }
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { iFilter -> this.registerReceiver(null,iFilter) }
            val status : Int =batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS,-1) ?: -1
            var isCharging = when(status){
                BatteryManager.BATTERY_STATUS_CHARGING -> "Phone is Charging"
                else -> "Phone is not Charging"
            }
            val txtView = findViewById<TextView>(R.id.batteryStatus)
            txtView.text = isCharging
            val batstate = "Your $isCharging"

            val batteryPct : Float? = batteryStatus?.let { intent ->
                val level:Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)
                val scale:Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1)
                level * 100/scale.toFloat()
            }
            val txtview = findViewById<TextView>(R.id.batteryPer)
            txtview.text = batteryPct.toString()
            val bst = "Your battery level is " + batteryPct.toString() + "percent and $batstate"
            tts?.speak(bst,TextToSpeech.QUEUE_FLUSH,null,null)

        }
    }

    public override fun onDestroy() {
        if (tts != null){
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            tts!!.language = Locale.US
            tts?.speak("Time,date and battery status opened.", TextToSpeech.QUEUE_FLUSH, null, null)

        }
    }
    private fun Date.toString(format:String):String{
        val formatter = SimpleDateFormat(format)
        return formatter.format(this)
    }
    private fun getCurrentDateTime():Date{
        return Calendar.getInstance().time
    }
}