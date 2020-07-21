package com.zhihu.matisse;

import java.util.List;

public interface StitchImgUseCase {
   interface Presenter {


      void onUpdateSelectedPaths(List<String> selectedPaths);
   }

   interface View {
      void enableButtonStitch();

      void disableButtonStitch();

      void showProgress();

      void hideProgress();

      void returnFileName(String obj);
   }
}
