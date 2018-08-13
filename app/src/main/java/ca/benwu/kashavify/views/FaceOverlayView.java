package ca.benwu.kashavify.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.otaliastudios.cameraview.Facing;

public class FaceOverlayView extends View {

    private float mWidthScaleFactor = 0;
    private float mHeightScaleFactor = 0;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;

    private FirebaseVisionFace mFace;

    private Facing mFacing = Facing.FRONT;

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mWidthScaleFactor = (float) canvas.getWidth() / mPreviewWidth;
        mHeightScaleFactor = (float) canvas.getHeight() / mPreviewHeight;

        drawGlasses(canvas, mFace);
    }

    public void init(int previewWidth, int previewHeight, Facing facing) {
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        mFacing = facing;
    }

    public void setFace(FirebaseVisionFace face) {
        mFace = face;
    }

    public void setFacing(Facing facing) {
        mFacing = facing;
    }

    private float getXFromLandmark(FirebaseVisionFaceLandmark landmark, float offset) {
        return translateX(landmark.getPosition().getX()) + offset;
    }

    private float getYFromLandmark(FirebaseVisionFaceLandmark landmark, float offset) {
        return translateY(landmark.getPosition().getY()) + offset;
    }

    private float getXFromLandmark(FirebaseVisionFaceLandmark landmark) {
        return getXFromLandmark(landmark, 0);
    }

    private float getYFromLandmark(FirebaseVisionFaceLandmark landmark) {
        return getYFromLandmark(landmark, 0);
    }

    private float translateX(float x) {
        return mFacing == Facing.FRONT
                ? getWidth() - scaleX(x)
                : scaleX(x);
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

    private void drawGlasses(Canvas canvas, FirebaseVisionFace face) {
        if (face == null) {
            return;
        }

        int[] landmarkTypes = new int[] {
                FirebaseVisionFaceLandmark.BOTTOM_MOUTH,
                FirebaseVisionFaceLandmark.LEFT_CHEEK,
                FirebaseVisionFaceLandmark.LEFT_EAR,
                FirebaseVisionFaceLandmark.LEFT_EYE,
                FirebaseVisionFaceLandmark.LEFT_MOUTH,
                FirebaseVisionFaceLandmark.NOSE_BASE,
                FirebaseVisionFaceLandmark.RIGHT_CHEEK,
                FirebaseVisionFaceLandmark.RIGHT_EAR,
                FirebaseVisionFaceLandmark.RIGHT_EYE,
                FirebaseVisionFaceLandmark.RIGHT_MOUTH
        };

        int[] colors = new int[] {
                0xff000000,
                0xff0000ff,
                0xff00ff00,
                0xffff0000,
                0xff00ffff,
                0xffff00ff,
                0xff0000ff,
                0xff00ff00,
                0xffff0000,
                0xff00ffff,
        };

        for (int type = 0 ; type < landmarkTypes.length ; type++) {
            Paint paint = new Paint();
            paint.setColor(colors[type]);
            FirebaseVisionFaceLandmark landmark = face.getLandmark(landmarkTypes[type]);
            if (landmark != null) {
                float size = 50;
                canvas.drawCircle(getXFromLandmark(landmark), getYFromLandmark(landmark), size, paint);
            }
        }
    }
}
