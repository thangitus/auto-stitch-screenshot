package com.zhihu.matisse;

import java.util.List;

public interface StitchImgUseCase {
   interface Presenter {

      void onUpdateSelectedPaths(List<String> selectedPaths);

      void stitchImages();
   }

   interface View {
      void showButtonStitch();

      void disableButtonStitch();
      void toast(String msg);

      void enableButtonStitch();

      void hideButtonStitch();

      void showProgress();

      void hideProgress();

      void returnFileName(String obj);
   }
}
