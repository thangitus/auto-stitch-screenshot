/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.IncapableCause;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter;
import com.zhihu.matisse.internal.ui.widget.CheckRadioView;
import com.zhihu.matisse.internal.ui.widget.CheckView;
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils;
import com.zhihu.matisse.internal.utils.Platform;
import com.zhihu.matisse.listener.OnFragmentInteractionListener;

public abstract class BasePreviewActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener, OnFragmentInteractionListener {

   public static final String EXTRA_DEFAULT_BUNDLE = "extra_default_bundle";
   public static final String EXTRA_RESULT_BUNDLE = "extra_result_bundle";
   public static final String EXTRA_RESULT_APPLY = "extra_result_apply";
   public static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
   public static final String CHECK_STATE = "checkState";

   protected final SelectedItemCollection mSelectedCollection = new SelectedItemCollection(this);
   protected SelectionSpec mSpec;
   protected ViewPager mPager;

   protected PreviewPagerAdapter mAdapter;

   protected CheckView mCheckView;

   protected int mPreviousPos = -1;
   protected boolean mOriginalEnable;
   private CheckRadioView mOriginal;
   private FrameLayout mTopToolbar;
   private boolean mIsToolbarHide = false;

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      setTheme(SelectionSpec.getInstance().themeId);
      super.onCreate(savedInstanceState);
      if (!SelectionSpec.getInstance().hasInited) {
         setResult(RESULT_CANCELED);
         finish();
         return;
      }
      setContentView(R.layout.activity_media_preview);
      if (Platform.hasKitKat()) {
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      }

      mSpec = SelectionSpec.getInstance();
      if (mSpec.needOrientationRestriction()) {
         setRequestedOrientation(mSpec.orientation);
      }

      if (savedInstanceState == null) {
         mSelectedCollection.onCreate(getIntent().getBundleExtra(EXTRA_DEFAULT_BUNDLE));
         mOriginalEnable = getIntent().getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false);
      } else {
         mSelectedCollection.onCreate(savedInstanceState);
         mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
      }

      mPager = (ViewPager) findViewById(R.id.pager);
      mPager.addOnPageChangeListener(this);
      mAdapter = new PreviewPagerAdapter(getSupportFragmentManager(), null);
      mPager.setAdapter(mAdapter);
      mCheckView = (CheckView) findViewById(R.id.check_view);
      mCheckView.setCountable(mSpec.countable);
      mTopToolbar = findViewById(R.id.top_toolbar);

      mCheckView.setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            Item item = mAdapter.getMediaItem(mPager.getCurrentItem());
            if (mSelectedCollection.isSelected(item)) {
               mSelectedCollection.remove(item);
               if (mSpec.countable) {
                  mCheckView.setCheckedNum(CheckView.UNCHECKED);
               } else {
                  mCheckView.setChecked(false);
               }
            } else {
               if (assertAddSelection(item)) {
                  mSelectedCollection.add(item);
                  if (mSpec.countable) {
                     mCheckView.setCheckedNum(mSelectedCollection.checkedNumOf(item));
                  } else {
                     mCheckView.setChecked(true);
                  }
               }
            }
            if (mSpec.onSelectedListener != null) {
               mSpec.onSelectedListener.onSelected(mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
            }
         }
      });
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      mSelectedCollection.onSaveInstanceState(outState);
      outState.putBoolean("checkState", mOriginalEnable);
      super.onSaveInstanceState(outState);
   }

   @Override
   public void onBackPressed() {
      sendBackResult(false);
      super.onBackPressed();
   }

   @Override
   public void onClick() {
      if (!mSpec.autoHideToobar) {
         return;
      }
      mIsToolbarHide = !mIsToolbarHide;

   }

   @Override
   public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

   }

   @Override
   public void onPageSelected(int position) {
      PreviewPagerAdapter adapter = (PreviewPagerAdapter) mPager.getAdapter();
      if (mPreviousPos != -1 && mPreviousPos != position) {
         ((PreviewItemFragment) adapter.instantiateItem(mPager, mPreviousPos)).resetView();

         Item item = adapter.getMediaItem(position);
         if (mSpec.countable) {
            int checkedNum = mSelectedCollection.checkedNumOf(item);
            mCheckView.setCheckedNum(checkedNum);
            if (checkedNum > 0) {
               mCheckView.setEnabled(true);
            } else {
               mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
            }
         } else {
            boolean checked = mSelectedCollection.isSelected(item);
            mCheckView.setChecked(checked);
            if (checked) {
               mCheckView.setEnabled(true);
            } else {
               mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
            }
         }
      }
      mPreviousPos = position;
   }

   @Override
   public void onPageScrollStateChanged(int state) {

   }

   private int countOverMaxSize() {
      int count = 0;
      int selectedCount = mSelectedCollection.count();
      for (int i = 0; i < selectedCount; i++) {
         Item item = mSelectedCollection.asList()
                                        .get(i);
         if (item.isImage()) {
            float size = PhotoMetadataUtils.getSizeInMB(item.size);
            if (size > mSpec.originalMaxSize) {
               count++;
            }
         }
      }
      return count;
   }

   protected void sendBackResult(boolean apply) {
      Intent intent = new Intent();
      intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle());
      intent.putExtra(EXTRA_RESULT_APPLY, apply);
      intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
      setResult(Activity.RESULT_OK, intent);
   }

   private boolean assertAddSelection(Item item) {
      IncapableCause cause = mSelectedCollection.isAcceptable(item);
      IncapableCause.handleCause(this, cause);
      return cause == null;
   }
}
