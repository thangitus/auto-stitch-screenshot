package com.demo.autostitchscreenshot.view.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.demo.autostitchscreenshot.R;
import com.demo.autostitchscreenshot.utils.Callback;
import com.demo.autostitchscreenshot.utils.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InputScreenshotAdapter extends RecyclerView.Adapter {

   Context context;
   private List<String> imgPaths;
   private Callback.WithPair<String, Integer> callback;

   public InputScreenshotAdapter(Context context, Callback.WithPair<String, Integer> callback) {
      this.context = context;
      this.callback = callback;
      imgPaths = new ArrayList<>();
   }
   public void removeItem(int index){
      imgPaths.remove(index);
      notifyItemRemoved(index);
      notifyItemRangeChanged(index, getItemCount());
   }
   public boolean isEmptyData(){
      return imgPaths.isEmpty();
   }
   public void setImgPaths(List<String> imgPaths) {
      this.imgPaths.addAll(imgPaths);
   }
   @NonNull
   @Override
   public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      View view = inflater.inflate(R.layout.item_screenshot, parent, false);
      return new InputScreenshotViewHolder(view);
   }

   @Override
   public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
      String item = imgPaths.get(position);
      ((InputScreenshotViewHolder) holder).bind(item, position);
   }

   @Override
   public int getItemCount() {
      if (imgPaths == null)
         return 0;
      return imgPaths.size();
   }

   private class InputScreenshotViewHolder extends RecyclerView.ViewHolder {

      private ImageView btnRemove, screenshot, btnReorder;

      InputScreenshotViewHolder(@NonNull View itemView) {
         super(itemView);
         btnRemove = itemView.findViewById(R.id.btn_remove);
         screenshot = itemView.findViewById(R.id.screenshot);
         btnReorder = itemView.findViewById(R.id.btn_reorder);
      }

      void bind(String imgPath, final int index) {
         if(imgPath!=null) {
            File file = new File(imgPath);
            if (file.exists()) {
               Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
               screenshot.setImageBitmap(bitmap);
            }
         }
         else
            Log.d("path", "unknown");

         btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               callback.run(Constants.REMOVE, index);
            }
         });
      }
   }
}
