package com.example.NoteMind;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class DetailActivity extends AppCompatActivity {

    private EditText et_question, et_answer, et_tag, et_category, et_user_note;
    private GridLayout gl_photos;
    private QuestionDao dao;
    private int currentId = -1;
    private ArrayList<String> photoPathList = new ArrayList<>();
    private String currentPhotoPath;

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        et_question = findViewById(R.id.et_question);
        et_answer = findViewById(R.id.et_answer);
        et_tag = findViewById(R.id.et_tag);
        et_category = findViewById(R.id.et_category);
        et_user_note = findViewById(R.id.et_user_note);
        gl_photos = findViewById(R.id.gl_photos);

        dao = new QuestionDao(this);

        findViewById(R.id.btn_photo_note).setOnClickListener(v -> takePhoto());
        findViewById(R.id.btn_gallery_note).setOnClickListener(v -> openGallery());

        findViewById(R.id.btn_update).setOnClickListener(v -> updateNote());
        findViewById(R.id.btn_delete).setOnClickListener(v -> deleteNote());

        loadData();
    }

    // ====================== 拍照 ======================
    private void takePhoto() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this, "com.example.doubao.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            }
        }
    }

    // ====================== 相册 ======================
    private void openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                photoPathList.add(currentPhotoPath);
                addImageToView(currentPhotoPath);
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri uri = data.getData();
                String path = getImagePathFromUri(uri);
                if (path != null) {
                    photoPathList.add(path);
                    addImageToView(path);
                }
            }
        }
    }

    // ====================== 图片生成工具 ======================
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private String getImagePathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return null;
    }

    // ====================== ✅ 核心：添加图片 + 点击查看 + 长按确认删除弹窗 ======================
    private void addImageToView(String path) {
        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new ViewGroup.LayoutParams(240, 240));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setPadding(8, 8, 8, 8);
        iv.setImageURI(Uri.fromFile(new File(path)));

        // 点击查看全屏
        iv.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, ImagePreviewActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
        });

        // 长按删除 → 弹窗确认
        iv.setOnLongClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要删除这张图片吗？")
                    .setPositiveButton("确定删除", (dialog, which) -> {
                        int index = photoPathList.indexOf(path);
                        if (index != -1) {
                            photoPathList.remove(index);
                            gl_photos.removeView(iv);
                            Toast.makeText(DetailActivity.this, "图片已删除", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        gl_photos.addView(iv);
    }

    // ====================== 加载 / 保存 / 删除 ======================
    private void loadData() {
        currentId = getIntent().getIntExtra("id", -1);
        if (currentId == -1) return;

        QuestionNote note = dao.queryById(currentId);
        if (note == null) return;

        et_question.setText(note.getQuestion());
        et_answer.setText(note.getAnswer());
        et_tag.setText(note.getTag());
        et_category.setText(note.getCategory());
        et_user_note.setText(note.getUserNote());

        String paths = note.getPhotoPaths();
        if (paths != null && !paths.isEmpty()) {
            String[] arr = paths.split(";");
            photoPathList = new ArrayList<>(Arrays.asList(arr));
            for (String p : arr) addImageToView(p);
        }
    }

    private void updateNote() {
        String question = et_question.getText().toString();
        String answer = et_answer.getText().toString();
        String tag = et_tag.getText().toString();
        String category = et_category.getText().toString();
        String userNote = et_user_note.getText().toString();

        StringBuilder sb = new StringBuilder();
        for (String s : photoPathList) sb.append(s).append(";");
        String photoPaths = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";

        QuestionNote note = new QuestionNote(currentId, question, answer, tag, userNote, category, 0, photoPaths);
        dao.updateNote(note);
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void deleteNote() {
        dao.deleteById(currentId);
        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dao.close();
    }
}