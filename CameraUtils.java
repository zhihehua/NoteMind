package com.example.NoteMind;

import android.app.Activity;
import android.content.Intent;
import android.provider.MediaStore;
import android.widget.Toast;

public class CameraUtils {

    // 相机请求码
    public static final int REQUEST_CAMERA = 100;
    // 相册请求码
    public static final int REQUEST_GALLERY = 101;

    private final Activity activity;
    private OnCameraResultListener listener;

    // 构造方法
    public CameraUtils(Activity activity) {
        this.activity = activity;
    }

    // 设置回调（拍照/选图完成后通知外面）
    public void setOnCameraResultListener(OnCameraResultListener listener) {
        this.listener = listener;
    }

    // 打开相机
    public void openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            activity.startActivityForResult(intent, REQUEST_CAMERA);
        } catch (Exception e) {
            Toast.makeText(activity, "相机打开失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 打开相册
    public void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activity.startActivityForResult(intent, REQUEST_GALLERY);
        } catch (Exception e) {
            Toast.makeText(activity, "相册打开失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 必须在 Activity 的 onActivityResult 中调用
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) return;

        try {
            if (requestCode == REQUEST_CAMERA) {
                // 拍照返回
                android.graphics.Bitmap bitmap = (android.graphics.Bitmap) data.getExtras().get("data");
                if (listener != null) listener.onCameraSuccess(bitmap);

            } else if (requestCode == REQUEST_GALLERY) {
                // 相册返回
                android.graphics.Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), data.getData());
                if (listener != null) listener.onGallerySuccess(bitmap);
            }
        } catch (Exception e) {
            if (listener != null) listener.onFail(e.getMessage());
        }
    }

    // 回调接口
    public interface OnCameraResultListener {
        void onCameraSuccess(android.graphics.Bitmap bitmap);
        void onGallerySuccess(android.graphics.Bitmap bitmap);
        void onFail(String error);
    }
}