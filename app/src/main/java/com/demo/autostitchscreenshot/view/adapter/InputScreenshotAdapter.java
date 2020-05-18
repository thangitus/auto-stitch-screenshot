package com.demo.autostitchscreenshot.view.adapter;

import android.content.Context;
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
import java.util.Collections;
import java.util.List;

public class InputScreenshotAdapter extends RecyclerView.Adapter {

   private Context context;
   private List<String> data;
   private Callback.WithPair<String, Integer> callback;

   public InputScreenshotAdapter(Context context, Callback.WithPair<String, Integer> callback) {
      this.context = context;
      this.callback = callback;
      data = new ArrayList<>();
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
      String item = data.get(position);
      ((InputScreenshotViewHolder) holder).bind(item, position);
   }

   @Override
   public int getItemCount() {
      if (data == null)
         return 0;
      return data.size();
   }

   public void onRowMoved(int fromPos, int toPos){
      if (fromPos<toPos){
         for(int i = fromPos; i < toPos; i++){
            Collections.swap(data, i, i+1);
         }
      }
      else{
         for(int i = fromPos; i > toPos; i--){
            Collections.swap(data, i, i-1);
         }
      }
      notifyItemMoved(fromPos, toPos);
   }

   public void removeItem(int index){
      if(index<data.size()) {
         data.remove(index);
         notifyItemRemoved(index);
         notifyItemRangeChanged(index, getItemCount());
      }

   }

   public boolean isEmptyData(){
      return data.isEmpty();
   }

   public void addData(List<String> items) {
      int positionStart = data.size() + 1;
      this.data.addAll(items);
      notifyItemRangeInserted(positionStart, items.size());
   }

   private class InputScreenshotViewHolder extends RecyclerView.ViewHolder {

      private ImageView btnRemove, screenshot;

      InputScreenshotViewHolder(@NonNull View itemView) {
         super(itemView);
         btnRemove = itemView.findViewById(R.id.btn_remove);
         screenshot = itemView.findViewById(R.id.screenshot);
      }

      void bind(String imgPath, final int index) {
         if(imgPath!=null) {
            File file = new File(imgPath);
            if (file.exists()) {
               Glide.with(context).load(file).into(screenshot);
            }
         }

         btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               callback.run(Constants.REMOVE, index);
            }
         });
      }
   }
}
