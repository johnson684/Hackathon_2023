package hackathon.facedetector

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
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
class FaceDetectionActivity : AppCompatActivity() {
    private var location: String by Delegates.observable("Center") { property, oldValue, newValue ->
        rectangleView.changeLocation(location)
        rectangleView.invalidate()
    }
    private var dis: Float?= null
    private var err: Float = 250f
    private lateinit var rectangleView: Rec
    private lateinit var binding: ActivityFaceDetectionBinding
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var imageCapture: ImageCapture
    private lateinit var tts: TextToSpeech
    private lateinit var checkBox: CheckBox
    private val REQUEST_CODE_SPEECH_INPUT = 1
    private var cameraSelector = CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build()
    private val cameraPermission = android.Manifest.permission.CAMERA
    private val cameraXViewModel = viewModels<CameraXViewModel>()
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val faceDetectionTimer = Timer()
    private var screenWidth = 0
    private var screenHeight = 0


    private val checkBoxList = mutableListOf<CheckBox>()
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
//            SettingActivity.startActivity(this)
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
//        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
//            if (it == TextToSpeech.SUCCESS){
//                tts.language = Locale.US
//                tts.setSpeechRate(1.0f)
//            }
//        })

//        faceDetectionTimer.scheduleAtFixedRate(object : TimerTask() {
//            override fun run() {
//                tts.speak("Turn your phone right", TextToSpeech.QUEUE_ADD, null)
//            }
//        }, 0, 3000) // 1000 毫秒（1秒）更新一次

        faceDetectionTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                //tts.speak("Turn your phone right", TextToSpeech.QUEUE_ADD, null)
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
    fun updateLifecycle(){
        try {
            processCameraProvider.bindToLifecycle(this, cameraSelector, imageCapture,
                imageAnalysis, cameraPreview)
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }
    fun takePhoto(){
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
            Toast
                .makeText(
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
            }
        }
    }
    private fun buildButtonOnClickListener(){
//        checkBox = findViewById<CheckBox>(R.id.button1)
//        checkBox.isChecked = true
        for (i in 0..8) {
//            val checkBoxId = resources.getIdentifier("button$i", "id", packageName)
//            val checkBox = findViewById<CheckBox>(checkBoxId)

//            checkBox.setOnClickListener(View.OnClickListener {
//                resetAllButton()
//            })
//            checkBoxList.add(checkBox)
        }
    }
    private fun resetAllButton(){
//        for (i in 0..8) {
//            val buttonId = resources.getIdentifier("button$i", "id", packageName)
//            val checkBoxObj = findViewById<CheckBox>(buttonId)
//            checkBoxObj.isChecked = false
//        }
    }
    private fun setLocation() {
        if(vertical == 0 && horizon == 0) {
            location = "Left_Top"
        } else if(vertical == 0 && horizon == 1) {
            location = "Top"
        } else if(vertical == 0 && horizon == 2) {
            location = "Right_Top"
        } else if(vertical == 1 && horizon == 0) {
            location = "Left"
        } else if(vertical == 1 && horizon == 1) {
            location = "Center"
        } else if(vertical == 1 && horizon == 2) {
            location = "Right"
        } else if(vertical == 2 && horizon == 0) {
            location = "Left_Bottom"
        } else if(vertical == 2 && horizon == 1) {
            location = "Bottom"
        } else if(vertical == 2 && horizon == 2) {
            location = "Right_Bottom"
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
        ) { dialog, which ->
            buildAngleDialog()
        }
        builder.setNegativeButton(
            "取消"
        ){  dialog, which ->
            binding.overlayView.alpha = 0f
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
        ) { dialog, which ->
            binding.overlayView.alpha = 0f
        }
        builder.setNegativeButton(
            "取消"
        ){  dialog, which ->
            binding.overlayView.alpha = 0f
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
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
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
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(detector: FaceDetector, imageProxy: ImageProxy) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage).addOnSuccessListener { faces ->
            binding.graphicOverlay.clear()
            faces.forEach { face ->
                val faceBox = FaceBox(binding.graphicOverlay, face, imageProxy.image!!.cropRect)
                binding.graphicOverlay.add(faceBox)
                var rect = faceBox.returnFace()

                Log.d("facebox","$rect")
//                if((faceBox.face.boundingBox.centerX() - rectangleView.centerX()).absoluteValue > err){
//                    if((faceBox.face.boundingBox.centerX() > rectangleView.centerX() && lensFacing == LENS_FACING_FRONT) || (faceBox.face.boundingBox.centerX() < rectangleView.centerX() && lensFacing == LENS_FACING_BACK)){
//                        Log.d("move", "turn left")
//                    }
//                    else{
//                        Log.d("move", "turn right")
//                    }
//                }
//                else{
//                    if((faceBox.face.boundingBox.centerY() - rectangleView.centerY()).absoluteValue > err) {
//                        if ((faceBox.face.boundingBox.centerY() > rectangleView.centerY() && lensFacing == LENS_FACING_FRONT) || (faceBox.face.boundingBox.centerY() < rectangleView.centerY() && lensFacing == LENS_FACING_BACK)) {
//                            Log.d("move", "turn up")
//                        } else {
//                            Log.d("move", "turn down")
//                        }
//                    }
//                }
            }
        }.addOnFailureListener {
            it.printStackTrace()
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }

    companion object {
        private val TAG = FaceDetectionActivity::class.simpleName
        fun startActivity(context: Context) {
            Intent(context, FaceDetectionActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}