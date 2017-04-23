package com.oobest.study.zxingdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;


    private CameraManager mCameraManger;

    private TextureView mTextureView;

    private Handler mHandler;

    private HandlerThread mBackgroundThread;

    private String mCameraId;

    private ImageReader mImageReader;

    private CameraDevice mCameraDevice;

    private CaptureRequest.Builder mPreviewBuilder;

    private CameraCaptureSession mSession;


    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private Semaphore mCapturePicture = new Semaphore(1);

    private MultiFormatReader mMultiFormatReader;

    private ViewfinderView mViewfinderView;

    private Rect mFramingRect;

    private Size mPreviewSize;

    private Rect mFramingRectInPreview;

    private Point mScreenResolution;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraManger = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        mTextureView = (TextureView) findViewById(R.id.textureView);

        mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    }


    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
            try {
                Log.d(TAG, "onSurfaceTextureAvailable: width=" + width + ",height=" + height);
                CameraCharacteristics characteristics = mCameraManger.getCameraCharacteristics(mCameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                // Size size = map.getOutputSizes(SurfaceTexture.class)[0];
                mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
                Rect framingRect = getFramingRect();
                Rect framingRectInPreview = getFramingRectInPreview();
                mViewfinderView.setFrame(framingRect, framingRectInPreview);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }


    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void openCamera(int width, int height) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT;
            mCameraManger.openCamera(mCameraId, mDeviceStateCallback, mHandler);
        } catch (Exception e) {
            Log.e(TAG, "initCameraAndPreview: ", e);
        }

    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mSession) {
                mSession.close();
                mSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } else {
                finish();
            }
        }
    }


    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            try {
                cameraCameraCaptureSession();
            } catch (CameraAccessException e) {
                Log.d(TAG, "onOpened: ", e);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
            finish();
        }
    };

    private void cameraCameraCaptureSession() throws CameraAccessException {
        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        Surface surface = new Surface(texture);
        mPreviewBuilder.addTarget(surface);
        mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), mSessionPreviewStateCallback, mHandler);
    }


    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mSession = session;
            try {
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                session.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "onConfigured: ", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            mSession = session;
            process(result);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession
                                                session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            mSession = session;
            process(partialResult);
        }

        private void process(CaptureResult partialResult) {
            Integer afState = partialResult.get(CaptureResult.CONTROL_AF_STATE);
            Integer aeState = partialResult.get(CaptureResult.CONTROL_AE_STATE);
            if (afState == null) {
                capturePicture();
            } else if (afState == CaptureRequest.CONTROL_AF_STATE_PASSIVE_FOCUSED) {
                capturePicture();
            } else if (afState == CaptureRequest.CONTROL_AF_STATE_INACTIVE && aeState == CaptureRequest.CONTROL_AE_STATE_CONVERGED) {
                capturePicture();
            }
        }

    };

    private void capturePicture() {
        if (mCapturePicture.tryAcquire()) {
            Log.d(TAG, "onCaptureCompleted: mCapturePicture.tryAcquire()=true");
            try {
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
                mPreviewBuilder.addTarget(mImageReader.getSurface());
                CameraCaptureSession.CaptureCallback captureCallback
                        = new CameraCaptureSession.CaptureCallback() {

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {
                        unlockFocus();
                    }
                };
                mSession.stopRepeating();
                mSession.capture(mPreviewBuilder.build(), captureCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private void unlockFocus() {
        if (mSession == null) {
            return;
        }
        try {
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewBuilder.removeTarget(mImageReader.getSurface());
            mSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler);
            Log.d(TAG, "unlockFocus: ");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            int len = buffer.remaining();
            Log.d(TAG, "onImageAvailable: len=" + len);
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            byte[] data = new byte[len];
            buffer.get(data);
            image.close();
            // Rect rect = getFramingRectInPreview();
            if (mMultiFormatReader == null) {
                mMultiFormatReader = new MultiFormatReader();
            }
            Rect rect = getFramingRectInPreview();
            Log.d(TAG, "onImageAvailable: imageWidth=" + imageWidth + ",imageHeight=" + imageHeight);

            Log.d(TAG, "onImageAvailable: FramingRectInPreview=" + rect.toString());


            PlanarYUVLuminanceSource planarYUVLuminanceSource = new PlanarYUVLuminanceSource(data, imageWidth, imageHeight, rect.left, rect.top, rect.width(), rect.height(), false);
            //PlanarYUVLuminanceSource planarYUVLuminanceSource = new PlanarYUVLuminanceSource(data, imageWidth, imageHeight, 0, 0, imageWidth, imageHeight, false);
            if (planarYUVLuminanceSource != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(planarYUVLuminanceSource));
                try {
                    Result result = mMultiFormatReader.decodeWithState(bitmap);
                    Log.d(TAG, "onImageAvailable: result=" + result.getText());
                } catch (ReaderException re) {
                    Log.e(TAG, "onImageAvailable: ", re);
                } finally {
                    mMultiFormatReader.reset();
                }
            }
            mCapturePicture.release();
        }
    };


    private Rect getFramingRect() {
        if (mFramingRect == null) {
            Point screenResolution = getScreenResolution();
            Log.d(TAG, "getFramingRect: screenResolution=" + screenResolution.toString());
            int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
            int height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);
            Log.d(TAG, "getFramingRect: width=" + width + ", height=" + height);
            int[] position = new int[2];
            mTextureView.getLocationOnScreen(position);
            int leftOffset = (screenResolution.x - width - position[0]) / 2;
            int topOffset = (screenResolution.y - height - position[1]) / 2;
            mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
        }
        return mFramingRect;
    }

    
    private Rect getFramingRectInPreview() {
        if (mFramingRectInPreview == null) {
            Rect framingRect = getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            Log.d(TAG, "getFramingRectInPreview: rect=" + rect.toString());
            Point screenResolution = getScreenResolution();
            Log.d(TAG, "getFramingRectInPreview: screenResolution=" + screenResolution.toString());
            Point cameraResolution = new Point(mPreviewSize.getWidth(), mPreviewSize.getHeight());//CameraConfigurationUtils.findBestPreviewSizeValue(,screenResolution);
            //Log.d(TAG, "getFramingRectInPreview: mPreviewSize=" + mPreviewSize.toString());
            Log.d(TAG, "getFramingRectInPreview: cameraResoltion=" + cameraResolution.toString());
            if (cameraResolution == null || screenResolution == null) {
                return null;
            }

            rect.left = rect.left * cameraResolution.x / screenResolution.x;
            rect.right = rect.right * cameraResolution.x / screenResolution.x;
            rect.top = rect.top * cameraResolution.y / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
            mFramingRectInPreview = rect;
        }
        return mFramingRectInPreview;
    }

    /**
     * 获取屏幕分辨率
     *
     * @return
     */
    private Point getScreenResolution() {
        if (mScreenResolution == null) {
            WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point theScreenResolution = new Point();
            display.getSize(theScreenResolution);
            mScreenResolution = theScreenResolution;
        }
        return mScreenResolution;
    }

    /**
     * 获取满意尺寸
     *
     * @param resolution
     * @param hardMin
     * @param hardMax
     * @return
     */
    private int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8;
        return Math.min(hardMax, Math.max(hardMin, dim));
    }


}
