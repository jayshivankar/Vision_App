package com.example.visionpro

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.Exception
import java.util.Locale
import kotlin.concurrent.thread

class PhoneActivity : AppCompatActivity(), View.OnClickListener,View.OnLongClickListener,TextToSpeech.OnInitListener {
    private var tts : TextToSpeech? = null
    private lateinit var phoneNum: EditText

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        val imageview2 = findViewById<ImageView>(R.id.imageview2)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_phone)
        phoneNum = findViewById(R.id.editTextPhone2)
        tts = TextToSpeech(this,this)

        imageview2.setOnClickListener{
            tts?.speak("Please speak recipient's phone number",TextToSpeech.QUEUE_FLUSH,null,null)
            Thread.sleep(2000)
            speakPhone()
        }
        val btn1 = findViewById<Button>(R.id.btn1)
        val btn2 = findViewById<Button>(R.id.btn2)
        val btn3 = findViewById<Button>(R.id.btn3)
        val btn4 = findViewById<Button>(R.id.btn4)
        val btn5 = findViewById<Button>(R.id.btn5)
        val btn6 = findViewById<Button>(R.id.btn6)
        val btn7 = findViewById<Button>(R.id.btn7)
        val btn8 = findViewById<Button>(R.id.btn8)
        val btn9 = findViewById<Button>(R.id.btn9)
        val btn0 = findViewById<Button>(R.id.btn0)
        val btnStar = findViewById<Button>(R.id.btnStar)
        val btnHash = findViewById<Button>(R.id.btnHash)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnCall = findViewById<Button>(R.id.btnCall)

        btn1.setOnClickListener(this)
        btn2.setOnClickListener(this)
        btn3.setOnClickListener(this)
        btn4.setOnClickListener(this)
        btn5.setOnClickListener(this)
        btn6.setOnClickListener(this)
        btn7.setOnClickListener(this)
        btn8.setOnClickListener(this)
        btn9.setOnClickListener(this)
        btn0.setOnClickListener(this)
        btnStar.setOnClickListener(this)
        btnCall.setOnClickListener(this)
        btnHash.setOnClickListener(this)
        btnBack.setOnClickListener(this)

        btnBack.setOnClickListener{
            val text : String = phoneNum.text.toString()
            if (text.isNotEmpty()){
                phoneNum.setText(text.substring(0,text.length - 1))
            }
        }
        btnCall.setOnClickListener{
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.CALL_PHONE)!= PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    111
                )
            } else {
            val editTextPhone2 = findViewById<EditText>(R.id.editTextPhone2)
            speak("Calling " + editTextPhone2.text.toString())
            Thread.sleep(2000)
            startCall()

            }
        true
        }
        btn1.setOnLongClickListener(this)
        btn2.setOnLongClickListener(this)
        btn3.setOnLongClickListener(this)
        btn4.setOnLongClickListener(this)
        btn5.setOnLongClickListener(this)
        btn6.setOnLongClickListener(this)
        btn7.setOnLongClickListener(this)
        btn8.setOnLongClickListener(this)
        btn9.setOnLongClickListener(this)
        btn0.setOnLongClickListener(this)
        btnStar.setOnLongClickListener(this)
        btnHash.setOnLongClickListener(this)
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun speak(text:String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    override fun onClick(view: View?) {
        val text = when(view?.id){
            R.id.btn1 -> "You clicked one"
            R.id.btn2 -> "You clicked two"
            R.id.btn3 -> "You clicked three"
            R.id.btn4 -> "You clicked four"
            R.id.btn5 -> "You clicked five"
            R.id.btn6 -> "You clicked six"
            R.id.btn7 -> "You clicked seven"
            R.id.btn8 -> "You clicked eight"
            R.id.btn9 -> "You clicked nine"
            R.id.btn0 -> "You clicked zero"
            R.id.btnStar -> "You clicked star"
            R.id.btnHash -> "You clicked hash"
            R.id.btnBack -> "You clicked delete"
            R.id.btnCall -> "You clicked call"
            else -> throw IllegalArgumentException("Undefined Clicked")
        }
        speak(text)

    }
        override fun onLongClick(view: View?): Boolean {
            val text = when(view?.id){
                R.id.btn1 -> "1"
                R.id.btn2 -> "2"
                R.id.btn3 -> "3"
                R.id.btn4 -> "4"
                R.id.btn5 -> "5"
                R.id.btn6 -> "6"
                R.id.btn7 -> "7"
                R.id.btn8 -> "8"
                R.id.btn9 -> "9"
                R.id.btn0 -> "0"
                R.id.btnStar -> "Star"
                R.id.btnHash-> "Hash"
                else -> IllegalArgumentException("Undefined Clicked")
            }
            phoneNum.append(text.toString())
            return true
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS){
            tts!!.language = Locale.US
            tts?.speak("Phone manager opened .",TextToSpeech.QUEUE_FLUSH,null,null)
        }
    }

    public override fun onDestroy() {
        if(tts != null){
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startCall(){
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:"+phoneNum.text)
        startActivity(callIntent)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 111)
            startCall()
    }
    private fun speakPhone(){
        val phnIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        phnIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        phnIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
        phnIntent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hi speak something")
        try {
            startActivityForResult(phnIntent,102)
        } catch (e:Exception){
            e.message?.let { Log.e("Phone",it) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 102){
            if (resultCode == Activity.RESULT_OK && null !=data){
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val editTextPhone2 = findViewById<EditText>(R.id.editTextPhone2)
                editTextPhone2.setText(result?.get(0))
            }
        }
    }

}