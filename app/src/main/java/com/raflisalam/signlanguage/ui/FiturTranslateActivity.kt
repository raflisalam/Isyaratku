package com.raflisalam.signlanguage.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.core.view.isGone
import com.raflisalam.signlanguage.databinding.ActivityFiturTranslateBinding
import com.raflisalam.signlanguage.utils.factory.CameraProcess
import com.raflisalam.signlanguage.utils.factory.ImageAnalyse
import com.raflisalam.signlanguage.viewmodel.FiturTranslateViewModel
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class FiturTranslateActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var _binding : ActivityFiturTranslateBinding? = null
    private val binding: ActivityFiturTranslateBinding get() = _binding!!

    private val viewModel: FiturTranslateViewModel by viewModels()
    lateinit var  cameraPreviewView: PreviewView
    lateinit var cameraProcess : CameraProcess
    lateinit var fullImageAnalyse: ImageAnalyse

    private var textToSpeech: TextToSpeech? = null
    private var final_output_text: String = ""
    private var current_output_text: String = ""
    private var text_voice: String = ""


    //pengujian runtime
    private val timer = Timer()
    private var secondsPassed = 0
    private var isTimerRunning = false

    private var executor: ScheduledThreadPoolExecutor? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityFiturTranslateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initiate The Preparation
        viewModel.initModel(baseContext)
        viewModel.isLoading.observe(this@FiturTranslateActivity) {
            binding.progressBar.isGone = !it;
        }
        cameraProcess = CameraProcess()
        cameraPreviewView = binding.cameraPreviewWrap
        if(!cameraProcess.allPermissionGranted(this@FiturTranslateActivity)) {
            cameraProcess.requestPermission(this@FiturTranslateActivity)
        }

        val actionBar = supportActionBar
        actionBar?.hide()

        setupButton()
    }

    private fun setupButton() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnClear.setOnClickListener {
            final_output_text = ""
            text_voice = ""
            binding.outputTranslate.text = final_output_text
        }

        binding.btnAdd.setOnClickListener {
            final_output_text = "$final_output_text $current_output_text"
            text_voice = final_output_text
            binding.outputTranslate.text = final_output_text
            convertTextToSpeech(text_voice)
        }

        binding.btnStartTimer.setOnClickListener {
            startTimer()
        }

        binding.btnResetTimer.setOnClickListener {
            resetTimer()
        }

    }

    private fun resetTimer() {
        isTimerRunning = false
        secondsPassed = 0
        binding.timerCount.text = "Waktu: proses deteksi ${secondsPassed} detik"
    }

    private fun startTimer() {
       /* if (!isTimerRunning) {
            val timerTask =  object : TimerTask() {
                override fun run() {
                    // Hentikan timer kalau current_output_text sudah terisi output label
                    if (current_output_text.isNotEmpty()) {
                        stopTimer()
                    } else {
                        //Parsing timer count ke textview
                        runOnUiThread {
                            binding.timerCount.text = "Waktu: proses deteksi label ${secondsPassed} detik"
                            Log.d("TIMER_COUNT", "Waktu: proses deteksi label ${secondsPassed} detik")
                        }
                        secondsPassed++
                    }
                }
            }

            timer.schedule(timerTask, 0, 1000) // Timer akan berjalan setiap 1 detik
            isTimerRunning = true
        }*/

        if (executor == null || executor!!.isShutdown) {
            executor = ScheduledThreadPoolExecutor(1)
            executor!!.scheduleAtFixedRate({
                runOnUiThread {
                    binding.timerCount.text = "Waktu: proses deteksi label ${secondsPassed} detik"
                    Log.d("TIMER_COUNT", "Waktu: proses deteksi label ${secondsPassed} detik")
                }
                secondsPassed++
            }, 0, 1000, TimeUnit.MILLISECONDS)
            isTimerRunning = true
        }
    }

    private fun stopTimer() {
        /*timer.cancel()
        timer.purge()
        isTimerRunning = false*/

        executor?.let {
            it.shutdownNow()
            it.awaitTermination(1, TimeUnit.SECONDS)
            executor = null
        }
        isTimerRunning = false
    }

    private fun convertTextToSpeech(textVoice: String) {
        textToSpeech = TextToSpeech(applicationContext, this)
        binding.btnVoice.isEnabled = false
        binding.btnVoice.setOnClickListener { speakOut(textVoice) }
    }

    override fun onResume() {
        super.onResume()

        viewModel.yoloV5Model.observe(this@FiturTranslateActivity) {
            fullImageAnalyse = ImageAnalyse(
                this,
                cameraPreviewView,
                this.windowManager.defaultDisplay.rotation,
                it,
                graphicOverlay = binding.graphicOverlay,
                onResult = { label ->
                    current_output_text = label
                    binding.outputTranslate.text = label
                    Log.d("TAG", current_output_text)
                    stopTimer()

                    binding.timerCount.text = "Waktu: label ${current_output_text} berhasil dideteksi dan berhenti pada detik ${secondsPassed}"
                    Log.d("TIMER_COUNT_STOP", "Waktu: label ${current_output_text} berhasil dideteksi dan berhenti pada detik ${secondsPassed}")
                }
            )
            cameraProcess.startCamera(this, fullImageAnalyse, cameraPreviewView)
        }

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech!!.setLanguage(Locale("id-ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            } else {
                binding.btnVoice.isEnabled = true
            }
        }
    }

    private fun speakOut(output: String) {
        textToSpeech!!.speak(output, TextToSpeech.QUEUE_FLUSH, null, "")
    }
}