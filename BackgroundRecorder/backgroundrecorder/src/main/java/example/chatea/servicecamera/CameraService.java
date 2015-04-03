package example.chatea.servicecamera;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import example.chatea.servicecamera.measurement.Point;
import example.chatea.servicecamera.messages.MeasurementStepMessage;
import example.chatea.servicecamera.messages.MessageHUB;

public class CameraService extends Service implements Camera.PreviewCallback{

    /***************************************************************************************************/
    /**
     * Represents the standard height of a peace of a4 paper e.g. 29.7cm
     */
    public static final int CALIBRATION_DISTANCE_A4_MM = 294;

    public static final int CALIBRATION_MEASUREMENTS = 10;

    public static final int AVERAGE_THREASHHOLD = 5;

    /**
     * Measured distance at calibration point
     */
    private float _distanceAtCalibrationPoint = -1;

    private float _currentAvgEyeDistance = -1;

    // private int _facesFoundInMeasurement = -1;

    /**
     * in cm
     */
    private float _currentDistanceToFace = -1;

    private FaceDetector.Face _foundFace = null;

    private int _threashold = CALIBRATION_MEASUREMENTS;

    private FaceDetectionThread _currentFaceDetectionThread;

    private List<Point> _points;

    protected final Paint _middlePointColor = new Paint();
    protected final Paint _eyeColor = new Paint();

    private Camera.Size _previewSize;

    // private boolean _measurementStartet = false;
    private boolean _calibrated = false;
    private boolean _calibrating = false;
    private int _calibrationsLeft = -1;

    /**
     * Variables for the onDraw method, in order to prevent variable allocation
     * to slow down the sometimes heavily called onDraw method
     */
    private final PointF _middlePoint = new PointF();
    private final Rect _trackingRectangle = new Rect();

    private final static int RECTANGLE_SIZE = 20;
    private boolean _showEyes = false;
    private boolean _showTracking = true;

    private long _lastFrameStart = System.currentTimeMillis();
    private float _processTimeForLastFrame = -1;
    /***************************************************************************************************/


    private Camera mCamera;
//    private MediaRecorder mMediaRecorder;

    public CameraService() {

    }

    /**
     * Used to take picture.
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = Util.getOutputMediaFile(Util.MEDIA_TYPE_IMAGE);

            if (pictureFile == null) {
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("TAG", "======= service in onStartCommand1");
//        MessageHUB.get().sendMessage(MessageHUB.LOG_ONSERVICE, null);
        Log.d("TAG", "======= service in onStartCommand2");
        if (Util.checkCameraHardware(this)) {
            mCamera = Util.getCameraInstance();
            if (mCamera != null) {
                SurfaceView sv = new SurfaceView(this);

                // 设置悬浮窗体属性
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT);


                final SurfaceHolder sh = sv.getHolder();// 绑定SurfaceView，取得SurfaceHolder对象



                sv.setZOrderOnTop(true);
                sh.setFormat(PixelFormat.TRANSPARENT);


                sh.addCallback(new SurfaceHolder.Callback() {// SurfaceHolder加入回调接口
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        Camera.Parameters params = mCamera.getParameters();
                        mCamera.setParameters(params);
                        Camera.Parameters p = mCamera.getParameters();

                        List<Camera.Size> listSize;

                        listSize = p.getSupportedPreviewSizes();
                        Camera.Size mPreviewSize = listSize.get(2);
                        Log.v("TAG", "preview width = " + mPreviewSize.width
                                + " preview height = " + mPreviewSize.height);
                        p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

                        listSize = p.getSupportedPictureSizes();
                        Camera.Size mPictureSize = listSize.get(2);
                        Log.v("TAG", "capture width = " + mPictureSize.width
                                + " capture height = " + mPictureSize.height);
                        p.setPictureSize(mPictureSize.width, mPictureSize.height);
//                        MessageHUB.get().sendMessage(MessageHUB.LOG_PARA, null);

                        try {
                            mCamera.setPreviewDisplay(sh);
                            mCamera.setPreviewCallback(CameraService.this);
                            mCamera.setParameters(p);
                        } catch (IOException e) {
//                            MessageHUB.get().sendMessage(MessageHUB.setPreviewDisplay_error, null);
                            e.printStackTrace();
                            mCamera.release();
                            mCamera = null;
                        }

//                    mCamera.takePicture(null, null, mPicture); // used to takePicture.



//                        mMediaRecorder = new MediaRecorder();
//                        mMediaRecorder.setCamera(mCamera);
//
//                        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//                        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//
//                        mMediaRecorder.setProfile(CamcorderProfile.get(1, CamcorderProfile.QUALITY_LOW));
//
//                        mMediaRecorder.setOutputFile(Util.getOutputMediaFile(Util.MEDIA_TYPE_VIDEO).getPath());
//
//                        mMediaRecorder.setPreviewDisplay(holder.getSurface());
//
//                        try {
//                            mMediaRecorder.prepare();
//                        } catch (IllegalStateException e) {
//                            Log.d("TAG", "====== IllegalStateException preparing MediaRecorder: " + e.getMessage());
//                        } catch (IOException e) {
//                            Log.d("TAG", "====== IOException preparing MediaRecorder: " + e.getMessage());
//                        }
//                        mMediaRecorder.start();
//                        Log.d("TAG", "========= recording start");

//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                mMediaRecorder.stop();
//                                mMediaRecorder.release();
//                                mCamera.stopPreview();
//                                mCamera.release();
//                                Log.d("TAG", "========== recording finished.");
//                            }
//                        }, 10000);
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                        if (holder.getSurface() == null){
                            return;
                        }
                        try {
                            mCamera.stopPreview();
                        } catch (Exception e){
                            // ignore: tried to stop a non-existent preview
                        }
                        try {
                            Camera.Parameters params = mCamera.getParameters();
                            List<Camera.Size> listSize;
                            listSize = params.getSupportedPreviewSizes();
                            Camera.Size mPreviewSize = listSize.get(2);
                            params.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

                            mCamera.setParameters(params);
                            mCamera.startPreview();

                        } catch (Exception e){
                            Log.d("TAG", "Error starting camera preview: " + e.getMessage());
                        }
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                    }
                });


                wm.addView(sv, params);

            } else {
                Log.d("TAG", "==== get Camera from service failed");
//                MessageHUB.get().sendMessage(MessageHUB.LOG_CAMERA_FAILED, null);

            }
        } else {
            Log.d("TAG", "==== There is no camera hardware on device.");
        }

        if(!isCalibrated())
            calibrate();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /***************************************************************************************************/
    public void reset() {
        _distanceAtCalibrationPoint = -1;
        _currentAvgEyeDistance = -1;
        _calibrated = false;
        _calibrating = false;
        _calibrationsLeft = -1;
    }

    /**
     * Sets this current EYE distance to be the distance of a peace of a4 paper
     * e.g. 29,7cm
     */
    public void calibrate() {
        if (!_calibrating || !_calibrated) {
            _points = new ArrayList<Point>();
            _calibrating = true;
            _calibrationsLeft = CALIBRATION_MEASUREMENTS;
            _threashold = CALIBRATION_MEASUREMENTS;
        }
    }

    public boolean isCalibrated() {
        return _calibrated || _calibrating;
    }

    public void showMiddleEye(final boolean on) {
        _showTracking = on;
    }

    public void showEyePoints(final boolean on) {
        _showEyes = on;
    }

    private void updateMeasurement(final FaceDetector.Face currentFace) {
        if (currentFace == null) {
            // _facesFoundInMeasurement--;
            return;
        }

        _foundFace = _currentFaceDetectionThread.getCurrentFace();

        _points.add(new Point(_foundFace.eyesDistance(),
                CALIBRATION_DISTANCE_A4_MM
                        * (_distanceAtCalibrationPoint / _foundFace
                        .eyesDistance())));

        while (_points.size() > _threashold) {
            _points.remove(0);
        }

        float sum = 0;
        for (Point p : _points) {
            sum += p.getEyeDistance();
        }

        _currentAvgEyeDistance = sum / _points.size();

        _currentDistanceToFace = CALIBRATION_DISTANCE_A4_MM
                * (_distanceAtCalibrationPoint / _currentAvgEyeDistance);

        _currentDistanceToFace = Util.MM_TO_CM(_currentDistanceToFace);

        MeasurementStepMessage message = new MeasurementStepMessage();
        message.setConfidence(currentFace.confidence());
        message.setCurrentAvgEyeDistance(_currentAvgEyeDistance);
        message.setDistToFace(_currentDistanceToFace);
        message.setEyesDistance(currentFace.eyesDistance());
        message.setMeasurementsLeft(_calibrationsLeft);
        message.setProcessTimeForLastFrame(_processTimeForLastFrame);

        MessageHUB.get().sendMessage(MessageHUB.MEASUREMENT_STEP, message);
    }

    private void doneCalibrating() {
        _calibrated = true;
        _calibrating = false;
        _currentFaceDetectionThread = null;
        // _measurementStartet = false;

        _threashold = AVERAGE_THREASHHOLD;

        _distanceAtCalibrationPoint = _currentAvgEyeDistance;
        MessageHUB.get().sendMessage(MessageHUB.DONE_CALIBRATION, null);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d("onPreviewFrame", "onPreviewFrame");
        MessageHUB.get().sendMessage(MessageHUB.LOG_PREVIEWFRAME, null);

        //传递进来的data,默认是YUV420SP的
        if (_calibrationsLeft == -1)
            return;

        if (_calibrationsLeft > 0) {
            // Doing calibration !

            if (_currentFaceDetectionThread != null
                    && _currentFaceDetectionThread.isAlive()) {
                // Drop Frame
                return;
            }

            // No face detection started or already finished
            _processTimeForLastFrame = System.currentTimeMillis()
                    - _lastFrameStart;
            _lastFrameStart = System.currentTimeMillis();

            if (_currentFaceDetectionThread != null) {
                _calibrationsLeft--;
                updateMeasurement(_currentFaceDetectionThread.getCurrentFace());

                if (_calibrationsLeft == 0) {
                    doneCalibrating();
//                                            invalidate();
                    return;
                }
            }

            _currentFaceDetectionThread = new FaceDetectionThread(data,
                    _previewSize);
            _currentFaceDetectionThread.start();

//                                    invalidate();
        } else {
            // Simple Measurement

            if (_currentFaceDetectionThread != null
                    && _currentFaceDetectionThread.isAlive()) {
                // Drop Frame
                return;
            }

            // No face detection started or already finished
            _processTimeForLastFrame = System.currentTimeMillis()
                    - _lastFrameStart;
            _lastFrameStart = System.currentTimeMillis();

            if (_currentFaceDetectionThread != null)
                updateMeasurement(_currentFaceDetectionThread.getCurrentFace());

            _currentFaceDetectionThread = new FaceDetectionThread(data,
                    _previewSize);
            _currentFaceDetectionThread.start();
//                                    invalidate();
        }
    }


    /***************************************************************************************************/

}
