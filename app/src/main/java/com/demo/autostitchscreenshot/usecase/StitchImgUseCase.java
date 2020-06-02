package com.demo.autostitchscreenshot.usecase;

import android.graphics.Bitmap;

import com.demo.autostitchscreenshot.IBasePresenter;
import com.demo.autostitchscreenshot.IBaseView;

import java.util.List;

public interface StitchImgUseCase {
    interface Presenter extends IBasePresenter {
        void stitchImages();
        void readSrc(List<String> imgPaths);
    }

    interface View extends IBaseView<Presenter> {
        void onStitchImageSuccess(Bitmap img);
        void onError(String msg);
        void showResult(Bitmap bitmap);
        void showProgress();
        void hideProgress();
    }
}
