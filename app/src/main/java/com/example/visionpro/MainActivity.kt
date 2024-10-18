package com.example.visionpro

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class MainActivity : AppCompatActivity(),View.OnClickListener ,View.OnLongClickListener,TextToSpeech.OnInitListener{

    private var tts : TextToSpeech? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this,this)
        //val text = "welcome to visionpro.single tap for details and long press to open an activity"
        //speak-text
        val msgBox = findViewById<RelativeLayout>(R.id.msgbox)
        val phoneMngr = findViewById<RelativeLayout>(R.id.phoneMngr)
        val timeDate = findViewById<RelativeLayout>(R.id.timeDate)
        val cameracard = findViewById<RelativeLayout>(R.id.cameraCard)
        msgBox.setOnClickListener(this)
        phoneMngr.setOnClickListener(this)
        timeDate.setOnClickListener(this)
        cameracard.setOnClickListener(this)

        msgBox.setOnLongClickListener(this)
        phoneMngr.setOnLongClickListener(this)
        timeDate.setOnLongClickListener(this)
        cameracard.setOnLongClickListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onClick(view:View){
        try {
            val text = when (view.id) {
                R.id.msgbox -> "You Clicked messaging"
                R.id.phoneMngr -> "You Clicked phone manager"
                R.id.timeDate -> "You Clicked Time,Date and Battery Status"
                R.id.cameraCard -> "You Clicked phone camera"
                else -> throw IllegalArgumentException("Undefiend Clicked")
            }
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            speak(text)
        } catch (e:Exception){
            e.printStackTrace()
        }

    }

    override fun onLongClick(view: View?): Boolean {
        val intent = when (view?.id){
            R.id.msgbox -> Intent(this,MessageActivity::class.java)
            R.id.phoneMngr ->Intent(this,PhoneActivity::class.java)
            R.id.timeDate -> Intent(this,DateTimeActivity::class.java)
            R.id.cameraCard -> Intent(this,CameraActivity::class.java)
            else -> throw IllegalArgumentException("Undefined CLicked")
        }
        if (intent!= null && intent.resolveActivity(packageManager)!= null){
        startActivity(intent)
        return true
        }
        Toast.makeText(this,"Activity not found",Toast.LENGTH_SHORT).show()
        return false
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun speak(text:String){
        tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,null)
    }
    override fun onInit(status: Int) {
        if(status == TextToSpeech.SUCCESS){
            tts!!.language = Locale.US
            tts?.speak("Welcome to VisionPro app . Single tap for details and long press to open an activity",TextToSpeech.QUEUE_FLUSH,null,null)

        }
    }

    public override fun onDestroy() {
        // shutdown TTS
        if (tts != null){
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
}