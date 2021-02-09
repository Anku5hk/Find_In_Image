package com.example.findinimage;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.StrictMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class SelectedImageView extends AppCompatActivity {
    ImageView selectedImage;
    Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_imageview);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        selectedImage = findViewById(R.id.selectedImage); // init a ImageView
        Intent intent = getIntent(); // get Intent which we set from Previous Activity
        String img_path = intent.getStringExtra("image");
        Uri imageUri = Uri.fromFile(new File(img_path));
        Bitmap mBitmap = BitmapFactory.decodeFile(img_path);
        selectedImage.setImageBitmap(mBitmap); // get image from Intent and set it in ImageView
        button = findViewById(R.id.btn);
        selectedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button.getVisibility() == View.VISIBLE){
                    button.setVisibility(View.INVISIBLE);
                }
                else{
                    button.setVisibility(View.VISIBLE);
                }
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent share_intent = new Intent(Intent.ACTION_SEND);
                share_intent.setType("image/*");
                share_intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                startActivity(Intent.createChooser(share_intent , "Share"));
            }
        });
    }
}

