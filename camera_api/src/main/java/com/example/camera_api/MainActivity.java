package com.example.camera_api;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;
//import com.dexafree.materialList.card.Card;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private ImageSurfaceView mImageSurfaceView;
    private Camera camera;

    private ProgressDialog mDialog;
    private FaceDet mFaceDet;
    private FrameLayout cameraPreviewLayout;
    private ImageView capturedImageHolder;
    private String picturePath;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        cameraPreviewLayout = (FrameLayout)findViewById(R.id.camera_preview);
        capturedImageHolder = (ImageView)findViewById(R.id.captured_image);

        camera = checkDeviceCamera();
        mImageSurfaceView = new ImageSurfaceView(MainActivity.this, camera);
        cameraPreviewLayout.addView(mImageSurfaceView);

        Button captureButton = (Button)findViewById(R.id.button);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(null, null, pictureCallback);
                camera.startPreview();
                mHandler.postDelayed(mStartActivityTask, 5000);
            }
        });
    }

    private Runnable mStartActivityTask= new Runnable() {
        public void run() {
            // Start the Activity
            camera.takePicture(null, null, pictureCallback);
//            demoFaceDet(picturePath);
//            camera.startPreview();
            faceDet(picturePath);
        }
    };


    private Camera checkDeviceCamera(){
        Camera mCamera = null;
        try {
//            mCamera = Camera.open();
            mCamera = openFrontFacingCameraGingerbread();
            mCamera.setDisplayOrientation(90);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mCamera;
    }

    private Camera openFrontFacingCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Toast.makeText(MainActivity.this, "Camera failed to open", Toast.LENGTH_LONG).show();
                }
            }
        }
        return cam;
    }


    PictureCallback pictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = decodeSampledBitmapFromResource(data, 300,200);
            if(bitmap==null){
                Toast.makeText(MainActivity.this, "Captured image is empty", Toast.LENGTH_LONG).show();
                return;
            }
            capturedImageHolder.setImageBitmap(bitmap);
            picturePath = saveImage(bitmap);
        }
    };

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(byte[] data, int   reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        //options.inPurgeable = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void demoFaceDet(final String imgPath) {
        new AsyncTask<Void, Void, List<VisionDetRet>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showDiaglog("Detecting faces");
            }

            @Override
            protected void onPostExecute(List<VisionDetRet> faceList) {
//                super.onPostExecute(faceList);
//                if (faceList.size() > 0) {
//                    Card card = new Card.Builder(MainActivity.this)
//                            .withProvider(BigImageCardProvider.class)
//                            .setDrawable(drawRect(imgPath, faceList, Color.GREEN))
//                            .setTitle("Face det")
//                            .endConfig()
//                            .build();
//                    mCard.add(card);
//                } else {
//                    Toast.makeText(getApplicationContext(), "No face", Toast.LENGTH_LONG).show();
//                }
//                updateCardListView();
//                dismissDialog();
                float resizeRatio = 1;
                for (final VisionDetRet ret : faceList) {
                    String label = ret.getLabel(); // If doing face detection, it will be 'Face'
                    int rectLeft = ret.getLeft();
                    int rectTop= ret.getTop();
                    int rectRight = ret.getRight();
                    int rectBottom = ret.getBottom();
                    ArrayList<Point> landmarks = ret.getFaceLandmarks();
                    for (Point point : landmarks) {
                        int pointX = (int) (point.x * resizeRatio);
                        int pointY = (int) (point.y * resizeRatio);
                        // Get the point of the face landmarks
                        System.out.println(pointX + ", " + pointY);
                    }
                }
            }

            @Override
            protected List<VisionDetRet> doInBackground(Void... voids) {
                // Init
                if (mFaceDet == null) {
                    mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
                }

                final String targetPath = Constants.getFaceShapeModelPath();
                if (!new File(targetPath).exists()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Copy landmark model to " + targetPath, Toast.LENGTH_SHORT).show();
                        }
                    });
                    FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.shape_predictor_68_face_landmarks, targetPath);
                }

                List<VisionDetRet> faceList = mFaceDet.detect(imgPath);
                return faceList;
            }
        }.execute();
    }


    private void faceDet(final String imgPath) {
        showDiaglog("Detecting faces");
        if (mFaceDet == null) {
            mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        }

        final String targetPath = Constants.getFaceShapeModelPath();
        if (!new File(targetPath).exists()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Copy landmark model to " + targetPath, Toast.LENGTH_SHORT).show();
                }
            });
            FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.shape_predictor_68_face_landmarks, targetPath);
        }

        List<VisionDetRet> faceList = mFaceDet.detect(imgPath);
        float resizeRatio = 1;
        for (final VisionDetRet ret : faceList) {
            String label = ret.getLabel(); // If doing face detection, it will be 'Face'
            int rectLeft = ret.getLeft();
            int rectTop= ret.getTop();
            int rectRight = ret.getRight();
            int rectBottom = ret.getBottom();
            ArrayList<Point> landmarks = ret.getFaceLandmarks();
            for (Point point : landmarks) {
                int pointX = (int) (point.x * resizeRatio);
                int pointY = (int) (point.y * resizeRatio);
                // Get the point of the face landmarks
                System.out.println(pointX + ", " + pointY);
            }
        }
    }



    // Saving pictures
    private String saveImage(Bitmap image) {
        FileOutputStream out = null;

        File pictureFileDir = getDir();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "Picture_" + date + ".jpg";

        String filename = pictureFileDir.getPath() + File.separator + photoFile;

        File pictureFile = new File(filename);
        try {
            out = new FileOutputStream(filename);
            image.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(MainActivity.this, pictureFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        return pictureFile.getAbsolutePath();
    }

    private File getDir() {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(sdDir, "CameraAPIDemo");
    }
    // end saving picture section


    private void showDiaglog(String title) {
        dismissDialog();
        mDialog = ProgressDialog.show(MainActivity.this, title, "process..", true);
    }

    private void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
