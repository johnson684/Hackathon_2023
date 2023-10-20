package hackathon.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hackathon.facedetector.FaceDetectionActivity
import meichu.hackathon.databinding.ActivitySettingBinding

class SettingActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backBtn.setOnClickListener{
            FaceDetectionActivity.startActivity(this)
        }

    }
    companion object {
        private val TAG = SettingActivity::class.simpleName
        fun startActivity(context: Context) {
            Intent(context, SettingActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}