package hackathon.facedetector

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.google.mlkit.vision.face.Face

class FaceBox(
    overlay: FaceBoxOverlay,
    private val face: Face,
    private val imageRect: Rect
) : FaceBoxOverlay.FaceBox(overlay) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 6.0f
    }
    private val gridPaint = Paint().apply {
        color = Color.BLUE // 方格的顏色
        style = Paint.Style.STROKE
        strokeWidth = 5f // 方格線條的寬度
    }
    override fun draw(canvas: Canvas?) {
        val rect = getBoxRect(
            imageRectWidth = imageRect.width().toFloat(),
            imageRectHeight = imageRect.height().toFloat(),
            faceBoundingBox = face.boundingBox
        )
        canvas?.drawRect(rect, paint)
    }
    public fun drawGridOnPreview(canvas: Canvas) {
//        val surfaceTexture = binding.graphicOverlay.plot()
//        val canvas: Canvas = binding.graphicOverlay.lockCanvas()

        // 計算方格的位置和大小，這裡簡單地在預覽的中心畫一個方格
        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f
        val squareSize = 200f

        // 繪製方格
        canvas.drawRect(
            centerX - squareSize / 2,
            centerY - squareSize / 2,
            centerX + squareSize / 2,
            centerY + squareSize / 2,
            gridPaint
        )

//        binding.previewView.unlockCanvasAndPost(canvas)
    }
}