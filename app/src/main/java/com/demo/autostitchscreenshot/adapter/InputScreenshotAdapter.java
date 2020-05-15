package com.demo.autostitchscreenshot.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.autostitchscreenshot.R;
import com.demo.autostitchscreenshot.utils.Callback;
import com.demo.autostitchscreenshot.utils.Constants;

import java.util.List;

public class InputScreenshotAdapter extends RecyclerView.Adapter {

    private List<Bitmap> data;
    private Callback.WithPair<String, Integer> callback;

    public InputScreenshotAdapter(List<Bitmap> screenshots, Callback.WithPair<String, Integer> onItemClick){
        this.data = screenshots;
        this.callback = onItemClick;
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
        Bitmap item = data.get(position);
        ((InputScreenshotViewHolder)holder).bind(item, position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private class InputScreenshotViewHolder extends RecyclerView.ViewHolder{

        private ImageView btnRemove, screenshot, btnReorder;

        InputScreenshotViewHolder(@NonNull View itemView) {
            super(itemView);
            btnRemove = itemView.findViewById(R.id.btn_remove);
            screenshot = itemView.findViewById(R.id.screenshot);
            btnReorder = itemView.findViewById(R.id.btn_reorder);
        }

        void bind(Bitmap item, final int index){

            screenshot.setImageBitmap(item);

            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.run(Constants.REMOVE, index);
                }
            });
        }
    }
}
