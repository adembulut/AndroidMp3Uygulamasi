package com.dkadem.bulut.downsong;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class SplashActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if(Anasayfa.InternetVarMi(SplashActivity.this)){
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        startActivity(new Intent(SplashActivity.this, Anasayfa.class));
                    }
                }
            });

            t.start();
        }else{
            Toast.makeText(SplashActivity.this, "Uygulama internet bağlantısı gerektirir", Toast.LENGTH_SHORT).show();
        }



    }

}
