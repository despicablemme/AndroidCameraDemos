package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Preview;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.myapplication.databinding.ActivityPicViewBinding;
import com.google.android.material.color.utilities.Scheme;

public class PicViewActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static String TAG = "PicViewPage";
    private ActivityPicViewBinding binding;
    private Uri uri;
    SurfaceHolder surfaceHolder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPicViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.i(TAG, "onCreate: layout binded to this activity");

        Intent intent = getIntent();
        uri = intent.getData();

        surfaceHolder = binding.captureView.getHolder();
        surfaceHolder.addCallback(this);

        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_MAIN));
            }
        });
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Bitmap bitmap = BitmapFactory.decodeFile(getPathFromUri(uri));
        if (bitmap == null) {
            Log.e(TAG, "onResume: bitmap is null");
        }
        Log.d(TAG, "onImageAvailable: priv bitmap w and h are" + bitmap.getWidth() + "*" + bitmap.getHeight());
        Canvas canvas=null;
        while(canvas == null){
            canvas = surfaceHolder.lockCanvas();
            Log.e(TAG, "onResume: try to lock canvas");
        }
        Log.e(TAG, "onResume: lock canvas success");
        canvas.drawBitmap(bitmap, 0, 0, new Paint());
        surfaceHolder.unlockCanvasAndPost(canvas);

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    @Override
    protected void onResume(){
        super.onResume();
        // 接收intent
//        Intent intent = getIntent();
//        intent.getData();
        //


//        if(surfaceHolder.getSurface() != null) {
//            Log.w(TAG, "onResume: " + uri.getPath());
//            Bitmap bitmap = BitmapFactory.decodeFile(getPathFromUri(uri));
//            if (bitmap == null) {
//                Log.e(TAG, "onResume: bitmap is null");
//            }
//            Log.d(TAG, "onImageAvailable: priv bitmap w and h are" + bitmap.getWidth() + "*" + bitmap.getHeight());
//            Canvas canvas=null;
//            while(canvas == null){
//                canvas = surfaceHolder.lockCanvas();
//                Log.e(TAG, "onResume: try to lock canvas");
//            }
//            Log.e(TAG, "onResume: lock canvas success");
//            canvas.drawBitmap(bitmap, 0, 0, new Paint());
//            surfaceHolder.unlockCanvasAndPost(canvas);
//        }
    }

    private String getPathFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }
}