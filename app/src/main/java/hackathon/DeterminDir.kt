package hackathon
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import hackathon.facedetector.lensFacing
import kotlin.math.absoluteValue
fun DeterminDir(FacePosX: Float,FacePosY: Float, rectPosX: Float, rectPosY: Float, tts: TextToSpeech, err:Float, NoFace:Boolean){
    if(NoFace) tts.speak("There are no faces", TextToSpeech.QUEUE_ADD, null)
    else {
        if ((FacePosX - rectPosX).absoluteValue > err) {
            if ((FacePosX > rectPosX && lensFacing == CameraSelector.LENS_FACING_FRONT) || (FacePosX < rectPosX && lensFacing == CameraSelector.LENS_FACING_BACK)) {
                //                        Log.d("move", "turn right")
                tts.speak("Rotate your phone right", TextToSpeech.QUEUE_ADD, null)
            } else {
                //                        Log.d("move", "turn left")
                tts.speak("Rotate your phone left", TextToSpeech.QUEUE_ADD, null)
            }
        } else {
            if ((FacePosY - rectPosY).absoluteValue > err) {
                if (FacePosY > rectPosY) {
                    //                            Log.d("move", "turn down")
                    tts.speak("Rotate your phone down", TextToSpeech.QUEUE_ADD, null)
                } else {
                    //                            Log.d("move", "turn up")
                    tts.speak("Rotate your phone up", TextToSpeech.QUEUE_ADD, null)
                }
            } else {
                //                        Log.d("move", "correct!!!")
//                when(DisiredFaceAngle){
//                    Left -> {
//                        if(faceAngle)
//                    }
//                        Middle->
//                    Right->
//                }
                tts.speak("Success", TextToSpeech.QUEUE_ADD, null)
            }
        }
    }
}