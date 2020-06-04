package com.demo.autostitchscreenshot.usecase;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import androidx.core.util.Pair;

import org.opencv.android.Utils;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static androidx.core.util.Preconditions.checkNotNull;

public class StitchImgPresenter implements StitchImgUseCase.Presenter {
   private static final String TAG = "StitchImgPresenter";
   private static final float RATIO = 2;
   private static final float DEVIATION_X = 2;
   private static final float DEVIATION_Y = 100;
   private static final int DONE = 1010;

   private CountDownLatch latch;
   private StitchImgUseCase.View view;
   private List<Mat> src;
   private float ratio_scale = 1;
   private List<List<KeyPoint>> keypoints;
   private List<Mat> decryptions;
   private Handler handler;
   @SuppressLint({"RestrictedApi", "HandlerLeak"})
   public StitchImgPresenter(final StitchImgUseCase.View view) {
      this.view = checkNotNull(view);
      handler = new Handler() {
         @Override
         public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == DONE) {
               Bitmap result = (Bitmap) msg.obj;
               view.showResult(result);
               view.hideProgress();
            }
         }
      };
   }

   @Override
   public void subscribe() {

   }

   @Override
   public void unsubscribe() {

   }
   @Override
   public void stitchImages() {
      await();
      view.showProgress();
      new Runnable() {
         @Override
         public void run() {
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
            int height = srcScaleAndGray.get(0)
                                        .rows();
            cut.add(null);
            cut.set(0, new Pair<Integer, Integer>(0, 0));
            for (int i = 1; i < src.size(); i++) {
               Pair<Integer, Integer> cutObjectAndScene = getPairCut(decryptions.get(i - 1), keypoints.get(i - 1), decryptions.get(i), keypoints.get(i));
               cut.set(i - 1, new Pair<Integer, Integer>(cut.get(i - 1).first, cutObjectAndScene.first));
               cut.add(new Pair<Integer, Integer>(cutObjectAndScene.second, height));
            }
            cropImg(cut);
            Bitmap res = stitchImagesVertical(src);
            Message message = new Message();
            message.what = DONE;
            message.obj = res;
            handler.sendMessage(message);
         }
      }.run();
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
      for (Mat img : src) {
         Size size = new Size(img.cols() * ratio_scale, img.rows() * ratio_scale);
         Mat tmp = new Mat();
         Imgproc.cvtColor(img, tmp, Imgproc.COLOR_BGR2GRAY);
         Imgproc.resize(tmp, tmp, size);
         res.add(tmp);
      }
      return res;
   }
   private void cropImg(List<Pair<Integer, Integer>> row) {
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

   private Bitmap stitchImagesVertical(List<Mat> src) {
      Mat result = new Mat();
      Core.vconcat(src, result);
      Bitmap bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
      Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGB);
      Utils.matToBitmap(result, bitmap);
      return bitmap;
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
      for (DMatch match : good_matches) {
         sub = (int) (keypoints_object.get(match.queryIdx).pt.y - keypoints_scene.get(match.trainIdx).pt.y);
         //sub += (5 - sub % 5);
         if (parallel.get(sub) == maxFrequency) {
            rowChoiceObject = (int) keypoints_object.get(match.queryIdx).pt.y;
            rowChoiceScene = (int) keypoints_scene.get(match.trainIdx).pt.y;
         }
      }
      return new Pair<>(rowChoiceObject, rowChoiceScene);
   }

   @Override
   public void readSrc(final List<String> imgPaths) {
      src = new ArrayList<>();
      latch = new CountDownLatch(1);
      new Runnable() {
         @Override
         public void run() {
            for (String file : imgPaths) {
               Mat img = Imgcodecs.imread(file);
               src.add(img);
               if (ratio_scale == 1)
                  ratio_scale = (float) 480 / img.cols();
            }
            latch.countDown();
         }
      }.run();

   }

   @Override
   public void move(int fromPos, int toPos) {
      await();
      Collections.swap(src, fromPos, toPos);
   }
   @Override
   public void delete(int position) {
      await();
      src.remove(position);
   }
}
