package ca.benwu.kashavify

import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.util.Log
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.otaliastudios.cameraview.CameraView

class MainActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName

    private val CAMERA_PERMISSION_REQ_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tryStartCamera()
    }

    private fun tryStartCamera() {
        if (this.checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQ_CODE)
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQ_CODE) {
            if (Manifest.permission.CAMERA == permissions[0] &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startCamera() {
        val cameraView = findViewById<CameraView>(R.id.camera_view)
        val faceOverlay = findViewById<FaceOverlayView>(R.id.face_overlay)
        lifecycle.addObserver(MainActivityLifecycleObserver(cameraView, faceOverlay))
    }

    // TODO: MODULARIZE
    class MainActivityLifecycleObserver(
            private val cameraView: CameraView,
            private val faceOverlay: FaceOverlayView) : LifecycleObserver {
        val TAG = this.javaClass.simpleName

        private val detectorOptions = FirebaseVisionFaceDetectorOptions.Builder()
                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .build()
        private val detector = FirebaseVision.getInstance().getVisionFaceDetector(detectorOptions)

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun startCamera() {
            cameraView.start()
            cameraView.addFrameProcessor { frame ->
                val metadata = FirebaseVisionImageMetadata.Builder()
                        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                        .setWidth(frame.size.width)
                        .setHeight(frame.size.height)
                        .setRotation(frame.rotation / 90)
                        .build()

                val firebaseVisionImage = FirebaseVisionImage.fromByteArray(frame.data, metadata)

                faceOverlay.init(cameraView.width, cameraView.height)

                detector.detectInImage(firebaseVisionImage)
                        .addOnSuccessListener { faceList ->
                            if (faceList.size > 0) {
                                val face = faceList[0]
                                faceOverlay.setFace(face)
                                faceOverlay.invalidate()
                            }
                        }
                        .addOnFailureListener {exception ->

                        }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun pauseCamera() {
            cameraView.stop()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun destroyCamera() {
            detector.close()
            cameraView.destroy()
        }
    }
}
