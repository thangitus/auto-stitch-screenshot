package com.demo.autostitchscreenshot.view.screen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.demo.autostitchscreenshot.R;

public class SplashActivity extends AppCompatActivity implements Runnable{

    @Override
    public void onCreate(Bundle onSavedInstanceState){
        super.onCreate(onSavedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    public void onResume(){
        super.onResume();
        Handler handler = new Handler();
        handler.postDelayed(this, 2000);
    }

    @Override
    public void run(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        this.finish();
    }
}
