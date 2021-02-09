package com.example.findinimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

public class CustomAdap extends ArrayAdapter<String> {
    Context context;
    ArrayList<String> results;
    LayoutInflater inflater;
    public CustomAdap(Context applicationContext, int textViewResourceId, ArrayList<String> selected_images) {
        super(applicationContext, textViewResourceId, selected_images);
        this.context = applicationContext;
        this.results = selected_images;
    }
    @Override
    public int getCount() {
        return results.size();
    }
    @Override
    public long getItemId(int i) {
        return 0;
    }
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        int cellWidth = 350;
        int cellHeight = 500;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.activity_select_imageview, null);
        ImageView image = view.findViewById(R.id.selectedImage); // get the reference of ImageView
        Bitmap mBitmap = BitmapFactory.decodeFile(results.get(i));
        // Set height and width constraints for the image view
        image.setLayoutParams(new RelativeLayout.LayoutParams(cellWidth, cellHeight));
        // Set Padding for images
        image.setPadding(1, 1, 1, 1);
        image.setImageBitmap(mBitmap); // set logo images

        return view;
    }
}