package in.codekamp.opencvtest;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


    private static final String TAG = "com_ynemov_blinkarbiter";
    private static final String RESULTS_FRAGMENT = "RESULTS_FRAGMENT";
    private static final String RES_FACE_CASCADE = "haarcascade_frontalface_alt.xml";
    private static final String RES_EYES_CASCADE = "haarcascade_eye_tree_eyeglasses.xml";
    private static final String CASCADE_INIT_ERROR = "Cascede classifiers weren't initiated!";

    private static final float FACE_SIZE_PERCENTAGE = 0.3f;

    private static final int DETECTION_NUMBER_OF_STEPS = 16; // 16 intervals
    private static final int DETECTION_STEP_DURATION = 1000; // 1s
    private static final int SHOW_DURATION = 1000; // 1s
    private static final String BLINK_MSG = "Blink is detected"; // Message to place if blink is detected
    private String[] mRawRes = {RES_FACE_CASCADE, RES_EYES_CASCADE};

    private CameraBridgeViewBase mOpenCvCameraView;
    //	private TextView mResults;
    private CascadeClassifier mFaceCascade;
    private CascadeClassifier mEyesCascade;
    private Mat mGrayscaleImage;
    private int mAbsoluteFaceSize;
    private Toast mToast;
    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }

        }
    };

    static {
        System.loadLibrary("MyLibs");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        mToast = new Toast(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1);
        mOpenCvCameraView.disableView();

        (new AsyncTask<String, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(String... params) {

                try {
                    // First URL
                    {
                        // Copy the resource into a temp file so OpenCV can load it
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, params[0]);
                        FileOutputStream os = new FileOutputStream(mCascadeFile);


                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // Load the cascade classifier
                        mFaceCascade = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    }

                    // Second URL
                    {
                        // Copy the resource into a temp file so OpenCV can load it
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, params[1]);
                        FileOutputStream os = new FileOutputStream(mCascadeFile);


                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // Load the cascade classifier
                        mEyesCascade = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean isSuccess) {
                if(isSuccess) {
                    mOpenCvCameraView.enableView();

                }
                else {

                }
                super.onPostExecute(isSuccess);
            }
        }).execute(mRawRes);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "opencv loaded successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "opencv not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        mGrayscaleImage = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {

        mGrayscaleImage.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat inputMat = inputFrame.rgba();
        Imgproc.cvtColor(inputMat, mGrayscaleImage, Imgproc.COLOR_RGBA2RGB);

        MatOfRect mFaces = new MatOfRect();
        Size mMinSize = new Size(mAbsoluteFaceSize, mAbsoluteFaceSize);
        Size mMaxSize = new Size();

        if (mFaceCascade != null) {
            mFaceCascade.detectMultiScale(mGrayscaleImage, mFaces, 1.1, 2, 2, mMinSize, mMaxSize);
        }

        Rect[] facesArray = mFaces.toArray();

		/*
		 *  In case of simplicity first detected face is used
		 *  Replace comment from FOR-loop in case of advances detection
		 */
        //		for (int i = 0; < facesArray.length; ++i)
        if (facesArray.length > 0) {
            int i = 0;
            Log.d(TAG,"face detected");
            for (Rect rect : mFaces.toArray()) {
                Imgproc.rectangle(
                        mGrayscaleImage,                                               // where to draw the box
                        new Point(rect.x, rect.y),                            // bottom left
                        new Point(rect.x + rect.width, rect.y + rect.height), // top right
                        new Scalar(0, 0, 255),
                        3                                                     // RGB colour
                );
            }

			/*
			 *  Face rectangle are used for debug purposes
			 *  Replace comments if has to debug
			 */
//            	Core.rectangle(inputMat, facesArray[i].tl(), facesArray[i].br(), mClr1, 3);

            Mat faceROI = mGrayscaleImage.submat(facesArray[i]);
            MatOfRect mEyes = new MatOfRect();

            //-- In each face, detect eyes
            mEyesCascade.detectMultiScale(faceROI, mEyes, 1.1, 2, 2, mMinSize, mMaxSize);
            Rect[] eyesArray = mEyes.toArray();
            Log.d(TAG,"eye detected");


        }
        return inputMat;
    }

}