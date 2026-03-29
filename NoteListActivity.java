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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        btnSearch.setOnClickListener(v -> showSearchTypeDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        showAllTags();
        hideList();
    }

    private void showAllTags() {
        tagContainer.removeAllViews();
        List<QuestionNote> all = dao.queryAll();
        Set<String> tagSet = new HashSet<>();
        for (QuestionNote note : all) {
            String tag = note.getTag() != null ? note.getTag().trim() : "";
            if (!tag.isEmpty()) tagSet.add(tag);
        }
        for (String tag : tagSet) {
            TextView tagView = new TextView(this);
            tagView.setText("  " + tag + "  ");
            tagView.setTextSize(14);
            tagView.setBackgroundResource(R.drawable.dialog_bg);
            tagView.setPadding(20, 10, 20, 10);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.bottomMargin = 4;
            tagView.setLayoutParams(params);
            tagView.setOnClickListener(v -> {
                Intent intent = new Intent(this, NoteListTwoActivity.class);
                intent.putExtra("type", "tag");
                intent.putExtra("keyword", tag);
                startActivity(intent);
            });
            tagContainer.addView(tagView);
        }
    }

    // --- 修复：Material 风格的搜索选择弹窗 ---
    private void showSearchTypeDialog() {
        String text = etSearch.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] types = {"按标题搜索", "按标签搜索", "按分类搜索"};
        new MaterialAlertDialogBuilder(this)
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
        if(titleList != null) titleList.setVisibility(View.GONE);
        if(scrollList != null) scrollList.setVisibility(View.GONE);
    }

    @Override protected void onDestroy() { super.onDestroy(); dao.close(); }
}