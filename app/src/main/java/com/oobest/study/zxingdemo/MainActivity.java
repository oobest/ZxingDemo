package com.oobest.study.zxingdemo;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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

    private AutoFitTextureView mTextureView;

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

    private int mSensorOrientation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = (AutoFitTextureView) findViewById(R.id.textureView);

        mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    }


    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
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
        Display display = getWindowManager().getDefaultDisplay();
        KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (display.getState() != Display.STATE_OFF && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
            startBackgroundThread();

            if (mTextureView.isAvailable()) {
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } else {
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        }
    }


    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    /**
     * Opens the camera specified by {@link #mCameraId}.
     */
    private void openCamera(int width, int height) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        initFramingRect(width, height);
        initFramingRectInPreview(width, height);
        mViewfinderView.setFrame(mFramingRect, mFramingRectInPreview);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mDeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }


    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
                        new CompareSizesByArea());


                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                //Log.d(TAG, "setUpCameraOutputs: mPreviewSize=" + mPreviewSize.toString());
                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                        ImageFormat.YUV_420_888, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mHandler);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // Check if the flash is supported.
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {

        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread == null) {
            return;
        }
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
            Log.d(TAG, "closeCamera: mCameraOpenCloseLock.release()");
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
        if (mSession != null && mCapturePicture.tryAcquire()) {
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
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            image.close();

            Rect rect = new Rect(mFramingRectInPreview);
            Result result = decode(imageWidth, imageHeight, data, rect);

            mCapturePicture.release();
        }
    };

    private Result decode(int imageWidth, int imageHeight, byte[] data, Rect rect) {
        Result result = null;
        MultiFormatReader multiFormatReader = getMultiFormatReader();

        PlanarYUVLuminanceSource planarYUVLuminanceSource = new PlanarYUVLuminanceSource(data, imageWidth, imageHeight, rect.left, rect.top, rect.width(), rect.height(), false);
        if (planarYUVLuminanceSource != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(planarYUVLuminanceSource));
            try {
                result = multiFormatReader.decodeWithState(bitmap);
                Log.d(TAG, "decode: result=" + result.getText());
            } catch (ReaderException re) {
                Log.e(TAG, ": ", re);
            } finally {
                multiFormatReader.reset();
            }
        }
        return result;
    }


    private MultiFormatReader getMultiFormatReader() {
        if (mMultiFormatReader == null) {
            mMultiFormatReader = new MultiFormatReader();
            Collection<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
            decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);

            final Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
            hints.put(DecodeHintType.CHARACTER_SET, "UTF8");
            hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, new ViewfinderResultPointCallback(mViewfinderView));
            mMultiFormatReader.setHints(hints);
        }
        return mMultiFormatReader;
    }


    private void initFramingRect(int viewWidth, int viewHeight) {
        int width = findDesiredDimensionInRange(viewWidth, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
        int height = findDesiredDimensionInRange(viewHeight, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);
        int leftOffset = (viewWidth - width) / 2;
        int topOffset = (viewHeight - height) / 2;
        mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    }


    private void initFramingRectInPreview(int viewWidth, int viewHeight) {
        Rect rect = new Rect(mFramingRect);
        rect.left = rect.left * mPreviewSize.getWidth() / viewWidth;
        rect.right = rect.right * mPreviewSize.getWidth() / viewWidth;
        rect.top = rect.top * mPreviewSize.getHeight() / viewHeight;
        rect.bottom = rect.bottom * mPreviewSize.getHeight() / viewHeight;
        mFramingRectInPreview = rect;
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

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }


}
