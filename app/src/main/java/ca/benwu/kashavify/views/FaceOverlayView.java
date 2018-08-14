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

import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
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
    private Bitmap mLeftBrow;
    private Bitmap mRightBrow;
    private Bitmap mGlasses;
    private Bitmap mMoustache;
    private Bitmap mBeard;

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        preloadBitmaps();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mWidthScaleFactor = (float) canvas.getWidth() / mPreviewWidth;
        mHeightScaleFactor = (float) canvas.getHeight() / mPreviewHeight;

        drawGlasses(canvas, mFace);
    }

    private void preloadBitmaps() {
        mHairBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hair);
        mLeftBrow = BitmapFactory.decodeResource(getResources(), R.drawable.left_brow);
        mRightBrow = BitmapFactory.decodeResource(getResources(), R.drawable.right_brow);
        mGlasses = BitmapFactory.decodeResource(getResources(), R.drawable.glasses);
        mMoustache = BitmapFactory.decodeResource(getResources(), R.drawable.moustache);
        mBeard = BitmapFactory.decodeResource(getResources(), R.drawable.beard);
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

    // TODO: Move some of these to utils

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

    // when using front facing camera, x-axis is mirrored
    private int xCorrection() {
        return mFacing == Facing.FRONT ? 1 : -1;
    }

    private Rect translateBoundingBox(Rect rect) {
        return translateBoundingBox(rect, 0);
    }

    private Rect translateBoundingBox(Rect rect, int padding) {
        Rect translated = new Rect();
        translated.top = (int) translateY(rect.top) - padding;
        translated.bottom = (int) translateY(rect.bottom) + padding;
        translated.left = (int) translateX(rect.left) + padding * xCorrection();
        translated.right = (int) translateX(rect.right) - padding * xCorrection();
        return translated;
    }

    // hardcode translate bounding box to get closer to actual face
    // TODO: Should probably figure out why bbox doesn't overlay face properly
    private void hardcodedOffset(Rect rect) {
        rect.top += 50;
        rect.bottom += 50;
        rect.left -= 50 * xCorrection();
        rect.right -= 50 * xCorrection();
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float angle) {
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(angle);

        return transformBitmap(bitmap, rotationMatrix);
    }

    private Bitmap transformBitmap(Bitmap bitmap, Matrix matrix) {
        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void drawGlasses(Canvas canvas, FirebaseVisionFace face) {
        if (face == null) {
            return;
        }

        Paint outlinePaint = new Paint();
        outlinePaint.setColor(0xffff0000);
        outlinePaint.setStrokeWidth(5f);
        outlinePaint.setStyle(Paint.Style.STROKE);

        hardcodedOffset(face.getBoundingBox());

        Rect translatedFaceBox = translateBoundingBox(face.getBoundingBox());

        float headTilt = face.getHeadEulerAngleZ();
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(-headTilt);

        int faceWidth = translatedFaceBox.right - translatedFaceBox.left;

        FirebaseVisionFaceLandmark leftCheek = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_CHEEK);
        FirebaseVisionFaceLandmark rightCheek = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_CHEEK);

        // Draw beard
        Bitmap rotatedBeard = transformBitmap(mBeard, rotationMatrix);

        if (leftCheek != null && rightCheek != null) {
            int margin = (int) (faceWidth * 0.1);

            float heightToWidth = (float) mBeard.getHeight() / mBeard.getWidth();
            int height = (int) Math.abs(heightToWidth * (faceWidth - margin * 2));

            int topY = translatedFaceBox.bottom - height / 2;

            Rect beardRect = new Rect(translatedFaceBox.left + margin, topY,
                    translatedFaceBox.right - margin, topY + height);

            canvas.drawBitmap(rotatedBeard, null, beardRect, null);
        }

        // Draw hair
        Bitmap rotatedHair = transformBitmap(mHairBitmap, rotationMatrix);
        Rect hairRect = translateBoundingBox(face.getBoundingBox(), 60 + (int) Math.abs(headTilt) * 8);
        canvas.drawBitmap(rotatedHair, null, hairRect, null);

        FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
        FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);

        // Draw glasses
        Bitmap rotatedGlasses = transformBitmap(mGlasses, rotationMatrix);

        if (leftEye != null && rightEye != null) {
            FirebaseVisionPoint leftPos = leftEye.getPosition();
            FirebaseVisionPoint rightPos = rightEye.getPosition();
            int topY = (int) Math.min(leftPos.getY(), rightPos.getY());
            int bottomY = (int) Math.max(leftPos.getY(), rightPos.getY());

            int margin = (int) (faceWidth * 0.1);

            float heightToWidth = (float) mGlasses.getHeight() / mGlasses.getWidth();
            int height = (int) Math.abs(heightToWidth * (faceWidth - margin * 2));

            Rect glassesRect = new Rect(translatedFaceBox.left + margin, topY,
                    translatedFaceBox.right - margin, bottomY + height);

            canvas.drawBitmap(rotatedGlasses, null, glassesRect, null);
        }

        //canvas.drawRect(translatedFaceBox, outlinePaint);
    }
}
