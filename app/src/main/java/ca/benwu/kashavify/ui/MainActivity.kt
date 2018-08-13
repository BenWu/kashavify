package ca.benwu.kashavify.ui

import android.Manifest
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import ca.benwu.kashavify.R
import ca.benwu.kashavify.views.FaceOverlayView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.Facing

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQ_CODE = 123

    lateinit var mCameraView: CameraView
    lateinit var mFaceOverlay: FaceOverlayView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()

        tryStartCamera()
    }

    private fun setupViews() {
        val cameraContainer : View = findViewById(R.id.camera_container)
        val flipButton : ImageButton = findViewById(R.id.btn_flip_camera)
        var canFlip = true
        flipButton.setOnClickListener { _ ->
            if (this::mCameraView.isInitialized && canFlip) {
                canFlip = false

                flipButton.animate().scaleX(0f).scaleY(0f).setDuration(1200)
                        .setInterpolator(AccelerateInterpolator(2.5f)).withEndAction {
                    flipButton.setImageDrawable(if (mCameraView.facing == Facing.FRONT)
                        this.getDrawable(R.drawable.camera_front_24)
                    else this.getDrawable(R.drawable.camera_rear_24))
                    flipButton.animate().scaleX(1f).scaleY(1f).setDuration(800)
                            .setInterpolator(OvershootInterpolator()).start()
                }

                cameraContainer.animate().alpha(0f).setDuration(1000).withEndAction {
                    cameraContainer.animate().alpha(1f).setDuration(1000).withEndAction {
                        canFlip = true
                    }.start()
                }.start()

                mCameraView.toggleFacing()
                if (this::mFaceOverlay.isInitialized) {
                    mFaceOverlay.setFacing(mCameraView.facing)
                }
            }
        }
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
        mCameraView = findViewById(R.id.camera_view)
        mFaceOverlay = findViewById(R.id.face_overlay)
        lifecycle.addObserver(MainActivityLifecycleObserver(mCameraView, mFaceOverlay))
    }

    // TODO: MODULARIZE
    class MainActivityLifecycleObserver(
            private val cameraView: CameraView,
            private val faceOverlay: FaceOverlayView) : LifecycleObserver {

        private val detectorOptions = FirebaseVisionFaceDetectorOptions.Builder()
                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .build()
        private val detector = FirebaseVision.getInstance().getVisionFaceDetector(detectorOptions)

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun startCamera() {
            cameraView.start()
            cameraView.addFrameProcessor { frame ->
                if (frame.size == null) {
                    return@addFrameProcessor
                }

                val metadata = FirebaseVisionImageMetadata.Builder()
                        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                        .setWidth(frame.size.width)
                        .setHeight(frame.size.height)
                        .setRotation(frame.rotation / 90)
                        .build()

                val firebaseVisionImage = FirebaseVisionImage.fromByteArray(frame.data, metadata)

                faceOverlay.init(cameraView.width, cameraView.height, cameraView.facing)

                detector.detectInImage(firebaseVisionImage)
                        .addOnSuccessListener { faceList ->
                            if (faceList.size > 0) {
                                val face = faceList[0]
                                faceOverlay.setFace(face)
                            } else if (faceList.isEmpty()) {
                                faceOverlay.setFace(null)
                            }
                            faceOverlay.invalidate()
                        }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun pauseCamera() {
            cameraView.clearFrameProcessors()
            cameraView.stop()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun destroyCamera() {
            detector.close()
            cameraView.destroy()
        }
    }
}
