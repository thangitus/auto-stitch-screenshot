package com.zhihu.matisse;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StitchImgPresenter implements StitchImgUseCase.Presenter {
   private static final String TAG = "StitchImgPresenter";
   private static final int DONE = 1010;
   private static final int CHECK = 11;
   ExecutorService pool = Executors.newSingleThreadExecutor();
   private boolean flag = true;
   private Handler handler;
   private StitchImgUseCase.View view;
   private Runnable runnable = null;
   private HashMap<String, Bitmap> hashMapSrc;
   List<String> selectedPaths;
   long start, end;
   @SuppressLint({"RestrictedApi", "HandlerLeak"})
   public StitchImgPresenter(StitchImgUseCase.View view) {
      this.view = view;
      hashMapSrc = new HashMap<>();
      handler = new Handler() {
         @Override
         public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == CHECK) {
               Boolean result = (Boolean) msg.obj;
               if (result)
                  view.showButtonStitch();
               else
                  view.hideButtonStitch();

               if (runnable != null) {
                  pool.submit(runnable);
                  runnable = null;
               }
            }
            if (msg.what == DONE) {
               view.hideProgress();
               view.enableButtonStitch();
               view.toast("Stitch " + (end - start) + " Millis");
               view.returnFileName((String) msg.obj);
            }
         }
      };
   }
   @Override
   public void onUpdateSelectedPaths(List<String> selectedPaths) {
      this.selectedPaths = selectedPaths;
      if (selectedPaths.size() < 2) {
         sendSuggest(false);
         return;
      }
      runnable = new Runnable() {
         @Override
         public void run() {
            flag = false;
            if (selectedPaths.size() >= 5)
               Collections.sort(selectedPaths);
            List<Bitmap> listSrc = getBitmaps(selectedPaths);
            boolean res = checkNativeStitch(selectedPaths.toArray(new String[0]), listSrc.toArray(new Bitmap[0]));
            sendSuggest(res);
            flag = true;
         }
      };
      if (flag) {
         view.hideButtonStitch();
         pool.submit(runnable);
         runnable = null;
      }
   }
   @Override
   public void stitchImages() {
      view.disableButtonStitch();
      view.showProgress();
      runnable = new Runnable() {
         @Override
         public void run() {
            start = System.currentTimeMillis();
            if (selectedPaths.size() >= 5)
               Collections.sort(selectedPaths);
            List<Bitmap> listSrc = getBitmaps(selectedPaths);
            Bitmap result = stitchNative(selectedPaths.toArray(new String[0]), listSrc.toArray(new Bitmap[0]));
            end = System.currentTimeMillis();
            String[] part = selectedPaths.get(0)
                                         .split("/");
            StringBuilder fileName = new StringBuilder();
            for (int i = 1; i < part.length - 1; i++) {
               fileName.append(part[i]);
               fileName.append("/");
            }
            fileName.append("Result.jpg");

            try (FileOutputStream out = new FileOutputStream(fileName.toString())) {
               result.compress(Bitmap.CompressFormat.JPEG, 100, out);
               sendLocalPathMessage(fileName.toString());
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      };
      pool.submit(runnable);
   }
   private List<Bitmap> getBitmaps(List<String> selectedPaths) {
      List<Bitmap> listSrc = new ArrayList<>();
      for (String path : selectedPaths) {
         Bitmap bitmap = hashMapSrc.get(path);
         if (bitmap == null) {
            bitmap = BitmapFactory.decodeFile(path);
            hashMapSrc.put(path, bitmap);
         }
         listSrc.add(bitmap);
      }
      return listSrc;
   }

   private void sendSuggest(boolean b) {
      flag = true;
      Message message = new Message();
      message.what = CHECK;
      message.obj = b;
      handler.sendMessage(message);
   }
   private void sendLocalPathMessage(String fileName) {
      Message message = new Message();
      message.what = DONE;
      message.obj = fileName;
      handler.sendMessage(message);
   }

   public native boolean checkNativeStitch(String[] selectedPaths, Bitmap[] listSrc);
   public native Bitmap stitchNative(String[] selectedPaths, Bitmap[] listSrc);
}
