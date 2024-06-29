package com.nurtaz.dev.posedetection;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    int PERMISSION_REQUEST = 1;
    PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;
    // Base pose detector with streaming frames, when depending on the pose-detection sdk
    PoseDetectorOptions options = new PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build();

    PoseDetector poseDetector = PoseDetection.getClient(options);

    Canvas canvas;
    Paint mPaint = new Paint();
    Display display;
    Bitmap bitmapSave;
    ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
    ArrayList<Bitmap> dispayArrayList = new ArrayList<>();
    ArrayList<Pose> dispayPoseArrayList = new ArrayList<>();
    boolean isRunnng = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.preView);
        display = findViewById(R.id.display);
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);

        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(10);

        cameraProviderListenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {

            }
        }, ContextCompat.getMainExecutor(this));

        if (!allPermissionsGranted()) {
            getRuntimePemission();
        }
    }


    Runnable runMLKit = new Runnable() {
        @Override
        public void run() {
      poseDetector.process(InputImage.fromBitmap(bitmapArrayList.get(0),0))
              .addOnSuccessListener(new OnSuccessListener<Pose>() {
                  @Override
                  public void onSuccess(Pose pose) {
                      dispayPoseArrayList.add(pose);
                  }
              })
              .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {

          }
      });
        }
    };
    void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector selector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                //.setTargetResolution(new Size(1280,720))
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ActivityCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                int rotationDegree = imageProxy.getImageInfo().getRotationDegrees();
                @OptIn(markerClass = ExperimentalGetImage.class) ByteBuffer byteBuffer = imageProxy.getImage().getPlanes()[0].getBuffer();
                byteBuffer.rewind();
                Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(),imageProxy.getHeight(),Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(byteBuffer);

                Matrix matrix = new Matrix();
                matrix.postRotate(-270);
                matrix.postScale(-1,1);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,imageProxy.getWidth(),imageProxy.getHeight(),matrix,false);


//                @OptIn(markerClass = ExperimentalGetImage.class) Image mediaImage = imageProxy.getImage();
//                if (mediaImage != null) {
//                    InputImage image = InputImage.fromBitmap(rotatedBitmap, 0);
//                    // Pass image to an ML Kit Vision API
//                    // ...
//
//
//                    Task<Pose> result = poseDetector.process(image).addOnSuccessListener(new OnSuccessListener<Pose>() {
//                        @Override
//                        public void onSuccess(Pose pose) {
//                            // Task completed successfully
//                            // ...
//                            canvas = new Canvas(rotatedBitmap);
//                            for (PoseLandmark poseLandmark : pose.getAllPoseLandmarks()){
//                                Log.i("TAg", "Posses data x: " + String.valueOf(poseLandmark.getPosition().x));
//                                Log.i("TAg", "Posses data y: " + String.valueOf(poseLandmark.getPosition().y));
//                                canvas.drawCircle(poseLandmark.getPosition().x,poseLandmark.getPosition().y,5,mPaint);
//                            }
//                            display.getBitmap(rotatedBitmap);
//                        }
//                    }).addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            // Task failed with an exception
//                            // ...
//                        }
//                    });
//                }
                bitmapArrayList.add(rotatedBitmap);
                if (dispayPoseArrayList.size() >= 1){
                    canvas = new Canvas(bitmapArrayList.get(0));
                    for (PoseLandmark poseLandmark : dispayPoseArrayList.get(0).getAllPoseLandmarks()){
                        canvas.drawCircle(poseLandmark.getPosition().x,poseLandmark.getPosition().y,5,mPaint);
                    }
                    dispayArrayList.clear();
                    dispayArrayList.add(bitmapArrayList.get(0));
                    bitmapSave = bitmapArrayList.get(bitmapArrayList.size() - 1);
                    bitmapArrayList.clear();
                    bitmapArrayList.add(bitmapSave);
                    dispayPoseArrayList.clear();
                    isRunnng = false;

                }
                if (dispayPoseArrayList.size()== 0 && bitmapArrayList.size() >= 1 && !isRunnng){
                    runMLKit.run();
                    isRunnng = true;
                }
                if (dispayArrayList.size() >= 1){
                    display.getBitmap(dispayArrayList.get(0));
                }
                imageProxy.close();
            }
        });


        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, selector, imageAnalysis, preview);
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermission()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredPermission() {
        try {
            PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
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

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.i("TAg", "Permission Granted" + permission);
            return true;
        }
        Log.i("TAg", "Permission Not Granted" + permission);
        return false;
    }

    private void getRuntimePemission() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermission()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }
        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUEST);
        }
    }
}