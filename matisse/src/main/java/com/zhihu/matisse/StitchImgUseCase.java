package com.zhihu.matisse;

import java.util.List;

public interface StitchImgUseCase {
   interface Presenter {

      void stitchImages();

      void readSrc(List<String> imgPaths);

      void checkStitch();
   }

   interface View {
      void enableButtonStitch();

      void disableButtonStitch();

      void showProgress();

      void hideProgress();

   }
}
