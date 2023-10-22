package hackathon
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import hackathon.facedetector.desiredAngle
import hackathon.facedetector.faceAngle
import hackathon.facedetector.lensFacing
import kotlin.math.absoluteValue
fun DetermineDirection(FacePosX: Float,FacePosY: Float, rectPosX: Float, rectPosY: Float, tts: TextToSpeech, err:Float, NoFace:Boolean): Boolean{
    if(NoFace){
        tts.speak("There are no faces", TextToSpeech.QUEUE_ADD, null)
    }
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
                // location is correct
                if (desiredAngle == "Full"){
                    if (-20 <= faceAngle && faceAngle <= 20){
                        // angle is correct
                        tts.speak("Successful, picture taken", TextToSpeech.QUEUE_ADD, null)
                        return true
                    }
                    else if (faceAngle < -20){
                        // angle is left
                        tts.speak("Turn your head left", TextToSpeech.QUEUE_ADD, null)
                    }
                    else{
                        // angle is right
                        tts.speak("Turn your head right", TextToSpeech.QUEUE_ADD, null)
                    }
                }
                else if (desiredAngle == "Right"){
                    if (-20 <= faceAngle && faceAngle <= 20){
                        tts.speak("Turn your head left", TextToSpeech.QUEUE_ADD, null)
                    }
                    else if (faceAngle < -20){
                        // angle is left
                        tts.speak("Turn your head left", TextToSpeech.QUEUE_ADD, null)
                    }
                    else{
                        // angle is right
                        tts.speak("Successful, picture taken", TextToSpeech.QUEUE_ADD, null)
                        return true
                    }
                }
                else{
                    if (-20 <= faceAngle && faceAngle <= 20){
                        tts.speak("Turn your head right", TextToSpeech.QUEUE_ADD, null)
                    }
                    else if (faceAngle < -20){
                        // angle is left
                        tts.speak("Successful, picture taken", TextToSpeech.QUEUE_ADD, null)
                        return true
                    }
                    else{
                        // angle is right
                        tts.speak("Turn your head right", TextToSpeech.QUEUE_ADD, null)
                    }
                }
            }
        }
    }
    return false
}