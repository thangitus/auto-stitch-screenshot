package com.zhihu.matisse;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class StitchImgPresenter implements StitchImgUseCase.Presenter {
   private static final String TAG = "StitchImgPresenter";
   private static final float RATIO = 2;
   private static final float DEVIATION_X = 2;
   private static final float DEVIATION_Y = 100;
   private static final int DONE = 1010;
   private static final int MIN_MATCH = 20;
   private static final int CHECK = 11;

   private CountDownLatch latch;
   private HashMap<String, Mat> hashMapSrc;
   private HashMap<String, Boolean> cache;
   private List<String> selectedPaths;
   private float ratio_scale = 1;
   private List<List<KeyPoint>> keypoints;
   private List<Mat> decryptions;
   private Handler handler;
   private int minWidth;
   private StitchImgUseCase.View view;
   @SuppressLint({"RestrictedApi", "HandlerLeak"})
   public StitchImgPresenter(StitchImgUseCase.View view) {
      this.view = view;
      hashMapSrc = new HashMap<>();
      cache = new HashMap<>();
      minWidth = Integer.MAX_VALUE;

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
            }
            if (msg.what == DONE)
               view.returnFileName((String) msg.obj);
         }
      };
   }

   @Override
   public void stitchImages() {
      view.disableButtonStitch();
      List<Mat> src = new ArrayList<>();
      for (String file : selectedPaths) {
         Mat mat = hashMapSrc.get(file);
         if (mat != null)
            src.add(mat);
      }
      keypoints = new ArrayList<>();
      decryptions = new ArrayList<>();
      List<Pair<Integer, Integer>> cut = new ArrayList<>();
      final List<Mat> srcScaleAndGray = scaleAndGray(src);
      latch = new CountDownLatch(src.size());
      for (int i = 0; i < src.size(); i++) {
         final int finalI = i;
         Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
               keypoints.add(null);
               decryptions.add(null);
               detectAndCompute(srcScaleAndGray.get(finalI), finalI);
            }
         });
         thread.start();
      }
      await();
      if (selectedPaths.size() < 2) {
         view.disableButtonStitch();
         return;
      }
      int height = srcScaleAndGray.get(0)
                                  .rows();
      cut.add(null);
      cut.set(0, new Pair<Integer, Integer>(0, 0));

      for (int i = 1; i < src.size(); i++) {
         Pair<Integer, Integer> cutObjectAndScene = getPairCut(decryptions.get(i - 1), keypoints.get(i - 1), decryptions.get(i), keypoints.get(i));
         cut.set(i - 1, new Pair<>(cut.get(i - 1).first, cutObjectAndScene.first));
         cut.add(new Pair<>(cutObjectAndScene.second, height));
      }
      cropImg(cut, src);
      Mat res = stitchImagesVertical(src);
      String[] part = selectedPaths.get(0)
                                   .split("/");
      StringBuilder fileName = new StringBuilder();
      for (int i = 1; i < part.length - 1; i++) {
         fileName.append(part[i]);
         fileName.append("/");
      }
      fileName.append("Result.jpg");
      Imgcodecs.imwrite(fileName.toString(), res);
      sendLocalPathMessage(fileName.toString());
   }

   private void await() {
      try {
         latch.await();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   private List<Mat> scaleAndGray(List<Mat> src) {
      List<Mat> res = new ArrayList<>();
      for (int i = 0; i < src.size(); i++) {
         Mat img = src.get(i);
         float scale = (float) minWidth / img.cols();
         Size size = new Size(img.cols() * scale, img.rows() * scale);
         Imgproc.resize(img, img, size);

         size.set(new double[]{img.cols() * ratio_scale, img.rows() * ratio_scale});
         Mat tmp = new Mat();
         Imgproc.cvtColor(img, tmp, Imgproc.COLOR_BGR2GRAY);
         Imgproc.resize(tmp, tmp, size);
         res.add(tmp);
      }
      return res;
   }
   private void cropImg(List<Pair<Integer, Integer>> row, List<Mat> src) {
      for (int i = 0; i < src.size(); i++) {
         Mat img = src.get(i);
         int top = (int) (row.get(i).first / ratio_scale);
         int bottom = (int) ((row.get(i).second - row.get(i).first) / ratio_scale);
         int height = src.get(i)
                         .rows();
         int width = src.get(i)
                        .cols();
         if (top + bottom > height)
            bottom = height - top;
         Rect roi = new Rect(0, top, width, bottom);
         src.set(i, new Mat(img, roi));
      }
   }

   private Mat stitchImagesVertical(List<Mat> src) {
      Mat result = new Mat();
      Core.vconcat(src, result);
      return result;
   }

   private void detectAndCompute(Mat imgSrc, int index) {
      FastFeatureDetector detector = FastFeatureDetector.create();
      ORB extractor = ORB.create();
      Mat decryption = new Mat();
      MatOfKeyPoint keypoint = new MatOfKeyPoint();
      detector.detect(imgSrc, keypoint);
      extractor.compute(imgSrc, keypoint, decryption);
      List<KeyPoint> keyPointList = keypoint.toList();
      synchronized (this) {
         keypoints.set(index, keyPointList);
         decryptions.set(index, decryption);
         latch.countDown();
      }

   }
   Pair<Integer, Integer> getPairCut(Mat descriptors_object, List<KeyPoint> keypoints_object, Mat descriptors_scene, List<KeyPoint> keypoints_scene) {
      BFMatcher matcher = BFMatcher.create();
      MatOfDMatch matches;
      matches = new MatOfDMatch();
      HashMap<Integer, Integer> parallel = new HashMap<>();
      matcher.match(descriptors_object, descriptors_scene, matches);
      List<DMatch> good_matches, dMatchList = matches.toList();
      good_matches = new ArrayList<>();
      int maxFrequency, rowChoiceObject, rowChoiceScene;
      maxFrequency = rowChoiceObject = rowChoiceScene = 0;

      int sub;
      for (DMatch match : dMatchList) {
         if ((keypoints_object.get(match.queryIdx).pt.x - keypoints_scene.get(match.trainIdx).pt.x) <= DEVIATION_X) {
            sub = (int) (keypoints_object.get(match.queryIdx).pt.y - keypoints_scene.get(match.trainIdx).pt.y);
            //sub += (5 - sub % 5);
            if (sub > DEVIATION_Y) {
               int count;
               if (!parallel.containsKey(sub))
                  count = 1;
               else {
                  count = parallel.get(sub);
                  count++;
               }
               parallel.put(sub, count);
               maxFrequency = Math.max(count, maxFrequency);
               good_matches.add(match);
            }
         }
      }
      int flag, count;
      flag = 0;
      count = 0;
      for (DMatch match : good_matches) {
         sub = (int) (keypoints_object.get(match.queryIdx).pt.y - keypoints_scene.get(match.trainIdx).pt.y);
         if (parallel.get(sub) == maxFrequency) {
            if (flag == 0) {
               flag = sub;
               count++;
            } else if (flag == sub)
               count++;
            rowChoiceObject = (int) keypoints_object.get(match.queryIdx).pt.y;
            rowChoiceScene = (int) keypoints_scene.get(match.trainIdx).pt.y;
         }
      }
      if (count < MIN_MATCH)
         return new Pair<>(0, 0);
      return new Pair<>(rowChoiceObject, rowChoiceScene);
   }

   @Override
   public void readSrc(final List<String> imgPaths) {
      selectedPaths = imgPaths;
      long start = System.currentTimeMillis();
      for (String file : selectedPaths) {
         if (hashMapSrc.containsKey(file))
            continue;
         Mat img = Imgcodecs.imread(file);
         hashMapSrc.put(file, img);
         minWidth = Math.min(minWidth, img.cols());
         ratio_scale = (float) 480 / minWidth;
      }
      long end = System.currentTimeMillis();
      Log.d(TAG, "readSrc: " + (end - start));
      checkStitch();
   }

   @Override
   public void checkStitch() {
      List<Mat> src = new ArrayList<>();
      for (int i = 0; i < selectedPaths.size(); i++) {
         String currentFile = selectedPaths.get(i);
         if (i < selectedPaths.size() - 1) {
            String nextFile = selectedPaths.get(i + 1);
            if (!checkCache(currentFile, nextFile))
               sendSuggest(false);
         }
         Mat mat = hashMapSrc.get(currentFile);
         if (mat != null)
            src.add(mat);
      }

      keypoints = new ArrayList<>();
      decryptions = new ArrayList<>();
      List<Pair<Integer, Integer>> cut = new ArrayList<>();
      final List<Mat> srcScaleAndGray = scaleAndGray(src);
      latch = new CountDownLatch(src.size());
      for (int i = 0; i < src.size(); i++) {
         final int finalI = i;
         Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
               keypoints.add(null);
               decryptions.add(null);
               detectAndCompute(srcScaleAndGray.get(finalI), finalI);
            }
         });
         thread.start();
      }
      await();
      if (selectedPaths.size() < 2) {
         view.disableButtonStitch();
         return;
      }
      int height = srcScaleAndGray.get(0)
                                  .rows();
      cut.add(null);
      cut.set(0, new Pair<>(0, 0));

      for (int i = 1; i < src.size(); i++) {
         Pair<Integer, Integer> cutObjectAndScene = getPairCut(decryptions.get(i - 1), keypoints.get(i - 1), decryptions.get(i), keypoints.get(i));
         if (cutObjectAndScene.first == 0 || cutObjectAndScene.second == 0) {
            sendSuggest(false);
            cache.put(selectedPaths.get(i - 1) + selectedPaths.get(i), false);
            return;
         }
         cache.put(selectedPaths.get(i - 1) + selectedPaths.get(i), true);
         cut.set(i - 1, new Pair<Integer, Integer>(cut.get(i - 1).first, cutObjectAndScene.first));
         cut.add(new Pair<Integer, Integer>(cutObjectAndScene.second, height));
      }
      sendSuggest(true);
   }

   private boolean checkCache(String currentFile, String nextFile) {
      if (cache.containsKey(currentFile + nextFile))
         if (!cache.get(currentFile + nextFile)) {
            sendSuggest(false);
            return false;
         } else
            return true;
      if (cache.containsKey(nextFile + currentFile))
         if (!cache.get(nextFile + currentFile)) {
            sendSuggest(false);
            return false;
         }

      return true;
   }
   private void sendSuggest(boolean b) {
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

}
