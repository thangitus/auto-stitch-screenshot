package com.demo.autostitchscreenshot.view.screen;

import android.Manifest;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.demo.autostitchscreenshot.databinding.ActivityMainBinding;
import com.demo.autostitchscreenshot.usecase.StitchImgPresenter;
import com.demo.autostitchscreenshot.utils.Callback;
import com.demo.autostitchscreenshot.utils.SpacingItemDecoration;
import com.demo.autostitchscreenshot.view.adapter.InputScreenshotAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Callback.WithPair {
   private static final String TAG = "MainActivity";
   private static final int REQUEST_CODE = 1;

   private StitchImgPresenter presenter;
   private InputScreenshotAdapter adapter;
   private ActivityMainBinding binding;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      binding = ActivityMainBinding.inflate(getLayoutInflater());
      setContentView(binding.getRoot());
      initUI();
   }
   private void initUI() {
      adapter = new InputScreenshotAdapter(this, this);
      binding.listInput.setAdapter(adapter);
      binding.listInput.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
      binding.listInput.addItemDecoration(new SpacingItemDecoration(25));
   }
   public void selectImage(View v) {
      if (!checkPermission())
         return;
      Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
      String[] mimeTypes = {"image/*"};
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
      intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
      startActivityForResult(intent, REQUEST_CODE);
   }

   private String getRealPathFromURI(Uri contentUri) {
      String[] projection = {MediaStore.Images.Media.DATA};
      Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
      String realPath = "";
      if(cursor!=null && cursor.moveToFirst()){
         int colIndex = cursor.getColumnIndex(projection[0]);
         realPath = cursor.getString(colIndex);
         cursor.close();
      }
      return realPath;
   }
   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      List<String> imgPaths = new ArrayList<>();
      if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && null != data)
            // When multiple images are selected.
            if (data.getClipData() != null) {
               ClipData clipData = data.getClipData();
               Log.d("uri", clipData.toString());
               for (int i = 0; i < clipData.getItemCount(); i++) {
                  ClipData.Item item = clipData.getItemAt(i);
                  Uri uri = item.getUri();
                  String imgPath = getRealPathFromURI(uri);
                  Log.d("path", imgPath);
                  imgPaths.add(imgPath);
               }
            }

      if (imgPaths.size() > 0) {
         adapter.setImgPaths(imgPaths);
         adapter.notifyDataSetChanged();
         binding.emptyLayout.setVisibility(View.GONE);
      }
      super.onActivityResult(requestCode, resultCode, data);
   }

   public boolean checkPermission() {
      int result = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
      if (result == PackageManager.PERMISSION_DENIED) {
         ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
         return false;
      }
      return true;
   }
   @Override
   public void run(Object o, Object o2) {

   }
}
