package com.example.NoteMind;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class NoteListActivity extends AppCompatActivity {
    private LinearLayout container, tagContainer;
    private ScrollView scrollList;
    private TextView titleList;
    private EditText etSearch;
    private Button btnSearch;
    private QuestionDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);
        container = findViewById(R.id.container);
        tagContainer = findViewById(R.id.tag_container);
        titleList = findViewById(R.id.title_list);
        scrollList = findViewById(R.id.scroll_list);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        dao = new QuestionDao(this);

        // 点击搜索按钮 → 弹出搜索类型
        btnSearch.setOnClickListener(v -> showSearchTypeDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        showAllTags(); // 核心修改：此方法内已把category换成tag
        hideList();
    }

    // 显示标签泡泡【已修正】：从tag字段获取数据，而非category
    private void showAllTags() {
        tagContainer.removeAllViews();
        List<QuestionNote> all = dao.queryAll();
        Set<String> tagSet = new HashSet<>(); // 改为tagSet
        // 遍历笔记，提取tag字段去重
        for (QuestionNote note : all) {
            String tag = note.getTag().trim(); // 核心：获取tag而非category
            if (!tag.isEmpty()) tagSet.add(tag); // 非空tag才展示
        }
        // 生成标签泡泡
        for (String tag : tagSet) {
            TextView tagView = new TextView(this);
            tagView.setText("  " + tag + "  ");
            tagView.setTextSize(14);
            tagView.setBackgroundResource(R.drawable.dialog_bg);
            tagView.setPadding(20, 10, 20, 10);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 4;
            tagView.setLayoutParams(params);
            // 点击泡泡 → 按tag筛选笔记（逻辑匹配）
            tagView.setOnClickListener(v -> {
                Intent intent = new Intent(NoteListActivity.this, NoteListTwoActivity.class);
                intent.putExtra("type", "tag"); // 筛选类型改为tag
                intent.putExtra("keyword", tag);
                startActivity(intent);
            });
            tagContainer.addView(tagView);
        }
    }

    // 搜索弹窗：保持原有逻辑（按标题/标签/分类搜索）
    private void showSearchTypeDialog() {
        String text = etSearch.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] types = {"按标题搜索", "按标签搜索", "按分类搜索"};
        new AlertDialog.Builder(this)
                .setTitle("选择搜索类型")
                .setItems(types, (dialog, which) -> {
                    Intent intent = new Intent(this, NoteListTwoActivity.class);
                    intent.putExtra("keyword", text);
                    switch (which) {
                        case 0: intent.putExtra("type", "question"); break;
                        case 1: intent.putExtra("type", "tag"); break;
                        case 2: intent.putExtra("type", "category"); break;
                    }
                    startActivity(intent);
                })
                .show();
    }

    private void hideList() {
        titleList.setVisibility(View.GONE);
        scrollList.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dao.close();
    }
}