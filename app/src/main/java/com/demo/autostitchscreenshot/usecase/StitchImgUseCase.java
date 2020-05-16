package com.demo.autostitchscreenshot.usecase;

import com.demo.autostitchscreenshot.IBasePresenter;
import com.demo.autostitchscreenshot.IBaseView;

public interface StitchImgUseCase {
    interface Presenter extends IBasePresenter {

    }

    interface View extends IBaseView<Presenter> {

    }
}
