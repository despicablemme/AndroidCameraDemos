package com.example.myapplication;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.Manifest;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Camera;
import androidx.camera.core.ImageAnalysis;

import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionFilter;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.camera.core.ImageCapture;


import com.example.myapplication.databinding.ActivityMainBinding;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "gyhCameraDemo";
    private View view;
    private ActivityMainBinding binding;
    private Camera camera;
    private CameraControl cameraControl;
    private ProcessCameraProvider processCameraProvider;
    private Preview preview;
    boolean is_back_camera=true;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private ResolutionSelector resolutionSelector;
    private CameraSelector cameraSelector;
    private int nameNumber = 0;
    private final int maxNameLength = 8;
    // 前置相机rotation is 90
    private Size outputSize = new Size(1280, 720);
    private float zoom_linear_state = 0f;
    private static float zoom_step = 0.025f;
    private Uri curFileUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            Log.w(TAG, "onCreate: restore the last time uri is " + savedInstanceState.getString("last_uri"));
            if(savedInstanceState.getString("last_uri") != null) {
                curFileUri = Uri.parse(savedInstanceState.getString("last_uri"));
            }
        }else{
            Log.w(TAG, "onCreate: savedInstanceState is still null");}
        Log.d(TAG, "onCreate: On CREATING?????!!!!!!");
        // 绑定view
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        view = binding.getRoot();
        setContentView(view);

        // 申请camera权限
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);

        // 绑定多个用例的生命周期
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        getCameraProvider(cameraProviderFuture);

        // 按键设置，前后摄切换和拍摄键
        binding.frontBack.setOnClickListener(v -> onChangeFrontBack());
        binding.capturebtn.setOnClickListener(v -> onCapture());
        binding.viewCapture.setOnClickListener(v -> onViewCapture());
        // TODO: 为什么这里camera还是null？？？
        if(camera==null){
            Log.w("gyh", "camera is null in create???");
        }else{Log.w("gyh", "camera is not null in create");}
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if(savedInstanceState != null){
            Log.w(TAG, "onRestoreInstanceState: restore the last time uri is " + savedInstanceState.getString("last_uri"));
            if(savedInstanceState.getString("last_uri") != null) {
                curFileUri = Uri.parse(savedInstanceState.getString("last_uri"));
            }
        }else{
            Log.w(TAG, "onRestoreInstanceState: savedInstanceState is still null");}
    }
    @Override
    public void onSaveInstanceState(Bundle outState){
        if(curFileUri != null) {
            Log.w(TAG, "onSaveInstanceState: save the last uri " + curFileUri.toString());
            outState.putString("last_uri", curFileUri.toString());
        }
        super.onSaveInstanceState(outState);
    }

    private void getCameraProvider(ListenableFuture<ProcessCameraProvider> cameraProviderFuture){
        cameraProviderFuture.addListener(()-> {
                    try {
                        processCameraProvider = cameraProviderFuture.get();
                        lifeCycleBinder();
                    } catch (ExecutionException | InterruptedException e) { } catch (
                            CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
        }, ContextCompat.getMainExecutor(this));
    }
    private void lifeCycleBinder() throws CameraAccessException {
        // rotationDecider();
        resolutionSelector = new ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY).build();
        Log.i(TAG, "lifeCycleBinder: start preview building");
        preview = new Preview.Builder()
                // .setTargetResolution(outputSize)
                .setTargetRotation(view.getDisplay().getRotation())
                .setResolutionSelector(resolutionSelector)
                .build();
        preview.setSurfaceProvider(binding.myview.getSurfaceProvider());
        Log.i(TAG, "lifeCycleBinder: start CameraSelector building");
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        Log.i(TAG, "lifeCycleBinder: start ImageCapture building");
        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(view.getDisplay().getRotation())
                // .setTargetResolution(outputSize)
                .setResolutionSelector(resolutionSelector)
                .build();
        Log.i(TAG, "lifeCycleBinder: start ImageAnalysis building");
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetRotation(view.getDisplay().getRotation())
                // .setTargetResolution(outputSize)
                .setResolutionSelector(resolutionSelector)
                .build();
        processCameraProvider.unbindAll();
        Log.i(TAG, "lifeCycleBinder: cameraProvider unbinded in onCreate, ready to bind");
        camera = processCameraProvider.bindToLifecycle(this, cameraSelector,
                imageCapture, imageAnalysis, preview);
        Log.i(TAG, "lifeCycleBinder: cameraProvider life cycle bind success in onCreate");
    }

    // todo::::
//    private void rotationDecider() throws CameraAccessException {
//        CameraManager cameraManager = this.getSystemService(CameraManager.class);
//        String[] cameraIdList = cameraManager.getCameraIdList();
//        for(String cameraId:cameraIdList){
//            CameraCharacteristics cameraCharacteristics = cameraManager
//                    .getCameraCharacteristics(cameraId);
//            int cameraRotation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//            Log.d(TAG, "rotationDecider: "+cameraId);
//            Log.d(TAG, "rotationDecider: "+cameraRotation);
//            int screenRotation = view.getDisplay().getRotation();
//            int needRotation = getRotationNeeded(cameraRotation, screenRotation, cameraId);
//
//            // TODO::
//        }
//    }
    // TODO::
//    private int getRotationNeeded(int cameraRotation, int ScreenRotation, String cameraId) {
//        int dataRotation=0;
//        if(cameraId.equals("0")){
//            dataRotation = cameraRotation - ScreenRotation;
//        }else if(cameraId.equals("1")){
//            dataRotation = cameraRotation + 180 - ScreenRotation
//        }
//        if(dataRotation<0){dataRotation = 360+dataRotation;}
//        return dataRotation;
//    }
    // TODO::
    private void onChangeFrontBack() {
        Log.i(TAG, "onChangeFrontBack: in change front back function");
        is_back_camera = !is_back_camera;
        if(is_back_camera){
            // TODO: 这里为什么不能直接传递int 0或1 进去，一定要传递宏定义？
            cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();}
        else{Log.i(TAG, "onChangeFrontBack: need a new cameraSelector, building...");
            cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();}// 直接传int不行
        Log.i(TAG, "onChangeFrontBack: change to new cameraSelector ");
        processCameraProvider.unbindAll();
        Log.i(TAG, "onChangeFrontBack: unbind all for camera provider");
        camera = processCameraProvider.bindToLifecycle(this, cameraSelector,
                imageCapture, imageAnalysis, preview);
        Log.i(TAG, "onChangeFrontBack: new camera obj got, need a new cameraControl");
        cameraControl = camera.getCameraControl();
    }

    private void onViewCapture() {
        if(curFileUri==null){
            return;
        }
        // 使用隐式intent查找能打开的activity
//        Intent viewIntent = new Intent();
        // 使用显示intent调用自己的activity
        Intent viewIntent = new Intent(this, PicViewActivity.class);
        viewIntent.setData(curFileUri);
        viewIntent.setAction(Intent.ACTION_VIEW);
        startActivity(viewIntent);
    }

    protected void onResume(){
        super.onResume();
        // TODO: 为什么这里经过了两次onResume后camera变量才不是null？？？
        if(camera==null){
            Log.i(TAG, "onResume: camera is null in resume???");
        }else{
            Log.i(TAG, "onResume: camera not null already in resume???");
            cameraControl = camera.getCameraControl();
            Log.i(TAG, "onResume: cameraControl got, setting zoom bar listener");
//            binding.zoomBar.setOnSeekBarChangeListener(getSeekBarChangeListener());
//            Log.i(TAG, "onResume: zoom bar change listener set success");
            binding.zoomIn.setOnTouchListener(onZoomInTouchListener);
            binding.zoomOut.setOnTouchListener(onZoomOutTouchListener);
        }
    }

    private View.OnTouchListener onZoomInTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(zoom_linear_state<1) {
                zoom_linear_state += zoom_step;
                cameraControl.setLinearZoom(zoom_linear_state);
            }
//            while(zoom_linear_state<=1){
//
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            if(zoom_linear_state==1){
//                return true;
//            }
            return false;
        }
    };

    private View.OnTouchListener onZoomOutTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(zoom_linear_state>0) {
                zoom_linear_state -= zoom_step;
                cameraControl.setLinearZoom(zoom_linear_state);
            }
//            while(zoom_linear_state>=0){
//                zoom_linear_state -= 0.05;
//                cameraControl.setLinearZoom(zoom_linear_state);
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            if(zoom_linear_state==0){
//                return true;
//            }
            return false;
        }
    };

    private SeekBar.OnSeekBarChangeListener getSeekBarChangeListener(){
        Log.i(TAG, "getSeekBarChangeListener: new a zoom bar change listener and return");
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float ratio = (float) progress / seekBar.getMax();
                Log.w("gyh", "onProgressChanged: "+ ratio);
                cameraControl.setLinearZoom(ratio);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        };
    }

    private void onCapture(){
        Log.i(TAG, "onCapture: getting file name");
        // String name = getFileName();
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.SIMPLIFIED_CHINESE).format(System.currentTimeMillis());

        Log.i(TAG, "onCapture: file name got -- "+name);
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        Log.i(TAG, "onCapture: content values all set, setting output options of imageCapture");
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions
                        .Builder(getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues).build();
        Log.i(TAG, "onCapture: picture taking");

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String message = outputFileResults.toString();
                        Uri uri = outputFileResults.getSavedUri();
                        curFileUri = uri;
                        Log.i(TAG, "onCapture： " + message);
                        Log.i(TAG, "onCapture: save path is "+uri.getPath());

                        // todo: 图像裁剪
                        try {
                            ExifInterface exif = new ExifInterface("/storage/emulated/0/Pictures/CameraX-Image/"+name+".jpg");
                            int rotate = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                            int rotate_need=0;
                            if(rotate==6){rotate_need=90;}
                            else if(rotate==3){rotate_need=180;}
                            else if(rotate==8){rotate_need=270;}
                            Log.d(TAG, "onImageSaved: rotate need is "+rotate_need);
                            Log.d(TAG, "onImageSaved: image rotate before is  "+ rotate);
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            inputStream.close();
                            int w = bitmap.getWidth();
                            int h = bitmap.getHeight();
                            Log.d(TAG, "onImageSaved: original w = "+w+", h = "+h);
                            Matrix matrix = new Matrix();
                            int xbias=120, ybias = 600;
                            if(camera.getCameraInfo().getLensFacing()==CameraSelector.LENS_FACING_BACK){xbias=288; ybias = 366;}
                            matrix.setRotate(rotate_need);
                            Log.d(TAG, "onImageSaved: rotate need is "+rotate_need);
                            Bitmap bitmapn = Bitmap.createBitmap(bitmap, xbias, ybias, 1280, 720, matrix, false);
                            int wn = bitmapn.getWidth();
                            int hn = bitmapn.getHeight();
                            Log.d(TAG, "onImageSaved: clipped w = "+wn+", h = "+hn);
                            // TODO:
                            FileOutputStream outputStream = new FileOutputStream("/storage/emulated/0/Pictures/CameraX-Image/"+name+".jpg");
                            bitmapn.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

                            Log.d(TAG, "onImageSaved: clipped w = "+wn+", h = "+hn);
                            outputStream.flush();
                            Log.d(TAG, "onImageSaved: output stream flushed");
                            outputStream.close();
                            exif = new ExifInterface("/storage/emulated/0/Pictures/CameraX-Image/"+name+".jpg");
                            rotate = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                            Log.d(TAG, "onImageSaved: image rotate after is  "+ rotate);
                            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE));

                            // int
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }


                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "onCapture: "+exception.getCause().getMessage());
                        Log.w(TAG, "onCapture: onImageSaved Error");
                    }
                });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

    }
    private String getFileName(){
        nameNumber += 1;
        int numFix = maxNameLength - Integer.toString(nameNumber).length();
        String name = "";
        for(int i=0;i<numFix;i++){name += "0";};
        name += Integer.toString(nameNumber);
        return name;
    }

    private void getSupportedPreviewSizes(){
    }
}

