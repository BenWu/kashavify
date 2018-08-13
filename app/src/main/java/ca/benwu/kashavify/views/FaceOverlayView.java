package ca.benwu.kashavify.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.otaliastudios.cameraview.Facing;

import ca.benwu.kashavify.R;

public class FaceOverlayView extends View {

    private float mWidthScaleFactor = 0;
    private float mHeightScaleFactor = 0;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;

    private FirebaseVisionFace mFace;

    private Facing mFacing = Facing.FRONT;

    private Bitmap mHairBitmap;

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHairBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hair);
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

    private Rect translateBoundingBox(Rect rect) {
        return translateBoundingBox(rect, 0);
    }

    private Rect translateBoundingBox(Rect rect, int padding) {
        int horzModifier = mFacing == Facing.FRONT ? 1 : -1;

        Rect translated = new Rect();
        translated.top = (int) translateY(rect.top) - padding;
        translated.bottom = (int) translateY(rect.bottom) + padding;
        translated.left = (int) translateX(rect.left) + padding * horzModifier;
        translated.right = (int) translateX(rect.right) - padding * horzModifier;
        return translated;
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

        float headTilt = face.getHeadEulerAngleZ();

        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(-headTilt);

        Bitmap rotatedHair = Bitmap.createBitmap(mHairBitmap, 0, 0,
                mHairBitmap.getWidth(), mHairBitmap.getHeight(), rotationMatrix, true);

        canvas.drawBitmap(rotatedHair, null,
                translateBoundingBox(face.getBoundingBox(), 60 + (int) Math.abs(headTilt) * 8), null);

        Paint outlinePaint = new Paint();
        outlinePaint.setColor(0xffff0000);
        outlinePaint.setStrokeWidth(5f);
        outlinePaint.setStyle(Paint.Style.STROKE);

        canvas.drawRect(translateBoundingBox(face.getBoundingBox()), outlinePaint);
    }
}
