package hackathon.facedetector

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import hackathon.CameraXViewModel
import hackathon.DeterminDir
import hackathon.cameraPermissionRequest
import hackathon.isPermissionGranted
import hackathon.openPermissionSetting
import meichu.hackathon.R
import meichu.hackathon.databinding.ActivityFaceDetectionBinding
import java.util.Locale
import java.util.Objects
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import kotlin.properties.Delegates

var lensFacing = LENS_FACING_FRONT
var faceAngle: Float = 0.0f
class FaceDetectionActivity : AppCompatActivity() {
    private var location: String by Delegates.observable("Center") { _, _, _ ->
        rectangleView.changeLocation(location)
        rectangleView.invalidate()
    }
    private var NoFace:Boolean = true
    private var err: Float = 250f
    private var rect: RectF = RectF(0f, 0f, 0f, 0f)
    private var command: String by Delegates.observable("center") { _, _, _ ->
        setLocation()
    }
    private lateinit var rectangleView: Rec
    private lateinit var binding: ActivityFaceDetectionBinding
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var imageCapture: ImageCapture
    private lateinit var tts: TextToSpeech
    private val REQUEST_CODE_SPEECH_INPUT = 1
    private var cameraSelector = CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build()
    private val cameraPermission = android.Manifest.permission.CAMERA
    private val cameraXViewModel = viewModels<CameraXViewModel>()
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private var faceDetectionTimer = Timer()
    private var screenWidth = 0
    private var screenHeight = 0
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isPermissionGranted(cameraPermission)) {
            requestCameraPermission()
        }
        super.onCreate(savedInstanceState)
        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.settingBtn.setOnClickListener{
            faceDetectionTimer.cancel()
            buildLocationDialog()
            binding.overlayView.alpha = 0.7f
        }

        var myUtilityClass = MyUtilityClass(this)
        val screenSize = myUtilityClass.getScreenSize()
        screenWidth = screenSize.first
        screenHeight = screenSize.second
        Log.d("height","$screenHeight")
        rectangleView = findViewById(R.id.Rec)
        rectangleView.setScreenSize(screenWidth, screenHeight)
        cameraXViewModel.value.processCameraProvider.observe(this) { provider ->
            processCameraProvider = provider
            bindCameraPreview()
            bindInputAnalyser()
            bindCameraCapture()
            bindCameraFlip()
        }
        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS){
                tts.language = Locale.US
                tts.setSpeechRate(1.0f)
            }
        })

        faceDetectionTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                DeterminDir(
                    rect.centerX(),
                    rect.centerY(),
                    rectangleView.centerX(),
                    rectangleView.centerY(),
                    tts,
                    err,
                    NoFace
                )
            }
        }, 0, 3000) // 1000 毫秒（1秒）更新一次
    }
    private var vertical = 1
    private var horizon = 1
    fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            // Is the button now checked?
            val checked = view.isChecked

            // Check which radio button was clicked
            when (view.getId()) {
                R.id.up_button ->
                    if (checked) {
                        vertical = 0
                    }
                R.id.middle_vertical_button ->
                    if (checked) {
                        vertical = 1
                    }
                R.id.down_button ->
                    if (checked) {
                        vertical = 2
                    }
                R.id.left_button ->
                    if(checked) {
                        horizon = 0
                    }
                R.id.middle_horizontal_button ->
                    if(checked) {
                        horizon = 1
                    }
                R.id.right_button ->
                    if(checked) {
                        horizon = 2
                    }
            }
        }

    }
    private fun updateLifecycle(){
        try {
            processCameraProvider.bindToLifecycle(this, cameraSelector, imageCapture,
                imageAnalysis, cameraPreview)
        } catch (illegalStateException: IllegalStateException) {
            Log.e("TAG", illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e("TAG", illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }
    private fun takePhoto(){
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW_IMAGE")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues).build()
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                }
            }
        )
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            faceDetectionTimer.cancel()
            voiceControl()
            return true
        }
//        if(event.keyCode == KeyEvent.KEYCODE_VOLUME_UP){
//            takePhoto()
//            updateLifecycle()
//        }
        return super.dispatchKeyEvent(event)
    }
    private fun voiceControl(){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        // on below line we are passing language model
        // and model free form in our intent
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        // on below line we are passing our
        // language as a default language.
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )

        // on below line we are specifying a prompt
        // message as speak to text on below line.
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

        // on below line we are specifying a try catch block.
        // in this block we are calling a start activity
        // for result method and passing our result code.
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            // on below line we are displaying error message in toast
            Toast.makeText(
                    this@FaceDetectionActivity, " " + e.message,
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // in this method we are checking request
        // code with our result code.
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            // on below line we are checking if result code is ok
            if (resultCode == RESULT_OK && data != null) {

                // in that case we are extracting the
                // data from our array list
                val res: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                // on below line we are setting data
                // to our output text view.
                Log.d("voice", Objects.requireNonNull(res)[0])
                command = Objects.requireNonNull((res)[0]).toLowerCase().split(" ")
                    .joinToString(separator = "_")

                faceDetectionTimer = Timer()
                faceDetectionTimer.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        DeterminDir(
                            rect.centerX(),
                            rect.centerY(),
                            rectangleView.centerX(),
                            rectangleView.centerY(),
                            tts,
                            err,
                            NoFace
                        )
                    }
                }, 0, 3000) // 1000 毫秒（1秒）更新一次
            }
        }
    }
    private fun setLocation() {
        if((vertical == 0 && horizon == 0) || command == "left_top") {
            location = "Left_Top"
        } else if((vertical == 0 && horizon == 1 ) || command == "top") {
            location = "Top"
        } else if((vertical == 0 && horizon == 2) || command == "right_top") {
            location = "Right_Top"
        } else if((vertical == 1 && horizon == 0) || command == "left"){
            location = "Left"
        } else if((vertical == 1 && horizon == 2) || command == "right"){
            location = "Right"
        } else if((vertical == 2 && horizon == 0) || command == "left_bottom"){
            location = "Left_Bottom"
        } else if((vertical == 2 && horizon == 1) || command == "bottom"){
            location = "Bottom"
        } else if((vertical == 2 && horizon == 2) || command == "right_bottom"){
            location = "Right_Bottom"
        } else if((vertical == 1 && horizon == 1) || command == "center"){
            location = "Center"
        }
    }
    private fun buildLocationDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("請設定位置")
        builder.setCancelable(false)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        builder.setView(inflater.inflate(R.layout.select_location, null))

        builder.setPositiveButton(
            "確定"
        ) { _, _ ->
            setLocation()
            buildAngleDialog()
        }
        builder.setNegativeButton(
            "取消"
        ){  _, _ ->
            binding.overlayView.alpha = 0f
            faceDetectionTimer = Timer()
            faceDetectionTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    DeterminDir(
                        rect.centerX(),
                        rect.centerY(),
                        rectangleView.centerX(),
                        rectangleView.centerY(),
                        tts,
                        err,
                        NoFace
                    )
                }
            }, 0, 3000) // 1000 毫秒（1秒）更新一次
        }
        val dialog = builder.create()
        dialog.show()
    }
    private fun buildAngleDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("請設定角度")
        builder.setCancelable(false)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        builder.setView(inflater.inflate(R.layout.select_angle, null))

        builder.setPositiveButton(
            "確定"
        ) { _, _ ->
            binding.overlayView.alpha = 0f
            faceDetectionTimer = Timer()
            faceDetectionTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    DeterminDir(
                        rect.centerX(),
                        rect.centerY(),
                        rectangleView.centerX(),
                        rectangleView.centerY(),
                        tts,
                        err,
                        NoFace
                    )
                }
            }, 0, 3000) // 1000 毫秒（1秒）更新一次
        }
        builder.setNegativeButton(
            "取消"
        ){  _, _ ->
            binding.overlayView.alpha = 0f
            faceDetectionTimer = Timer()
            faceDetectionTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    DeterminDir(
                        rect.centerX(),
                        rect.centerY(),
                        rectangleView.centerX(),
                        rectangleView.centerY(),
                        tts,
                        err,
                        NoFace
                    )
                }
            }, 0, 3000) // 1000 毫秒（1秒）更新一次
        }
        val dialog = builder.create()
        dialog.show()
    }
    private fun bindCameraFlip(){
        binding.lenSwitchBtn.setOnClickListener {
             lensFacing = if (lensFacing == LENS_FACING_BACK) {
                 LENS_FACING_FRONT
             } else {
                 LENS_FACING_BACK
             }
            cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            println("Hello, $lensFacing")
            processCameraProvider.unbindAll()
            updateLifecycle()
        }
    }
    private fun requestCameraPermission() {
        when {
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                cameraPermissionRequest(
                    positive = { openPermissionSetting() }
                )
            }
            else -> {
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }
    private fun bindCameraCapture() {
        imageCapture = ImageCapture.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
        binding.captureBtn.setOnClickListener {
            takePhoto()
        }
        updateLifecycle()
    }
    private fun bindCameraPreview() {
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
        try {
            processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
        } catch (illegalStateException: IllegalStateException) {
            Log.e("TAG", illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e("TAG", illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    private fun bindInputAnalyser() {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .build()
        )
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(detector, imageProxy)
        }

        try {
            processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
        } catch (illegalStateException: IllegalStateException) {
            Log.e("TAG", illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e("TAG", illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(detector: FaceDetector, imageProxy: ImageProxy) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage).addOnSuccessListener { faces ->
            binding.graphicOverlay.clear()
            if(faces.size == 0) NoFace = true
            faces.forEach { face ->
                NoFace = false
                val faceBox = FaceBox(binding.graphicOverlay, face, imageProxy.image!!.cropRect)
                binding.graphicOverlay.add(faceBox)
                rect = faceBox.returnFace()
                faceAngle = face.headEulerAngleZ
                Log.d("Facepos","face:${rect.centerX()}")
                Log.d("Facepos","rect:${rectangleView.centerX()}")
            }
        }.addOnFailureListener {
            it.printStackTrace()
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }


}