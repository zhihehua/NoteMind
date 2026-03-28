package com.example.NoteMind;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView; // 导入 WebView 用于调试和加速
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button btnTakePhoto, btnGallery, btnInputText, btnShowList, btnKnowledgeAtom;
    private ChipGroup chipGroupTags;
    private String currentSelectedTag = "";

    private CameraUtils cameraUtils;
    private QuestionDao questionDao;

    // 注意：请确保你的 API_KEY 已经正确填写
    private static final String API_KEY = "你的API_KEY";
    private static final String API_URL = "https://hello-occejwojlb.cn-hangzhou.fcapp.run";
    private static final String MODEL = "doubao-seed-1-8-251228";

    private AlertDialog loadingDialog;
    private static final int ALL_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- 核心修复：全局开启硬件加速 (解决原子不动的问题) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        questionDao = new QuestionDao(this);
        cameraUtils = new CameraUtils(this);

        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnGallery = findViewById(R.id.btn_gallery);
        btnInputText = findViewById(R.id.btn_input_text);
        btnShowList = findViewById(R.id.btn_show_list);
        btnKnowledgeAtom = findViewById(R.id.btn_knowledge_atom);
        chipGroupTags = findViewById(R.id.chip_group_tags);

        checkPermissions();
        refreshTagChips();

        btnTakePhoto.setOnClickListener(v -> cameraUtils.openCamera());
        btnGallery.setOnClickListener(v -> cameraUtils.openGallery());
        btnInputText.setOnClickListener(v -> showInputTextDialog());
        btnShowList.setOnClickListener(v -> startActivity(new Intent(this, NoteListActivity.class)));
        btnKnowledgeAtom.setOnClickListener(v -> showAtomTagDialog());

        cameraUtils.setOnCameraResultListener(new CameraUtils.OnCameraResultListener() {
            @Override
            public void onCameraSuccess(Bitmap bitmap) { requestAiOcr(bitmap); }
            @Override
            public void onGallerySuccess(Bitmap bitmap) { requestAiOcr(bitmap); }
            @Override
            public void onFail(String msg) {
                Toast.makeText(MainActivity.this, "操作失败: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshTagChips() {
        if (chipGroupTags == null) return;
        chipGroupTags.removeAllViews();

        // 1. 添加“+ 新标签”按钮
        Chip addBtn = new Chip(this);
        addBtn.setText("+ 新标签");
        addBtn.setChipIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_input_add));
        addBtn.setCheckable(false); // 明确不可选中
        addBtn.setOnClickListener(v -> showNewTagInput());
        chipGroupTags.addView(addBtn);

        // 2. 从数据库调取已有标签
        List<String> tags = questionDao.getAllUniqueTags();
        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            // 恢复之前选中的状态
            if (tag.equals(currentSelectedTag)) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentSelectedTag = tag;
                    // 单选逻辑：如果选中了这一个，确保其他的不被选中（可选）
                } else if (currentSelectedTag.equals(tag)) {
                    currentSelectedTag = "";
                }
            });
            chipGroupTags.addView(chip);
        }
    }

    private void showNewTagInput() {
        EditText et = new EditText(this);
        et.setHint("如：线性代数、美食...");
        new AlertDialog.Builder(this)
                .setTitle("定义新场景")
                .setView(et)
                .setPositiveButton("选定", (d, w) -> {
                    String val = et.getText().toString().trim();
                    if(!val.isEmpty()) {
                        currentSelectedTag = val;
                        // 刷新列表以显示新标签并默认选中
                        refreshTagChips();
                        Toast.makeText(this, "当前场景：" + val, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAtomTagDialog() {
        List<String> tags = questionDao.getAllUniqueTags();
        if (tags.isEmpty()) {
            Toast.makeText(this, "暂无数据，无法生成原子图谱", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] tagArray = tags.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("选择要探索的原子领域")
                .setItems(tagArray, (dialog, which) -> {
                    Intent intent = new Intent(this, AtomGraphActivity.class);
                    intent.putExtra("TARGET_TAG", tagArray[which]);
                    startActivity(intent);
                }).show();
    }

    private void showInputTextDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_single_input, null);
        EditText et = v.findViewById(R.id.et_single_question);

        if(!currentSelectedTag.isEmpty()) {
            et.setHint("针对 [" + currentSelectedTag + "] 提问...");
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        v.findViewById(R.id.btn_cancel_single).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_send_ai).setOnClickListener(view -> {
            String text = et.getText().toString().trim();
            if (!text.isEmpty()) {
                dialog.dismiss();
                // 包装标签逻辑：确保 AI 知道上下文
                String wrappedPrompt = currentSelectedTag.isEmpty() ? text :
                        "【场景标签：" + currentSelectedTag + "】用户问题：" + text;
                requestAiText(wrappedPrompt);
            }
        });
        dialog.show();
    }

    // --- 识别核心逻辑 ---
    private void requestAiOcr(Bitmap bitmap) {
        showLoading();
        new Thread(() -> {
            try {
                String base64 = bitmapToSmallBase64(bitmap);
                JSONObject json = new JSONObject();
                json.put("model", MODEL);
                JSONArray messages = new JSONArray();

                messages.put(new JSONObject().put("role", "system")
                        .put("content", "你是一个知识助手。请按格式输出：题目：xxx解答：xxx。禁止包含Markdown代码块或多余解释。"));

                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                JSONArray contentArr = new JSONArray();

                JSONObject textObj = new JSONObject();
                textObj.put("type", "text");
                textObj.put("text", currentSelectedTag.isEmpty() ? "识别图片中的题目并解答。" :
                        "【场景：" + currentSelectedTag + "】请分析图片中的知识点并解答。");

                contentArr.put(textObj);
                JSONObject imgObj = new JSONObject();
                imgObj.put("type", "image_url");
                imgObj.put("image_url", new JSONObject().put("url", "data:image/jpeg;base64," + base64));
                contentArr.put(imgObj);

                msg.put("content", contentArr);
                messages.put(msg);
                json.put("messages", messages);

                callApi(json.toString());
            } catch (Exception e) {
                handleError(e.getMessage());
            }
        }).start();
    }

    private void requestAiText(String text) {
        showLoading();
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("model", MODEL);
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system")
                        .put("content", "你是一个知识助手。请按格式输出：题目：xxx解答：xxx。"));

                messages.put(new JSONObject().put("role", "user").put("content", text));
                json.put("messages", messages);
                callApi(json.toString());
            } catch (Exception e) {
                handleError(e.getMessage());
            }
        }).start();
    }

    private void callApi(String body) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(20000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            JSONObject res = new JSONObject(sb.toString());
            String fullText = "";
            if (res.has("choices")) {
                fullText = res.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content");
            }

            if (fullText.isEmpty()) throw new Exception("AI 未能生成内容");

            // 清洗 Markdown 代码块符号
            String cleanText = fullText.replaceAll("```[a-zA-Z]*", "").replace("```", "").trim();

            String finalQ = "智能识别";
            String finalA = cleanText;

            if (cleanText.contains("题目：") && cleanText.contains("解答：")) {
                int qIdx = cleanText.indexOf("题目：") + 3;
                int aIdx = cleanText.indexOf("解答：");
                if (qIdx < aIdx) {
                    finalQ = cleanText.substring(qIdx, aIdx).trim();
                    finalA = cleanText.substring(aIdx + 3).trim();
                }
            }

            final String fQ = finalQ;
            final String fA = finalA;
            runOnUiThread(() -> {
                if (loadingDialog != null) loadingDialog.dismiss();
                Intent intent = new Intent(this, ConfirmActivity.class);
                intent.putExtra("question", fQ);
                intent.putExtra("answer", fA);
                intent.putExtra("selected_tag", currentSelectedTag);
                startActivity(intent);
            });

        } catch (Exception e) {
            handleError(e.getMessage());
        }
    }

    private void handleError(String msg) {
        runOnUiThread(() -> {
            if (loadingDialog != null) loadingDialog.dismiss();
            Toast.makeText(MainActivity.this, "识别出错: " + msg, Toast.LENGTH_LONG).show();
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        List<String> list = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                list.add(p);
            }
        }
        if (!list.isEmpty()) {
            ActivityCompat.requestPermissions(this, list.toArray(new String[0]), ALL_PERMISSION_CODE);
        }
    }

    private void showLoading() {
        runOnUiThread(() -> {
            View v = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            loadingDialog = new AlertDialog.Builder(this).setView(v).setCancelable(false).create();
            loadingDialog.show();
        });
    }

    private String bitmapToSmallBase64(Bitmap bitmap) {
        Bitmap small = Bitmap.createScaledBitmap(bitmap, 640, 480, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        small.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTagChips();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        cameraUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (questionDao != null) questionDao.close();
    }
}