package com.demo.autostitchscreenshot.usecase;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import static androidx.core.util.Preconditions.checkNotNull;
import static org.opencv.core.CvType.CV_32FC1;

public class StitchImgPresenter implements StitchImgUseCase.Presenter {

    private StitchImgUseCase.View view;

    @SuppressLint("RestrictedApi")
    public StitchImgPresenter(StitchImgUseCase.View view){
        this.view = checkNotNull(view);
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public void stitchImages(List<String> imgPaths){
        view.onError("This feature is in develop!");
    }

    private Mat convertBitmapToMat(Bitmap img){
        Mat mat = new Mat();
        Utils.bitmapToMat(img, mat);
        return mat;
    }

    private Mat convertToGrayscale(Mat mat){
        Mat result = new Mat();
        Imgproc.cvtColor(mat, result, Imgproc.COLOR_BayerRG2GRAY);
        return result;
    }

    private Bitmap findMatching(Mat source, Mat template){
        Mat result;
        int resultCols = source.cols() - template.cols() + 1;
        int resultRows = source.rows() - template.rows() + 1;
        result = new Mat(resultCols, resultRows, CV_32FC1);
        Imgproc.matchTemplate(source, template, result, Imgproc.TM_SQDIFF);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
        Point matchLocation;
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        matchLocation = mmr.minLoc;
        Mat imgResult= new Mat();
        Imgproc.rectangle(imgResult, matchLocation, new Point(matchLocation.x + template.cols(), matchLocation.y + template.rows()),
                new Scalar(0, 0, 0), 2, 8, 0);

        Bitmap bitmap = Bitmap.createBitmap(imgResult.cols(), imgResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgResult, bitmap);
        return bitmap;
    }
}
