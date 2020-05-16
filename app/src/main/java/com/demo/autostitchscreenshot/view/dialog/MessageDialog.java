package com.demo.autostitchscreenshot.view.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.demo.autostitchscreenshot.R;
import com.demo.autostitchscreenshot.utils.Callback;

public class MessageDialog extends Dialog {

    private TextView title, message, btnConfirm;
    private Activity mOwnerActivity;
    private Callback confirmListener;

    public MessageDialog(@NonNull Context context) {
        super(context);
        mOwnerActivity = (context instanceof Activity) ? (Activity) context : null;
        if(mOwnerActivity!=null)
            setOwnerActivity(mOwnerActivity);
        setCancelable(true);
        setCanceledOnTouchOutside(false);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mOwnerActivity = getOwnerActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.message_dialog_layout);
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.windowAnimations = R.style.CoreTheme_AnimDialog_Fade;
            params.width = mOwnerActivity.getResources().getDisplayMetrics().widthPixels * 8 / 10;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }

        title = findViewById(R.id.text_title);
        message = findViewById(R.id.text_msg);
        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmListener.run();
            }
        });
    }

    public void show(String textTitle, String textMsg, String textConfirm, Callback onConfirmClickListener) {
        super.show();
        title.setText(textTitle);
        message.setText(textMsg);
        btnConfirm.setText(textConfirm);
        confirmListener = onConfirmClickListener;
    }

}
