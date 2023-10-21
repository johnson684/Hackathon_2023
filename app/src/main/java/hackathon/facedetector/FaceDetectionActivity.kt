package hackathon.facedetector

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
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
import hackathon.setting.SettingActivity
import meichu.hackathon.R
import meichu.hackathon.databinding.ActivityFaceDetectionBinding
import java.util.Locale
import java.util.Timer
import java.util.concurrent.Executors
import kotlin.properties.Delegates

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
    private var cameraSelector = CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build()
    private var lensFacing = LENS_FACING_FRONT
    private val cameraPermission = android.Manifest.permission.CAMERA
    private val cameraXViewModel = viewModels<CameraXViewModel>()
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val faceDetectionTimer = Timer()
    private var screenWidth = 0
    private var screenHeight = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isPermissionGranted(cameraPermission)) {
            requestCameraPermission()
        }
        super.onCreate(savedInstanceState)
        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)

        setContentView(binding.root)
        binding.settingBtn.setOnClickListener{
            SettingActivity.startActivity(this)
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
        // new
        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS){
                tts.language = Locale.US
                tts.setSpeechRate(1.0f)
            }
        })

//        faceDetectionTimer.scheduleAtFixedRate(object : TimerTask() {
//            override fun run() {
//                tts.speak("Turn your phone right", TextToSpeech.QUEUE_ADD, null)
//            }
//        }, 0, 3000) // 1000 毫秒（1秒）更新一次

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
            try {
                processCameraProvider.bindToLifecycle(this, cameraSelector, imageCapture,
                    imageAnalysis, cameraPreview)
            } catch (illegalStateException: IllegalStateException) {
                Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
            } catch (illegalArgumentException: IllegalArgumentException) {
                Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
            }
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
        try {
            processCameraProvider.bindToLifecycle(this, cameraSelector, imageCapture,
                imageAnalysis, cameraPreview)
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
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