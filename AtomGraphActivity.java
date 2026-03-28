package com.example.NoteMind;

import android.content.Intent;
import android.os.Build; // 新增
import android.os.Bundle;
import android.view.View; // 新增
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtomGraphActivity extends AppCompatActivity {
    private WebView webView;
    private QuestionDao dao;
    private String targetTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atom_graph);

        // --- 核心修复 1：在 Activity 级别开启硬件加速 ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        targetTag = getIntent().getStringExtra("TARGET_TAG");
        if (targetTag == null) targetTag = "未知标签";

        TextView tvTitle = findViewById(R.id.tv_graph_title);
        tvTitle.setText(targetTag + " · 原子分布");

        webView = findViewById(R.id.webview_atom);

        // --- 核心修复 2：在 WebView 级别二次强化硬件加速 ---
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        dao = new QuestionDao(this);

        initGraph();
        findViewById(R.id.btn_graph_back).setOnClickListener(v -> finish());
    }

    private void initGraph() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        // 提高渲染优先级
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/atom_engine.html");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // --- 核心修复 3：增加延迟注入，确保 HTML 里的 JS 函数已声明 ---
                webView.postDelayed(() -> injectDataToGraph(), 300);
            }
        });
    }

    private void injectDataToGraph() {
        List<QuestionNote> notes = dao.queryByTag(targetTag);
        try {
            JSONObject data = new JSONObject();
            JSONArray nodes = new JSONArray();
            JSONArray links = new JSONArray();

            // 1. 中心节点
            JSONObject root = new JSONObject();
            root.put("name", targetTag);
            root.put("value", 60);
            root.put("category", 0);
            nodes.put(root);

            // 2. 分类统计
            Map<String, Integer> categoryMap = new HashMap<>();
            for (QuestionNote note : notes) {
                String cat = note.getCategory();
                if (cat == null || cat.trim().isEmpty()) cat = "未分类";
                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0) + 1);
            }

            // 3. 构建周围节点
            for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {
                String catName = entry.getKey();
                int count = entry.getValue();

                JSONObject node = new JSONObject();
                node.put("name", catName);
                node.put("value", 25 + (count * 10));
                node.put("category", 1);
                nodes.put(node);

                JSONObject link = new JSONObject();
                link.put("source", targetTag);
                link.put("target", catName);
                links.put(link);
            }

            data.put("nodes", nodes);
            data.put("links", links);

            // --- 核心修复 4：处理字符串转义，防止数据中的特殊字符破坏 JS 语法 ---
            final String jsonData = data.toString().replace("\\", "\\\\").replace("'", "\\'");

            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript("renderGraph('" + jsonData + "')", null);
                } else {
                    webView.loadUrl("javascript:renderGraph('" + jsonData + "')");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void openCategoryList(String categoryName) {
            runOnUiThread(() -> {
                Intent intent = new Intent(AtomGraphActivity.this, NoteListTwoActivity.class);
                intent.putExtra("type", "category");
                intent.putExtra("keyword", categoryName);
                startActivity(intent);
            });
        }

        @android.webkit.JavascriptInterface
        public void startAiReview() {
            runOnUiThread(() -> {
                Intent intent = new Intent(AtomGraphActivity.this, SummaryActivity.class);
                intent.putExtra("TARGET_TAG", targetTag);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dao != null) dao.close();
    }
}