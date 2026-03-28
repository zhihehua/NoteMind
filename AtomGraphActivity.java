package com.example.NoteMind;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class AtomGraphActivity extends AppCompatActivity {
    private WebView webView;
    private QuestionDao dao;
    private String targetTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atom_graph);

        targetTag = getIntent().getStringExtra("TARGET_TAG");
        ((TextView)findViewById(R.id.tv_graph_title)).setText(targetTag + " · 原子分布");

        webView = findViewById(R.id.webview_atom);
        dao = new QuestionDao(this);

        initGraph();
        findViewById(R.id.btn_graph_back).setOnClickListener(v -> finish());
    }

    private void initGraph() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        // 加载 assets 下的 html 模板
        webView.loadUrl("file:///android_asset/atom_engine.html");

        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                injectDataToGraph();
            }
        });
    }

    private void injectDataToGraph() {
        // 根据 Tag 查出该分类下所有的笔记
        List<QuestionNote> notes = dao.queryByTag(targetTag);

        try {
            JSONObject data = new JSONObject();
            JSONArray nodes = new JSONArray();
            JSONArray links = new JSONArray();

            // 中心原子（你选的 Tag，如“高数”）
            JSONObject root = new JSONObject();
            root.put("name", targetTag);
            root.put("value", 50); // 大小
            root.put("category", 0);
            nodes.put(root);

            // 周围原子（对应的 Category，如“积分”）
            for (QuestionNote note : notes) {
                JSONObject node = new JSONObject();
                node.put("name", note.getCategory());
                node.put("value", 30);
                node.put("category", 1);
                nodes.put(node);

                JSONObject link = new JSONObject();
                link.put("source", targetTag);
                link.put("target", note.getCategory());
                links.put(link);
            }

            data.put("nodes", nodes);
            data.put("links", links);

            // 把数据传给 JS
            webView.evaluateJavascript("renderGraph('" + data.toString() + "')", null);
        } catch (Exception e) { e.printStackTrace(); }
    }
}