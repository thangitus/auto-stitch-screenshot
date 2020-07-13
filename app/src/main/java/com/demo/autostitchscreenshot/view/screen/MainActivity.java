package com.demo.autostitchscreenshot.view.screen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.demo.autostitchscreenshot.R;
import com.demo.autostitchscreenshot.databinding.ActivityMainBinding;
import com.demo.autostitchscreenshot.usecase.StitchImgPresenter;
import com.demo.autostitchscreenshot.usecase.StitchImgUseCase;
import com.demo.autostitchscreenshot.utils.Callback;
import com.demo.autostitchscreenshot.utils.Constants;
import com.demo.autostitchscreenshot.utils.ItemTouchHelperCallback;
import com.demo.autostitchscreenshot.utils.SpacingItemDecoration;
import com.demo.autostitchscreenshot.view.adapter.InputScreenshotAdapter;
import com.demo.autostitchscreenshot.view.dialog.MessageDialog;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static androidx.core.util.Preconditions.checkNotNull;

public class MainActivity extends AppCompatActivity implements Callback.WithPair<String, Integer>, Callback.ItemTouchListener, StitchImgUseCase.View {
   private static final String TAG = MainActivity.class.getSimpleName();
   private static final int REQUEST_CODE = 1;
   private static final int REQUEST_CODE_CHOOSE = 10;

   private StitchImgUseCase.Presenter presenter;
   private InputScreenshotAdapter adapter;
   private ActivityMainBinding binding;
   private MessageDialog messageDialog;
   private long startTime;
   private long endTime;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      binding = ActivityMainBinding.inflate(getLayoutInflater());
      setContentView(binding.getRoot());
      setPresenter(new StitchImgPresenter(this));
      initUI();
   }

   private void initUI() {
      adapter = new InputScreenshotAdapter(this, this);
      ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(this);
      ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
      itemTouchHelper.attachToRecyclerView(binding.listInput);
      binding.listInput.setAdapter(adapter);
      binding.listInput.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
      binding.listInput.addItemDecoration(new SpacingItemDecoration(25));
   }

   public void selectImage(View v) {
      if (checkPermission()) {
         binding.scrollViewResult.setVisibility(View.GONE);
         binding.listInput.setVisibility(View.VISIBLE);
         //         sendIntentPickImg();
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

   private void sendIntentPickImg() {
      Intent intent = new Intent();
      intent.setType("image/*");
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
      intent.setAction(Intent.ACTION_GET_CONTENT);
      startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE);

   }

   public void stitchImages(View v) {
      startTime = System.currentTimeMillis();
      presenter.stitchImages();
   }
   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      List<String> imgPaths = new ArrayList<>();
      if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && null != data)
         if (data.getData() != null) {
            Uri uri = data.getData();
            imgPaths.add(getPath(this, uri));
            //            imgPaths.add(uri.getPath()
            //                            .replace("/document/raw:/", ""));
         } else {
            // When multiple images are selected.
            if (data.getClipData() != null) {
               ClipData clipData = data.getClipData();
               for (int i = 0; i < clipData.getItemCount(); i++) {
                  ClipData.Item item = clipData.getItemAt(i);
                  Uri uri = item.getUri();
                  imgPaths.add(getPath(this, uri));
                  //                  imgPaths.add(uri.getPath()
                  //                                  .replace("/document/raw:/", ""));
               }
            }
         }

      if (imgPaths.size() > 0) {
         Collections.sort(imgPaths);
         adapter.setData(imgPaths);
         binding.emptyLayout.setVisibility(View.GONE);
         adapter.notifyDataSetChanged();
         presenter.readSrc(imgPaths);
      }

      if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CHOOSE) {
         String fileName = data.getStringExtra("FileName");
         binding.emptyLayout.setVisibility(View.GONE);
         binding.scrollViewResult.setVisibility(View.VISIBLE);
         binding.listInput.setVisibility(View.GONE);
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
            sendIntentPickImg();
         }
      }
   }

   @Override
   public void onMove(int fromPos, int toPos) {
      adapter.onRowMoved(fromPos, toPos);
      presenter.move(fromPos, toPos);
   }

   @Override
   public void swipe(int position, int direction) {
      presenter.delete(position);
      adapter.removeItem(position);
      if (adapter.isEmptyData())
         binding.emptyLayout.setVisibility(View.VISIBLE);
   }

   @Override
   public void run(String option, Integer index) {
      if (option.equalsIgnoreCase(Constants.REMOVE)) {
         adapter.removeItem(index);
         presenter.delete(index);
         if (adapter.isEmptyData())
            binding.emptyLayout.setVisibility(View.VISIBLE);
      }
   }

   @Override
   public void onStitchImageSuccess(Bitmap img) {
      Intent intent = new Intent(this, ResultActivity.class);
      intent.putExtra("RESULT", img);
      startActivity(intent);
   }

   @Override
   public void onError(String msg) {
      if (messageDialog == null)
         messageDialog = new MessageDialog(this);

      messageDialog.show("Error", msg, "Accept", null);
   }
   @Override
   public void showResult(Bitmap bitmap) {
      endTime = System.currentTimeMillis();
      binding.scrollViewResult.setVisibility(View.VISIBLE);
      binding.resultImg.setImageBitmap(bitmap);
      binding.listInput.setVisibility(View.GONE);
      Toast.makeText(this, (endTime - startTime) + " Millis", Toast.LENGTH_SHORT)
           .show();
   }

   @Override
   public void showProgress() {
      binding.loader.setVisibility(View.VISIBLE);
   }

   @Override
   public void hideProgress() {
      binding.loader.setVisibility(View.GONE);
   }

   @SuppressLint("RestrictedApi")
   @Override
   public void setPresenter(StitchImgUseCase.Presenter presenter) {
      this.presenter = checkNotNull(presenter);
   }

   @Override
   protected void onResume() {
      super.onResume();
//      OpenCVLoader.initDebug();
//      loadLibrary("opencv_java3");
      loadLibrary("opencv_core");
      loadLibrary("opencv_flann");
      loadLibrary("opencv_imgproc");
      loadLibrary("opencv_features2d");
      loadLibrary("opencv_imgcodecs");
      loadLibrary("opencv_highgui");
   }

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
            final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

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

   private static boolean loadLibrary(String Name) {
      boolean result = true;

      Log.d(TAG, "Trying to load library " + Name);
      try {
         System.loadLibrary(Name);
         Log.d(TAG, "Library " + Name + " loaded");
      } catch (UnsatisfiedLinkError e) {
         Log.d(TAG, "Cannot load library \"" + Name + "\"");
         e.printStackTrace();
         result = false;
      }

      return result;
   }
}
