package ca.benwu.kashavify;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

public class FaceOverlayView extends View {

    private float mWidthScaleFactor = 0;
    private float mHeightScaleFactor = 0;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;

    private FirebaseVisionFace mFace;

    FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mWidthScaleFactor = (float) canvas.getWidth() / mPreviewWidth;
        mHeightScaleFactor = (float) canvas.getHeight() / mPreviewHeight;

        drawGlasses(canvas, mFace);
    }

    public void init(int previewWidth, int previewHeight) {
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
    }

    private float translateX(float x) {
        return getWidth() - scaleX(x);
    }

    private float translateY(float y) {
        return scaleY(y);
    }

    private float scaleX(float x) {
        return x * mWidthScaleFactor;
    }

    private float scaleY(float y) {
        return y * mHeightScaleFactor;
    }

    public void setFace(FirebaseVisionFace face) {
        mFace = face;
    }

    private void drawGlasses(Canvas canvas, FirebaseVisionFace face) {
        if (face == null) {
            return;
        }

        FirebaseVisionFaceLandmark leftEye =
                face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
        FirebaseVisionFaceLandmark rightEye =
                face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);

        if (leftEye != null && rightEye != null) {
            float eyeDistance = leftEye.getPosition().getX() - rightEye.getPosition().getY();
            float delta = mWidthScaleFactor * eyeDistance / 2;
            RectF glassesRect = new RectF(
                    translateX(leftEye.getPosition().getX()) - delta,
                    translateY(leftEye.getPosition().getY()) - delta,
                    translateX(rightEye.getPosition().getX()) + delta,
                    translateY(rightEye.getPosition().getY()) + delta
            );
            Paint paint = new Paint();
            paint.setColor(0xff000000);
            canvas.drawOval(glassesRect, paint);
        }
    }
}
