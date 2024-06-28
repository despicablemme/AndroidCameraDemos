package com.example.camera2demo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.camera2demo.databinding.ActivityMainBinding;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "gyhCamera2Demo";
    ActivityMainBinding binding;
    CameraManager cameraManager;
    String[] cameraIds;
    CameraDevice mCamera;
    ArrayList<Surface> surfaces = new ArrayList<>();
    ImageReader imageReader;
    Image image = null;
    SurfaceHolder holderPost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: layout binding this activity");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        addSurface();

        getCameraManager();
    }

    private void addSurface(){
        Log.d(TAG, "onCreate: create surfaces, surfaceView and ImageReader surface");
        Surface surfacePreview = binding.preview.getHolder().getSurface();
        surfaces.add(surfacePreview);
        imageReader = ImageReader.newInstance(1856, 1392, ImageFormat.JPEG, 1);
        setImageReader();
        Surface surfaceEdit = imageReader.getSurface();
        surfaces.add(surfaceEdit);
    }

    private void getCameraManager() {
        Log.d(TAG, "onCreate: get camera manager");
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.d(TAG, "onCreate: get camera ids");
        try {
            cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                Log.d(TAG, "onCreate: camera id is " + id);
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    void setImageReader(){
        holderPost = binding.previewAfter.getHolder();
        // holderPost.
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.i(TAG, "onImageAvailable: -----");
                image = reader.acquireNextImage();
                if(image!=null){
                    // image info
                    int w = image.getWidth(); int h = image.getHeight();
                    Log.d(TAG, "onImageAvailable: prev image w and h are "+ w+"*"+h);
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    // setup rotate
                    Matrix matrix = new Matrix();
                    boolean isback = isFacingBack();
                    int rotate = getRotate(isback);
                    matrix.setRotate(rotate);
                    //
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                    Log.d(TAG, "onImageAvailable: priv bitmap w and h are" + bitmap.getWidth()+"*"+bitmap.getHeight());
                    bitmap = Bitmap.createBitmap(bitmap, 288, 366, 1280, 720, matrix, false);
                    Canvas canvas = holderPost.lockCanvas();
                    canvas.drawBitmap(bitmap, 0, 0, new Paint());
                    holderPost.unlockCanvasAndPost(canvas);

                    image.close();
                }
            }
        }, null);
    };

    boolean isFacingBack(){
        String id = mCamera.getId();
        return id.equals("0");
    }

    int getRotate(boolean isback){
        if(isback){
            return 90;
        }else{
            return 270;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    123);
            Log.d(TAG, "onCreate: requeste permission");
            return;
        }
        Log.d(TAG, "onResume: re-open camera, creating session, and capture request");
        try {
            cameraManager.openCamera(cameraIds[0], new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCamera = camera;
                    try {
                        camera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                try {
                                    CaptureRequest.Builder captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                                    for(Surface surface : surfaces) {
                                        captureRequestBuilder.addTarget(surface);
                                        Log.w(TAG, "onConfigured: surface added ");
                                    }
                                    CaptureRequest captureRequest = captureRequestBuilder.build();
                                    session.setRepeatingRequest(captureRequest, new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                                                     @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                                            // Log.i(TAG, "onCaptureStarted: captured a frame with timestamp" + timestamp
                                            //         + ";; and with frame number:"+frameNumber);
                                        }
                                    }, null);
                                } catch (CameraAccessException e) {

                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    releaseCamera();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "openCamera onError: " + cameraIds[0] + "error code is " + error);
                    releaseCamera();
                }
            }, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected void onPause(){
        super.onPause();

    }

    @Override
    protected void onStop(){
        super.onStop();
        releaseCamera();
    }

    void releaseCamera(){
        if(mCamera!=null){
            mCamera.close();
            mCamera = null;
        }
    }
}