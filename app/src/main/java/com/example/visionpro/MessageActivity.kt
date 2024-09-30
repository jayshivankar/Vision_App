package com.example.visionpro

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.Exception
import java.util.Locale

class MessageActivity : AppCompatActivity(),TextToSpeech.OnInitListener {

    private var tts:TextToSpeech? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        val editTextMultiline = findViewById<EditText>(R.id.editTextPhone)
        val editTextPhone = findViewById<EditText>(R.id.editTextPhone)
        val sendMsg = findViewById<Button>(R.id.sendMsg)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message)
        tts = TextToSpeech(this,this)

        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
            ){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS,Manifest.permission.SEND_SMS),111)
        } else
            receiveMsg()

        editTextMultiline.setOnClickListener {
            tts?.speak("Please speak your Message",TextToSpeech.QUEUE_FLUSH,null,null)
            Thread.sleep(2000)
            speakMsg()
        }
        editTextPhone.setOnClickListener {
            tts?.speak("Please speak recipent's phone number",TextToSpeech.QUEUE_FLUSH,null,null)
            Thread.sleep(2000)
            speakPhone()

        }
        sendMsg.setOnClickListener {
            val sms : SmsManager = SmsManager.getDefault()
            sms.sendTextMessage(
                editTextPhone.text.toString(),
                "ME",
                editTextMultiline.text.toString(),null,null)
            tts?.speak("Message sent successfully",TextToSpeech.QUEUE_FLUSH,null,null)

        }
    }

    private fun speakMsg(){
        val msgIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        msgIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        msgIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
        msgIntent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hi speak something")
        try {
            startActivityForResult(msgIntent,101)
        } catch (e:Exception){
            e.message?.let { Log.e("Speech Recognition","Error:${e.message}") }
        }
    }
    private fun speakPhone(){
        val phnIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        phnIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        phnIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
        phnIntent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hi speak something")
        try {
            startActivityForResult(phnIntent,102)
        } catch (e:Exception){
            e.message?.let { Log.e("Speech Recognition","Error:${e.message}") }
        }
    }

    private fun receiveMsg(){
        val br = object : BroadcastReceiver(){
           @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
           override fun onReceive(p0:Context?,p1:Intent?){
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                   for (sms:SmsMessage in Telephony.Sms.Intents.getMessagesFromIntent(p1)){
                       val editTextPhone = findViewById<EditText>(R.id.editTextPhone)
                       val editTextTextMultiLine = findViewById<EditText>(R.id.editTextTextMultiLine)
                       editTextTextMultiLine.setText(sms.displayMessageBody)
                       editTextPhone.setText(sms.originatingAddress)
                       Toast.makeText(applicationContext,"Message received",Toast.LENGTH_SHORT).show()
                       val text = """Message recevied from ${editTextPhone.text}"""
                       tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,null)
                       Thread.sleep(2000)
                       tts?.speak(editTextTextMultiLine.text,TextToSpeech.QUEUE_FLUSH,null,null)
                   }
               }
           }
        }
        registerReceiver(br, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101){
            if (resultCode == Activity.RESULT_OK && null!= data){
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val editTextTextMultiLine = findViewById<EditText>(R.id.editTextTextMultiLine)

                editTextTextMultiLine.setText(result?.get(0))
            }
        } else if(requestCode == 102){
            if( resultCode == Activity.RESULT_OK && null != data){
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val editTextPhone = findViewById<EditText>(R.id.editTextPhone)
                editTextPhone.setText(result?.get(0))
            }
        }
    }

    public override fun onDestroy() {
        if (tts!=null){
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    override fun onInit(p0: Int) {
        Log.d(TAG,"Initializing TTS")
        if(p0 == TextToSpeech.SUCCESS){
            Log.d(TAG,"SUCCESS")
            tts!!.language = Locale.US
            tts?.speak(
                "Messaging box opened",
                TextToSpeech.QUEUE_FLUSH,null,null

            )
        }

    }
}