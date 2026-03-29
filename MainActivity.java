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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // 新增 ImageButton 导入
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    // 修改：底部三段式使用 ImageButton 适配 SVG
    private ImageButton btnRecognizeMain, btnKnowledgeAtom, btnShowList;

    // 弹出菜单内部依然使用 Button 或保持原样
    private Button btnTakePhoto, btnGallery, btnInputText;
    private LinearLayout llRecognizeMenu;
    private boolean isMenuExpanded = false;

    private ChipGroup chipGroupTags;
    private String currentSelectedTag = "";

    private CameraUtils cameraUtils;
    private QuestionDao questionDao;

    private static final String API_KEY = "你的API_KEY";
    private static final String API_URL = "https://hello-occejwojlb.cn-hangzhou.fcapp.run";
    private static final String MODEL = "doubao-seed-1-8-251228";

    private AlertDialog loadingDialog;
    private static final int ALL_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 开启硬件加速
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        questionDao = new QuestionDao(this);
        cameraUtils = new CameraUtils(this);

        // --- 1. 绑定底部三段式核心 ImageButton ---
        btnRecognizeMain = findViewById(R.id.btn_recognize_main); // 对应 ic_list
        btnKnowledgeAtom = findViewById(R.id.btn_knowledge_atom); // 对应 ic_atom
        btnShowList = findViewById(R.id.btn_show_list);           // 对应 it_my

        // --- 2. 绑定弹出菜单及其子功能 ---
        llRecognizeMenu = findViewById(R.id.ll_recognize_menu);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnGallery = findViewById(R.id.btn_gallery);
        btnInputText = findViewById(R.id.btn_input_text);

        chipGroupTags = findViewById(R.id.chip_group_tags);

        checkPermissions();
        refreshTagChips();

        // --- 3. 核心交互逻辑：三段式点击 ---

        // 左侧：识别列表图标 (点击切换菜单)
        btnRecognizeMain.setOnClickListener(v -> toggleRecognizeMenu());

        // 中间：原子智图
        btnKnowledgeAtom.setOnClickListener(v -> {
            closeMenu();
            showAtomTagDialog();
        });

        // 右侧：我的/展示列表
        btnShowList.setOnClickListener(v -> {
            closeMenu();
            startActivity(new Intent(this, NoteListActivity.class));
        });

        // --- 4. 弹出菜单子项功能 ---

        btnTakePhoto.setOnClickListener(v -> {
            closeMenu();
            cameraUtils.openCamera();
        });

        btnGallery.setOnClickListener(v -> {
            closeMenu();
            cameraUtils.openGallery();
        });

        btnInputText.setOnClickListener(v -> {
            closeMenu();
            showInputTextDialog();
        });

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

    /**
     * 切换识别菜单的显示状态（带优雅动画）
     */
    private void toggleRecognizeMenu() {
        if (!isMenuExpanded) {
            llRecognizeMenu.setVisibility(View.VISIBLE);
            llRecognizeMenu.setAlpha(0f);
            llRecognizeMenu.setTranslationY(20f); // 从下方浮现
            llRecognizeMenu.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .start();
        } else {
            closeMenu();
        }
        isMenuExpanded = !isMenuExpanded;
    }

    /**
     * 强制收起菜单
     */
    private void closeMenu() {
        if (llRecognizeMenu.getVisibility() == View.VISIBLE) {
            llRecognizeMenu.animate()
                    .alpha(0f)
                    .translationY(20f)
                    .setDuration(200)
                    .withEndAction(() -> llRecognizeMenu.setVisibility(View.GONE))
                    .start();
            isMenuExpanded = false;
        }
    }

    private void refreshTagChips() {
        if (chipGroupTags == null) return;
        chipGroupTags.removeAllViews();

        Chip addBtn = new Chip(this);
        addBtn.setText("+ 新标签");
        addBtn.setTextSize(15f); // 调大字体，增强突出感
        addBtn.setChipIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_input_add));
        addBtn.setCheckable(false);
        addBtn.setOnClickListener(v -> showNewTagInput());
        chipGroupTags.addView(addBtn);

        List<String> tags = questionDao.getAllUniqueTags();
        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setTextSize(15f); // 调大字体
            chip.setCheckable(true);
            if (tag.equals(currentSelectedTag)) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) currentSelectedTag = tag;
                else if (currentSelectedTag.equals(tag)) currentSelectedTag = "";
            });
            chipGroupTags.addView(chip);
        }
    }

    // --- 后续 AI 识别与弹窗逻辑保持不变 ---

    private void showNewTagInput() {
        View v = LayoutInflater.from(this).inflate(R.layout.layout_custom_dialog, null);
        TextView tvTitle = v.findViewById(R.id.dialog_title);
        EditText etInput = v.findViewById(R.id.dialog_input);
        Button btnConfirm = v.findViewById(R.id.dialog_btn_confirm);

        tvTitle.setText("定义新场景: (如：高数、菜谱)");
        etInput.setHint("");
        etInput.setBackgroundResource(R.drawable.edittext_bg);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(true)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        }

        btnConfirm.setBackgroundResource(R.drawable.btn_bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btnConfirm.setBackgroundTintList(null);
        }

        btnConfirm.setOnClickListener(view -> {
            String val = etInput.getText().toString().trim();
            if(!val.isEmpty()) {
                currentSelectedTag = val;
                refreshTagChips();
                dialog.dismiss();
            }
        });
    }

    private void showAtomTagDialog() {
        List<String> tags = questionDao.getAllUniqueTags();
        if (tags.isEmpty()) {
            Toast.makeText(this, "暂无数据", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] tagArray = tags.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("选择要探索的原子领域")
                .setItems(tagArray, (dialog, which) -> {
                    Intent intent = new Intent(this, AtomGraphActivity.class);
                    intent.putExtra("TARGET_TAG", tagArray[which]);
                    startActivity(intent);
                }).show();
    }

    private void showInputTextDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.layout_custom_dialog, null);
        TextView tvTitle = v.findViewById(R.id.dialog_title);
        EditText etInput = v.findViewById(R.id.dialog_input);
        Button btnConfirm = v.findViewById(R.id.dialog_btn_confirm);

        String label = "智能问答: (请在此输入问题)";
        if(!currentSelectedTag.isEmpty()) {
            label = "智能问答: (针对 [" + currentSelectedTag + "] 提问)";
        }
        tvTitle.setText(label);
        etInput.setHint("");
        etInput.setBackgroundResource(R.drawable.edittext_bg);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(true)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        }

        btnConfirm.setBackgroundResource(R.drawable.btn_bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btnConfirm.setBackgroundTintList(null);
        }

        btnConfirm.setOnClickListener(view -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                dialog.dismiss();
                String wrappedPrompt = currentSelectedTag.isEmpty() ? text :
                        "【场景标签：" + currentSelectedTag + "】用户问题：" + text;
                requestAiText(wrappedPrompt);
            }
        });
    }

    private void requestAiOcr(Bitmap bitmap) {
        showLoading();
        new Thread(() -> {
            try {
                String base64 = bitmapToSmallBase64(bitmap);
                JSONObject json = new JSONObject();
                json.put("model", MODEL);
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system")
                        .put("content", "你是一个知识助手。请按格式输出,并且不能有转化符号,纯文本输出：题目：xxx解答：xxx。"));
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
            } catch (Exception e) { handleError(e.getMessage()); }
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
            } catch (Exception e) { handleError(e.getMessage()); }
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
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes()); }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONObject res = new JSONObject(sb.toString());
            String fullText = res.getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content");
            String cleanText = fullText.replaceAll("```[a-zA-Z]*", "").replace("```", "").trim();
            String finalQ = "智能识别", finalA = cleanText;
            if (cleanText.contains("题目：") && cleanText.contains("解答：")) {
                int qIdx = cleanText.indexOf("题目：") + 3, aIdx = cleanText.indexOf("解答：");
                if (qIdx < aIdx) {
                    finalQ = cleanText.substring(qIdx, aIdx).trim();
                    finalA = cleanText.substring(aIdx + 3).trim();
                }
            }
            final String fQ = finalQ, fA = finalA;
            runOnUiThread(() -> {
                if (loadingDialog != null) loadingDialog.dismiss();
                Intent intent = new Intent(this, ConfirmActivity.class);
                intent.putExtra("question", fQ);
                intent.putExtra("answer", fA);
                intent.putExtra("selected_tag", currentSelectedTag);
                startActivity(intent);
            });
        } catch (Exception e) { handleError(e.getMessage()); }
    }

    private void handleError(String msg) {
        runOnUiThread(() -> {
            if (loadingDialog != null) loadingDialog.dismiss();
            Toast.makeText(this, "识别出错: " + msg, Toast.LENGTH_LONG).show();
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        List<String> list = new ArrayList<>();

        // 1. 相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.CAMERA);
        }

        // 2. 存储/图片权限解析
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) 及以上，必须申请 READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12 及以下，使用旧权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!list.isEmpty()) {
            ActivityCompat.requestPermissions(this, list.toArray(new String[0]), ALL_PERMISSION_CODE);
        }
    }

    private void showLoading() {
        runOnUiThread(() -> {
            View v = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            loadingDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setView(v)
                    .setCancelable(false)
                    .create();
            loadingDialog.show();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                loadingDialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
            }
        });
    }

    private String bitmapToSmallBase64(Bitmap bitmap) {
        Bitmap small = Bitmap.createScaledBitmap(bitmap, 640, 480, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        small.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    @Override protected void onResume() { super.onResume(); refreshTagChips(); }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        cameraUtils.onActivityResult(requestCode, resultCode, data);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (questionDao != null) questionDao.close();
    }
}