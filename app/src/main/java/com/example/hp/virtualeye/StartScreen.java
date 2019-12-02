package com.example.hp.virtualeye;

import android.content.Intent;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class StartScreen extends AppCompatActivity {

    Button objectTagging;
    Button imageProcessing;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);
        objectTagging=findViewById(R.id.object_tagging);
        imageProcessing=findViewById(R.id.image_processing);
        objectTagging.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(StartScreen.this,MainActivity.class);
                startActivity(intent);
            }
        });
        imageProcessing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent=new Intent(StartScreen.this,Image_processing.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
