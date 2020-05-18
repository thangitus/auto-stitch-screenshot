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
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
      binding.listInput.addItemDecoration(new SpacingItemDecoration(16));
   }
   public void selectImage(View v) {
      if (!checkPermission())
         return;
      Intent intent = new Intent();
      intent.setType("image/*");
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
      intent.setAction(Intent.ACTION_GET_CONTENT);
      startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE);
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      List<String> imgPaths = new ArrayList<>();
      if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && null != data)
         if (data.getData() != null) {
            Uri uri = data.getData();
            //            imgPaths.add(getPath(this, uri));
            imgPaths.add(uri.getPath()
                            .replace("/document/raw:/", ""));
         } else {
            // When multiple images are selected.
            if (data.getClipData() != null) {
               ClipData clipData = data.getClipData();
               for (int i = 0; i < clipData.getItemCount(); i++) {
                  ClipData.Item item = clipData.getItemAt(i);
                  Uri uri = item.getUri();
                  //                  imgPaths.add(getPath(this, uri));
                  imgPaths.add(uri.getPath()
                                  .replace("/document/raw:/", ""));
               }
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
   // Implementation of the getPath() method and all its requirements is taken from the StackOverflow Paul Burke's answer: https://stackoverflow.com/a/20559175/5426539
   private String getPath(final Context context, final Uri uri) {

      final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

      // DocumentProvider
      if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
         // ExternalStorageProvider
         if (isExternalStorageDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            if ("primary".equalsIgnoreCase(type)) {
               return Environment.getExternalStorageDirectory() + "/" + split[1];
            }
         }
         // DownloadsProvider
         else if (isDownloadsDocument(uri)) {

            final String id = DocumentsContract.getDocumentId(uri);
            final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

            return getDataColumn(context, contentUri, null, null);
         }
         // MediaProvider
         else if (isMediaDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            Uri contentUri = null;
            if ("image".equals(type)) {
               contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
               contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
               contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            final String selection = "_id=?";
            final String[] selectionArgs = new String[]{split[1]};

            return getDataColumn(context, contentUri, selection, selectionArgs);
         }
      }
      // MediaStore (and general)
      else if ("content".equalsIgnoreCase(uri.getScheme())) {
         return getDataColumn(context, uri, null, null);
      }
      // File
      else if ("file".equalsIgnoreCase(uri.getScheme())) {
         return uri.getPath();
      }

      return null;
   }
   private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
      Cursor cursor = null;
      final String column = "_data";
      final String[] projection = {column};

      try {
         cursor = context.getContentResolver()
                         .query(uri, projection, selection, selectionArgs, null);
         if (cursor != null && cursor.moveToFirst()) {
            final int column_index = cursor.getColumnIndexOrThrow(column);
            return cursor.getString(column_index);
         }
      } finally {
         if (cursor != null)
            cursor.close();
      }
      return null;
   }

   private boolean isExternalStorageDocument(Uri uri) {
      return "com.android.externalstorage.documents".equals(uri.getAuthority());
   }
   private boolean isDownloadsDocument(Uri uri) {
      return "com.android.providers.downloads.documents".equals(uri.getAuthority());
   }
   private boolean isMediaDocument(Uri uri) {
      return "com.android.providers.media.documents".equals(uri.getAuthority());
   }
}
