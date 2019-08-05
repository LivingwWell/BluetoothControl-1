package com.example.bluetoothcontrol;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class receiverActivity extends AppCompatActivity {
    TextView textView;
    Button button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.itent1);
        textView=(TextView)findViewById(R.id.txt);
        // button=(Button)findViewById(R.id.b2);
        Message();


    }
    private void Message() {
        Intent intent = getIntent();
        //把传送进来的String类型的Message的值赋给新的变量message
        String message = intent.getStringExtra("EXTRA_MESSAGE");
        //把布局文件中的文本框和textview链接起来
        TextView textView = (TextView) findViewById(R.id.txt);
        //在textview中显示出来message
        textView.setText(message);
    }}

