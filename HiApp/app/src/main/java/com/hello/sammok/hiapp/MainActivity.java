package com.hello.sammok.hiapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "inside print...");
    }

    private static final String TAG = "hello";

    public void printHi(View view) {
        Intent intent = new Intent(this, Main2Activity.class);
        startActivity(intent);
    }

}

