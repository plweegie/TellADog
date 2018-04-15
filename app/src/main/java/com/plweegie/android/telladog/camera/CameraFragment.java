/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Modifications (C) 2018 Jan K Szymanski
==============================================================================*/

package com.plweegie.android.telladog.camera;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
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
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.plweegie.android.telladog.ImageClassifier;
import com.plweegie.android.telladog.InferenceAdapter;
import com.plweegie.android.telladog.MainActivity;
import com.plweegie.android.telladog.MyApp;
import com.plweegie.android.telladog.R;
import com.plweegie.android.telladog.data.DogPrediction;
import com.plweegie.android.telladog.data.PredictionRepository;
import com.plweegie.android.telladog.ui.FragmentSwitchListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import kotlin.Pair;


public class CameraFragment extends Fragment implements FragmentCompat.OnRequestPermissionsResultCallback {

    private final String TAG = this.getClass().getSimpleName();

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final String HANDLE_THREAD_NAME = "CameraBackground";

    private static final int PERMISSIONS_REQUEST_CODE = 1;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    @Inject
    PredictionRepository mRepository;

    private final Object lock = new Object();
    private boolean runClassifier = false;
    private boolean checkedPermissions = false;
    private TextView textView;
    private ImageClassifier classifier;
    private RecyclerView recyclerView;
    private InferenceAdapter adapter;
    private FragmentSwitchListener mFragmentSwitchListener;

    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {

                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                    configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture texture) {}
            };

    private String cameraId;
    
    private AutoFitTextureView textureView;

    private CameraCaptureSession captureSession;

    private CameraDevice cameraDevice;

    private Size previewSize;

    private Pair<String, Float> mTopPrediction;

    private final CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {

                @Override
                public void onOpened(@NonNull CameraDevice currentCameraDevice) {
                    cameraOpenCloseLock.release();
                    cameraDevice = currentCameraDevice;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice currentCameraDevice) {
                    cameraOpenCloseLock.release();
                    currentCameraDevice.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice currentCameraDevice, int error) {
                    cameraOpenCloseLock.release();
                    currentCameraDevice.close();
                    cameraDevice = null;
                    Activity activity = getActivity();
                    if (null != activity) {
                        activity.finish();
                    }
                }
            };

    private HandlerThread backgroundThread;

    private Handler backgroundHandler;

    private ImageReader imageReader;
    private File outputFile;
    private String imageUrl;

    private final ImageReader.OnImageAvailableListener onImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    try {
                        outputFile = createImageFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (imageUrl != null && outputFile != null) {
                        DogPrediction predictionToSave = new DogPrediction(
                                mTopPrediction.getFirst(),
                                mTopPrediction.getSecond(),
                                imageUrl,
                                new Date().getTime()
                        );

                        mRepository.add(predictionToSave);
                        backgroundHandler.post(new ImageSaver(imageReader.acquireNextImage(), outputFile));
                    } else {
                        Toast.makeText(getActivity(), "Failed to save image", Toast.LENGTH_SHORT)
                            .show();
                    }
                }
            };

    private CaptureRequest.Builder previewRequestBuilder;

    private CaptureRequest previewRequest;

    private int state = STATE_PREVIEW;

    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    private CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureProgressed(
                        @NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request,
                        @NonNull CaptureResult partialResult) {
                    processCaptureResult(partialResult);
                }

                @Override
                public void onCaptureCompleted(
                        @NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request,
                        @NonNull TotalCaptureResult result) {
                    processCaptureResult(result);
                }
            };


    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(text);
                        }
                    });
        }
    }

    private void updateAdapterAsync(final List<Pair<String, Float>> predictions) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.setPredictions(predictions);
                }
            });
        }
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public void onAttach(Context context) {
        ((MyApp) getActivity().getApplication()).getMAppComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mFragmentSwitchListener = (MainActivity) getActivity();
    }

    /** Layout the preview and buttons. */
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        adapter = new InferenceAdapter(getActivity());
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    /** Connect the buttons to their event handler. */
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        textView = (TextView) view.findViewById(R.id.text);

        recyclerView = view.findViewById(R.id.inference_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recyclerView.getVisibility() != View.VISIBLE) {
                    recyclerView.setAdapter(adapter);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /** Load the model and labels. */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {
            classifier = new ImageClassifier(getActivity());
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.");
        }
        startBackgroundThread();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        classifier.close();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_camera, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.change_to_list:
                mFragmentSwitchListener.onDogListFragmentSelect();
                return true;
            case R.id.save_pic_data:
                takePicture();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // // For still image captures, we use the largest available size.
                Size largest =
                        Collections.max(
                                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                imageReader =
                        ImageReader.newInstance(
                                largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                // noinspection ConstantConditions
        /* Orientation of the camera sensor */
                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (sensorOrientation == 90 || sensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (sensorOrientation == 0 || sensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                }

                previewSize = new Size(rotatedPreviewWidth, rotatedPreviewHeight);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                } else {
                    textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }

                this.cameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    private String[] getRequiredPermissions() {
        Activity activity = getActivity();
        try {
            PackageInfo info =
                    activity
                            .getPackageManager()
                            .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    /** Opens the camera specified by {@link CameraFragment#cameraId}. */
    private void openCamera(int width, int height) {
        if (!checkedPermissions && !allPermissionsGranted()) {
            requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE);
            return;
        } else {
            checkedPermissions = true;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /** Closes the current {@link CameraDevice}. */
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    /** Starts a background thread and its {@link Handler}. */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        synchronized (lock) {
            runClassifier = true;
        }
        backgroundHandler.post(periodicClassify);
    }

    /** Stops the background thread and its {@link Handler}. */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
            synchronized (lock) {
                runClassifier = false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** Takes photos and classify them periodically. */
    private Runnable periodicClassify =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (runClassifier) {
                            classifyFrame();
                        }
                    }
                    backgroundHandler.post(periodicClassify);
                }
            };

    /** Creates a new {@link CameraCaptureSession} for camera preview. */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            textureView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (recyclerView.getVisibility() == View.VISIBLE) {
                        recyclerView.setAdapter(null);
                        recyclerView.setVisibility(View.GONE);
                    }
                }
            });

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice.createCaptureSession(
                    Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(
                                        previewRequest, captureCallback, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `textureView`. This
     * method should be called after the camera preview size is determined in setUpCameraOutputs and
     * also the size of `textureView` is fixed.
     *
     * @param viewWidth The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == textureView || null == previewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    private void takePicture() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            state = STATE_WAITING_LOCK;
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecapture() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            state = STATE_WAITING_PRECAPTURE;
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureFrame() {
        try {
            final Activity activity = getActivity();
            if (activity == null || cameraDevice == null) {
                return;
            }

            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            CameraCaptureSession.CaptureCallback captureCallback =
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                       @NonNull CaptureRequest request,
                                                       @NonNull TotalCaptureResult result) {
                            unlockFocus();
                        }
                    };
            captureSession.stopRepeating();
            captureSession.abortCaptures();
            captureSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void processCaptureResult(CaptureResult result) {
        switch (state) {
            case STATE_PREVIEW:
                break;

            case STATE_WAITING_LOCK:
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                if (afState == null) {
                    captureFrame();
                } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                        afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        state = STATE_PICTURE_TAKEN;
                        captureFrame();
                    } else {
                        runPrecapture();
                    }
                }
                break;

            case STATE_WAITING_PRECAPTURE:
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    state = STATE_WAITING_NON_PRECAPTURE;
                }
                break;

            case STATE_WAITING_NON_PRECAPTURE:
                Integer aeStateNonPrecapture = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeStateNonPrecapture == null ||
                        aeStateNonPrecapture != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    state = STATE_PICTURE_TAKEN;
                    captureFrame();
                }
                break;
        }
    }

    private void unlockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);

            state = STATE_PREVIEW;
            captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "PREDICTION_" + timestamp + "_";
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = new File(storageDir, fileName);
        imageUrl = image.getAbsolutePath();

        return image;
    }

    /** Classifies a frame from the preview stream. */
    private void classifyFrame() {
        if (classifier == null || getActivity() == null || cameraDevice == null) {
            showToast("Uninitialized Classifier or invalid context.");
            return;
        }
        Bitmap bitmap =
                textureView.getBitmap(ImageClassifier.DIM_IMG_SIZE_X, ImageClassifier.DIM_IMG_SIZE_Y);
        List<Pair<String, Float>> predictions = classifier.getPredictions(bitmap);
        bitmap.recycle();

        Pair<String, Float> topPrediction = predictions.get(0);
        mTopPrediction = topPrediction;

        if (predictions.get(0).getSecond() < 0.30) {
            showToast(getString(R.string.no_dogs_here));
        } else {
            showToast(topPrediction.getFirst());
        }

        updateAdapterAsync(predictions);
    }

    private static class ImageSaver implements Runnable {

        private final Image mImage;
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {

            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;

            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();

                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /** Compares two {@code Size}s based on their areas. */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /** Shows an error message dialog. */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    activity.finish();
                                }
                            })
                    .create();
        }
    }
}
