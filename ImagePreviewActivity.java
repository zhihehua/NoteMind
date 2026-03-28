package com.example.NoteMind;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        ImageView iv_preview = findViewById(R.id.iv_preview);
        String path = getIntent().getStringExtra("path");

        // 纯安卓原生加载图片，不用任何第三方库
        iv_preview.setImageURI(android.net.Uri.fromFile(new File(path)));
    }
}