package com.demo.autostitchscreenshot.utils;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpacingItemDecoration extends RecyclerView.ItemDecoration {
   private int mSpaceHeight;

   public SpacingItemDecoration(int mSpaceHeight) {this.mSpaceHeight = mSpaceHeight;}

   @Override
   public void getItemOffsets(
           @NonNull Rect outRect,
           @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
      super.getItemOffsets(outRect, view, parent, state);
      boolean isLastItem = parent.getChildAdapterPosition(view) == parent.getAdapter()
                                                                         .getItemCount() - 1;

      if (!isLastItem)
         outRect.bottom = mSpaceHeight;
      else
         outRect.bottom = mSpaceHeight * 2;

      boolean isFirstItem = parent.getChildAdapterPosition(view) == 0;
      if (isFirstItem)
         outRect.top = mSpaceHeight * 2;
   }
}
