package com.demo.autostitchscreenshot.view.screen;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.demo.autostitchscreenshot.databinding.ActivityResultBinding;

public class ResultActivity extends AppCompatActivity {
   private ActivityResultBinding binding;
   @Override
   public void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      binding = ActivityResultBinding.inflate(getLayoutInflater());
      Intent intent=getIntent();
      byte[] bytes=intent.getByteArrayExtra("RESULT");
      Bitmap result= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
      binding.resultImg.setImageBitmap(result);
   }
}
