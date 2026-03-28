package com.example.NoteMind;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
    private CameraUtils cameraUtils;
    private QuestionDao questionDao;
    private static final String API_KEY = "你的API_KEY"; // APIKey
    private static final String API_URL = "https://hello-occejwojlb.cn-hangzhou.fcapp.run";
    private static final String MODEL = "doubao-seed-1-8-251228";
    private AlertDialog loadingDialog;
    private static final int ALL_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        questionDao = new QuestionDao(this);
        cameraUtils = new CameraUtils(this);

        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnGallery = findViewById(R.id.btn_gallery);
        btnInputText = findViewById(R.id.btn_input_text);
        btnShowList = findViewById(R.id.btn_show_list);
        btnKnowledgeAtom = findViewById(R.id.btn_knowledge_atom);

        checkPermissions();

        btnTakePhoto.setOnClickListener(v -> cameraUtils.openCamera());
        btnGallery.setOnClickListener(v -> cameraUtils.openGallery());
        btnInputText.setOnClickListener(v -> showInputTextDialog());
        btnShowList.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NoteListActivity.class)));
        btnKnowledgeAtom.setOnClickListener(v -> showAtomTagDialog());

        cameraUtils.setOnCameraResultListener(new CameraUtils.OnCameraResultListener() {
            @Override
            public void onCameraSuccess(Bitmap bitmap) {
                requestAiOcr(bitmap);
            }
            @Override
            public void onGallerySuccess(Bitmap bitmap) {
                requestAiOcr(bitmap);
            }
            @Override
            public void onFail(String msg) {
                Toast.makeText(MainActivity.this, "操作失败: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAtomTagDialog() {
        List<String> tags = questionDao.getAllUniqueTags();
        if (tags.isEmpty()) {
            Toast.makeText(this, "暂无笔记数据，无法生成图谱", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] tagArray = tags.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("选择要探索的领域")
                .setItems(tagArray, (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, AtomGraphActivity.class);
                    intent.putExtra("TARGET_TAG", tagArray[which]);
                    startActivity(intent);
                }).show();
    }

    private void showInputTextDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_single_input, null);
        EditText et = v.findViewById(R.id.et_single_question);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        v.findViewById(R.id.btn_cancel_single).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_send_ai).setOnClickListener(view -> {
            String text = et.getText().toString().trim();
            if (!text.isEmpty()) {
                dialog.dismiss();
                requestAiText(text);
            }
        });
        dialog.show();
    }

    private void checkPermissions() {
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

    private void requestAiOcr(Bitmap bitmap) {
        showLoading();
        new Thread(() -> {
            try {
                String base64 = bitmapToSmallBase64(bitmap);
                JSONObject json = new JSONObject();
                json.put("model", MODEL);
                JSONArray messages = new JSONArray();

                // 重点：加入 System 角色规范格式
                messages.put(new JSONObject().put("role", "system").put("content", "你是一个知识助手。请严格按格式输出，禁止Markdown符号和开场白：题目：xxx解答：xxx"));

                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                JSONArray contentArr = new JSONArray();
                JSONObject textObj = new JSONObject();
                textObj.put("type", "text");
                textObj.put("text", "请识别图片中的题目并给出解答。");
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
                e.printStackTrace();
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

                // 重点：加入 System 角色规范格式，解决乱码和格式崩溃
                messages.put(new JSONObject().put("role", "system").put("content", "你是一个知识助手。请严格按格式输出，禁止Markdown符号和开场白：题目：xxx解答：xxx"));

                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", "内容：" + text);
                messages.put(msg);
                json.put("messages", messages);
                callApi(json.toString());
            } catch (Exception e) {
                e.printStackTrace();
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
            conn.setConnectTimeout(15000);

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
                JSONObject messageObj = res.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
                // 重点：getString 自动处理转义乱码
                fullText = messageObj.getString("content");
            }

            if (fullText.isEmpty()) throw new Exception("AI 返回内容为空");

            // 清洗 Markdown 代码块，防止切割失败
            String cleanText = fullText.replace("```", "").replace("html", "").replace("json", "").trim();

            String finalQuestion = "识别结果";
            String finalAnswer = cleanText;

            if (cleanText.contains("题目：") && cleanText.contains("解答：")) {
                int qIdx = cleanText.indexOf("题目：") + 3;
                int aIdx = cleanText.indexOf("解答：");
                if (qIdx < aIdx) {
                    finalQuestion = cleanText.substring(qIdx, aIdx).trim();
                    finalAnswer = cleanText.substring(aIdx + 3).trim();
                }
            }

            final String q = finalQuestion;
            final String a = finalAnswer;
            runOnUiThread(() -> {
                if (loadingDialog != null) loadingDialog.dismiss();
                showConfirmDialog(q, a);
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                if (loadingDialog != null) loadingDialog.dismiss();
                Toast.makeText(MainActivity.this, "识别失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    private void showConfirmDialog(String q, String a) {
        Intent intent = new Intent(this, ConfirmActivity.class);
        intent.putExtra("question", q);
        intent.putExtra("answer", a);
        startActivity(intent);
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
        small.compress(Bitmap.CompressFormat.JPEG, 40, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
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