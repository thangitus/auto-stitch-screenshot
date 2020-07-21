package com.zhihu.matisse;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import org.opencv.core.Core;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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

   @SuppressLint({"RestrictedApi", "HandlerLeak"})
   public StitchImgPresenter(StitchImgUseCase.View view) {
      this.view = view;
      handler = new Handler() {
         @Override
         public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == CHECK) {
               Boolean result = (Boolean) msg.obj;
               if (result)
                  view.enableButtonStitch();
               else
                  view.disableButtonStitch();

               if (runnable != null)
                  pool.submit(runnable);
            }
            if (msg.what == DONE) {
               view.hideProgress();
               view.returnFileName((String) msg.obj);
            }
         }
      };
   }

   @Override
   public void onUpdateSelectedPaths(List<String> selectedPaths) {
      if (selectedPaths.size() < 2)
         sendSuggest(false);
      runnable = new Runnable() {
         @Override
         public void run() {
            flag = false;
            boolean res = checkNativeStitch(selectedPaths.toArray(new String[0]));
            sendSuggest(res);
            flag = true;
         }
      };
      if (flag) {
         pool.submit(runnable);
         runnable = null;
      }
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

   public native boolean checkNativeStitch(String[] selectedPaths);

}
