package com.phoenix.silentcamera.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.phoenix.silentcamera.util.CameraSave;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CaptureService extends Service {
    private static final String TAG = "CaptureService";

    public static final String ACTION_SINGLE_PIC = "com.phoenix.devicemonitor.SINGLE_PIC";
    public static final String ACTION_MULTI_PIC = "com.phoenix.devicemonitor.MULTI_PIC";
    public static final String BLOCK_SEND_DELAY = "com.phoenix.devicemonitor.BLOCK_SEND_DELAY";
    public static final String RESEND_CONNECTED = "com.phoenix.devicemonitor.RESEND_CONNECTED";

    private Camera mCamera;
    private int mCameraId;
    private SurfaceTexture mTexture;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if(ACTION_SINGLE_PIC.equals(action)){
            mCamera = getCamerInstance();
            initTexture();
            takePicture();
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if(mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        super.onDestroy();
    }

    private void takePicture() {
        try {
            mCamera.takePicture(null, null, mPictureCallback);
        } catch (Exception e) {
            Log.d(TAG, "call takePicture failed, e:" + e.getMessage());
            mCamera.release();
            mCamera = null;
        }
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File outputFile = CameraSave.getOutputMediaFile(CameraSave.DIRECTORY_PUBLIC);

            if(outputFile == null) {
                Log.e(TAG, "generate output file failed");
                return;
            }

            try{
                FileOutputStream fops = new FileOutputStream(outputFile);
                fops.write(data);
                fops.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "generate picture failed : " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "write picture failed : " + e.getMessage());
            }
            mCamera.release();
            mCamera = null;
        }
    };

    private Camera getCamerInstance() {
        android.hardware.Camera c = null;

        if(mCamera != null) {
            return mCamera;
        }

        int cameraNum = android.hardware.Camera.getNumberOfCameras();
        Log.d(TAG, "camera number: " + cameraNum);
        try {
            if(cameraNum > 1) {
                //more than one camera
                c = android.hardware.Camera.open(android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
                mCameraId = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
            } else {
                c = android.hardware.Camera.open();
                mCameraId = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
            }
        } catch (Exception e) {
            Log.e(TAG, "initiate Camera failed");
            e.printStackTrace();
        }


        return c;
    }

    private void initTexture() {
        mTexture = new SurfaceTexture(0);

        try {
            mCamera.setPreviewTexture(mTexture);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "initiate camera failed, e: " + e.getMessage());
        }
    }
}
