package com.tzutalin.dlibtest;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.provider.BigImageCardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.PedestrianDet;
import com.tzutalin.dlib.VisionDetRet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

//import com.dexafree.materialList.card.Card;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int RESULT_LOAD_IMG = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;
    private ImageSurfaceView mImageSurfaceView;
    private Camera camera;

    private ProgressDialog mDialog;
    private FrameLayout cameraPreviewLayout;
    private ImageView capturedImageHolder;
    private String picturePath;
    private List<Card> mCard = new ArrayList<>();
    private Handler mHandler = new Handler();
    private MaterialListView mListView;
    private FaceDet mFaceDet;
    private PedestrianDet mPersonDet;


    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        cameraPreviewLayout = (FrameLayout)findViewById(R.id.camera_preview);
        capturedImageHolder = (ImageView)findViewById(R.id.captured_image);

        camera = checkDeviceCamera();
        mImageSurfaceView = new ImageSurfaceView(MainActivity.this, camera);
        mListView = (MaterialListView) findViewById(R.id.material_listview);
        cameraPreviewLayout.addView(mImageSurfaceView);

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.M) {
            verifyPermissions(this);
        }

        Button captureButton = (Button)findViewById(R.id.button);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(null, null, pictureCallback);
                camera.startPreview();
                mHandler.postDelayed(mStartActivityTask, 2000);
//                Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
//                runDemosAsync(picturePath);
                demoPersonDet(picturePath);
            }
        });
    }

    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_persmission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_persmission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }

    private Runnable mStartActivityTask= new Runnable() {
        public void run() {
            // Start the Activity
            camera.takePicture(null, null, pictureCallback);

//            camera.startPreview();
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
            System.out.println("check picture captured");
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


    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    // Saving pictures
    private String saveImage(Bitmap image) {
        FileOutputStream out = null;

        File pictureFileDir = getDir();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "Picture_" + date + ".jpg";

        String filename = pictureFileDir.getPath() + File.separator + photoFile;

        image = RotateBitmap(image, -90);

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

    private void runDemosAsync(@NonNull final String imgPath) {
        demoPersonDet(imgPath);
        demoFaceDet(imgPath);
    }

    @DebugLog
    protected void demoStaticImage() {
        if (picturePath != null) {
            Timber.tag(TAG).d("demoStaticImage() launch a task to det");
            runDemosAsync(picturePath);
        } else {
            Timber.tag(TAG).d("demoStaticImage() mTestImgPath is null, go to gallery");
            Toast.makeText(MainActivity.this, "Pick an image to run algorithms", Toast.LENGTH_SHORT).show();
            // Create intent to Open Image applications like Gallery, Google Photos
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            Toast.makeText(MainActivity.this, "Demo using static images", Toast.LENGTH_SHORT).show();
            demoStaticImage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                picturePath = cursor.getString(columnIndex);
                cursor.close();
                if (picturePath != null) {
                    runDemosAsync(picturePath);
                    Toast.makeText(this, "Img Path:" + picturePath, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }
    }

    private void demoPersonDet(final String imgPath) {
        new AsyncTask<Void, Void, List<VisionDetRet>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(List<VisionDetRet> personList) {
                super.onPostExecute(personList);
                if (personList.size() > 0) {
                    Card card = new Card.Builder(MainActivity.this)
                            .withProvider(BigImageCardProvider.class)
                            .setDrawable(drawRect(imgPath, personList, Color.BLUE))
                            .setTitle("Person det")
                            .endConfig()
                            .build();
                    mCard.add(card);
                } else {
                    Toast.makeText(getApplicationContext(), "No person", Toast.LENGTH_LONG).show();
                }
                updateCardListView();
            }

            @Override
            protected List<VisionDetRet> doInBackground(Void... voids) {
                // Init
                if (mPersonDet == null) {
                    mPersonDet = new PedestrianDet();
                }

                Timber.tag(TAG).d("Image path: " + imgPath);

                List<VisionDetRet> personList = mPersonDet.detect(imgPath);
                return personList;
            }
        }.execute();
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
                super.onPostExecute(faceList);
                if (faceList.size() > 0) {
                    Card card = new Card.Builder(MainActivity.this)
                            .withProvider(BigImageCardProvider.class)
                            .setDrawable(drawRect(imgPath, faceList, Color.GREEN))
                            .setTitle("Face det")
                            .endConfig()
                            .build();
                    mCard.add(card);
                } else {
                    Toast.makeText(getApplicationContext(), "No face", Toast.LENGTH_LONG).show();
                }
                updateCardListView();
                dismissDialog();
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


    private BitmapDrawable drawRect(String path, List<VisionDetRet> results, int color) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);
        int width = bm.getWidth();
        int height = bm.getHeight();
        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();

        final int MAX_SIZE = 400;
        int newWidth = MAX_SIZE;
        int newHeight = MAX_SIZE;
        float resizeRatio = 1;
        newHeight = Math.round(newWidth / aspectRatio);
        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            Timber.tag(TAG).d("Resize Bitmap");
            bm = getResizedBitmap(bm, newWidth, newHeight);
            resizeRatio = (float) bm.getWidth() / (float) width;
            Timber.tag(TAG).d("resizeRatio " + resizeRatio);
        }

        // Create canvas to draw
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        // Loop result list
        for (VisionDetRet ret : results) {
            Rect bounds = new Rect();
            bounds.left = (int) (ret.getLeft() * resizeRatio);
            bounds.top = (int) (ret.getTop() * resizeRatio);
            bounds.right = (int) (ret.getRight() * resizeRatio);
            bounds.bottom = (int) (ret.getBottom() * resizeRatio);
            canvas.drawRect(bounds, paint);
            // Get landmark
            ArrayList<Point> landmarks = ret.getFaceLandmarks();
            for (Point point : landmarks) {
                int pointX = (int) (point.x * resizeRatio);
                int pointY = (int) (point.y * resizeRatio);
                canvas.drawCircle(pointX, pointY, 1, paint);
            }
        }

        return new BitmapDrawable(getResources(), bm);
    }

    private void updateCardListView() {
        mListView.clearAll();
        for (Card each : mCard) {
            mListView.add(each);
        }
    }

    @DebugLog
    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return resizedBitmap;
    }

}
