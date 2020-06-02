package com.demo.autostitchscreenshot.view.screen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.demo.autostitchscreenshot.databinding.ActivityMainBinding;
import com.demo.autostitchscreenshot.usecase.StitchImgPresenter;
import com.demo.autostitchscreenshot.usecase.StitchImgUseCase;
import com.demo.autostitchscreenshot.utils.Callback;
import com.demo.autostitchscreenshot.utils.Constants;
import com.demo.autostitchscreenshot.utils.ItemTouchHelperCallback;
import com.demo.autostitchscreenshot.utils.SpacingItemDecoration;
import com.demo.autostitchscreenshot.view.adapter.InputScreenshotAdapter;
import com.demo.autostitchscreenshot.view.dialog.MessageDialog;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static androidx.core.util.Preconditions.checkNotNull;
import static com.demo.autostitchscreenshot.utils.Utils.getRealImagePathFromURI;

public class MainActivity extends AppCompatActivity implements Callback.WithPair<String, Integer>, Callback.ItemTouchListener, StitchImgUseCase.View {
   private static final String TAG = MainActivity.class.getSimpleName();
   private static final int REQUEST_CODE = 1;

   private StitchImgUseCase.Presenter presenter;
   private InputScreenshotAdapter adapter;
   private ActivityMainBinding binding;
   private MessageDialog messageDialog;

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
      if (checkPermission())
         sendIntentPickImg();
   }

   private void sendIntentPickImg() {
      Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
      String[] mimeTypes = {"image/*"};
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
      intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
      startActivityForResult(intent, REQUEST_CODE);
   }

   public void stitchImages(View v) {
      presenter.stitchImages();
   }
   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      List<String> imgPaths = new ArrayList<>();
      if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && null != data)
         // When multiple images are selected.
         if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            for (int i = 0; i < clipData.getItemCount(); i++) {
               ClipData.Item item = clipData.getItemAt(i);
               Uri uri = item.getUri();
               String imgPath = getRealImagePathFromURI(uri, this);
               imgPaths.add(imgPath);
            }
         }

      if (imgPaths.size() > 0) {
         Collections.sort(imgPaths);
         adapter.addData(imgPaths);
         binding.emptyLayout.setVisibility(View.GONE);
         presenter.readSrc(imgPaths);
      }
      super.onActivityResult(requestCode, resultCode, data);
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
   }

   @Override
   public void swipe(int position, int direction) {
      adapter.removeItem(position);
      if (adapter.isEmptyData())
         binding.emptyLayout.setVisibility(View.VISIBLE);
   }

   @Override
   public void run(String option, Integer index) {
      if (option.equalsIgnoreCase(Constants.REMOVE)) {
         adapter.removeItem(index);
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
      binding.scrollViewResult.setVisibility(View.VISIBLE);
      binding.resultImg.setImageBitmap(bitmap);
      binding.listInput.setVisibility(View.GONE);
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
      boolean success = OpenCVLoader.initDebug();
      if (!success)
         Log.d("MainActivity", "Asynchronous initialization failed!");
      else
         Log.d("MainActivity", "Asynchronous initialization succeeded!");
   }
}
