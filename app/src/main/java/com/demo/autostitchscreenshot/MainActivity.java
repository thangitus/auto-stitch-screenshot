package com.demo.autostitchscreenshot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.demo.autostitchscreenshot.databinding.ActivityMainBinding;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;

public class MainActivity extends AppCompatActivity{
   private static final String TAG = MainActivity.class.getSimpleName();
   private static final int REQUEST_CODE = 1;
   private static final int REQUEST_CODE_CHOOSE = 10;

   private ActivityMainBinding binding;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      binding = ActivityMainBinding.inflate(getLayoutInflater());
      setContentView(binding.getRoot());
   }


   public void selectImage(View v) {
      if (checkPermission()) {
         binding.scrollViewResult.setVisibility(View.GONE);
         Matisse.from(MainActivity.this)
                .choose(MimeType.ofImage(), false)
                .countable(true)
                .capture(true)
                .captureStrategy(new CaptureStrategy(true, "com.zhihu.matisse.sample.fileprovider", "test"))
                .maxSelectable(12)
                .theme(R.style.Matisse_Dracula)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.85f)
                .imageEngine(new GlideEngine())
                .showSingleMediaType(true)
                .originalEnable(true)
                .maxOriginalSize(10)
                .autoHideToolbarOnSingleTap(true)
                .forResult(REQUEST_CODE_CHOOSE);
      }
   }


   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CHOOSE) {
         String fileName = data.getStringExtra("FileName");
         binding.emptyLayout.setVisibility(View.GONE);
         binding.scrollViewResult.setVisibility(View.VISIBLE);
         Glide.with(this)
              .load(new File(fileName))
              .skipMemoryCache(true)
              .diskCacheStrategy(DiskCacheStrategy.NONE)
              .into(binding.resultImg);
      }
   }

   private boolean checkPermission() {
      int result = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
      if (result == PackageManager.PERMISSION_DENIED) {
         ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
         return false;
      }
      return true;
   }

   @Override
   public void onRequestPermissionsResult(int requestCode,
                                          @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
      if (requestCode == REQUEST_CODE) {
         if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectImage(binding.btnAdd);
         }
      }
   }
   static {
      Log.d(TAG, "Trying to load native-lib");
      try {
         System.loadLibrary("native-lib");
         Log.d(TAG, "Library native-lib loaded");
      } catch (UnsatisfiedLinkError e) {
         Log.d(TAG, "Cannot load library native-lib");
         e.printStackTrace();
      }
   }
}
