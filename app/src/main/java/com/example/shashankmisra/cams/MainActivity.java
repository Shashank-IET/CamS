package com.example.shashankmisra.cams;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "msg";
    private final int REQ_CODE = 101;
    private final String ROTATIONAL_STATE = "rState";
    private final String ORIENTATION = "or";
    private final String CAMERA_ID = "cid";

    private RelativeLayout mLayout;
    private CameraPreview mPreview;
    private RelativeLayout.LayoutParams mPreviewLayoutParams;

    // camera settings default
    private boolean mRotationLocked = false;
    private int mSavedOrientation = Configuration.ORIENTATION_PORTRAIT;
    private int mCurrentCameraId = 1;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        {

        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            MainActivity.this.useThis(data);
            File pictureFile = SaveFileUtility.getOutputMediaFile(SaveFileUtility.MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
//        View decorView = getWindow().getDecorView();
//        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//        decorView.setSystemUiVisibility(uiOptions);
        if (savedInstanceState != null) {
            mRotationLocked = savedInstanceState.getBoolean(ROTATIONAL_STATE);
            mSavedOrientation = savedInstanceState.getInt(ORIENTATION);
            mCurrentCameraId = savedInstanceState.getInt(CAMERA_ID);
        }

        if (mRotationLocked) {
            setDeservedOrientation();
            ((ImageView) findViewById(R.id.rotational_state)).setImageResource(R.drawable.ic_screen_lock_rotation_black_24dp);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            ((ImageView) findViewById(R.id.rotational_state)).setImageResource(R.drawable.ic_screen_rotation_black_24dp);
        }
        if (mCurrentCameraId == 0)
            ((ImageView) findViewById(R.id.switch_camera)).setImageResource(R.drawable.ic_camera_front_black_24dp);
        else if (mCurrentCameraId == 1)
            ((ImageView) findViewById(R.id.switch_camera)).setImageResource(R.drawable.ic_camera_rear_black_24dp);
        mLayout = (RelativeLayout) findViewById(R.id.cam_preview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPreviewLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        int permissionCheck1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck1 == PackageManager.PERMISSION_GRANTED &&
                permissionCheck2 == PackageManager.PERMISSION_GRANTED) {
            mPreview = new CameraPreview(this, mCurrentCameraId, CameraPreview.LayoutMode.FitToParent);
            mLayout.addView(mPreview, 0, mPreviewLayoutParams);
        } else {
            if (Build.VERSION.SDK_INT >= 23)
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_CODE: {
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mPreview = new CameraPreview(this, mCurrentCameraId, CameraPreview.LayoutMode.FitToParent);
                    mLayout.addView(mPreview, 0, mPreviewLayoutParams);
                } else {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) mPreview.stop();
        mLayout.removeView(mPreview);
        mPreview = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ROTATIONAL_STATE, mRotationLocked);
        outState.putInt(ORIENTATION, mSavedOrientation);
        outState.putInt(CAMERA_ID, mCurrentCameraId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mSavedOrientation = newConfig.orientation;
    }

    private int getAlternateCameraId() {
        mCurrentCameraId = mCurrentCameraId == 0 ? 1 : 0;
        return mCurrentCameraId;
    }

    public void switchCamera(View view) {
        if (mPreview != null) mPreview.stop();
        mLayout.removeView(mPreview);
        mPreview = new CameraPreview(this, getAlternateCameraId(), CameraPreview.LayoutMode.FitToParent);
        mLayout.addView(mPreview, 0, mPreviewLayoutParams);
        if (mCurrentCameraId == 0)
            ((ImageView) view).setImageResource(R.drawable.ic_camera_front_black_24dp);
        else if (mCurrentCameraId == 1)
            ((ImageView) view).setImageResource(R.drawable.ic_camera_rear_black_24dp);
    }

    public void clickPicture(View view) {

        if (mPreview != null && mPreview.getmCamera() != null) {
            Camera camera = mPreview.getmCamera();
            camera.takePicture(null, null, mPicture);
            camera.startPreview();
        }
    }

    public void lockOrientation(View view) {
        mSavedOrientation = getResources().getConfiguration().orientation;
        mRotationLocked = !mRotationLocked;
        if (mRotationLocked) {
            setDeservedOrientation();
            ((ImageView) view).setImageResource(R.drawable.ic_screen_lock_rotation_black_24dp);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            ((ImageView) view).setImageResource(R.drawable.ic_screen_rotation_black_24dp);
        }
    }

    private void setDeservedOrientation() {
        switch (mSavedOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }

    private void useThis(byte[] data) {
        final Dialog imagePreview = new Dialog(this);
        imagePreview.requestWindowFeature(Window.FEATURE_NO_TITLE);
        imagePreview.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        imagePreview.setContentView(R.layout.dialog_image_view);
        ImageView shotImage = imagePreview.findViewById(R.id.image);
        Bitmap original = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap rotated = original;
        if (mSavedOrientation == Configuration.ORIENTATION_PORTRAIT) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            rotated = Bitmap.createBitmap(original, 0, 0,
                    original.getWidth(), original.getHeight(),
                    matrix, true);
        }
        shotImage.setImageBitmap(rotated);
        imagePreview.show();
        // may as well use any other view for calling postDelayed!
        mPreview.postDelayed(new Runnable() {
            @Override
            public void run() {
                imagePreview.dismiss();
            }
        }, 2000);
    }
}
