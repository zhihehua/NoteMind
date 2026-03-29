package com.example.NoteMind;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoteListTwoActivity extends AppCompatActivity {
    private LinearLayout container;
    private QuestionDao dao;
    private TextView tvEmpty, tvManage;
    private LinearLayout llBatchOp;

    private String currentType, currentKeyword;
    private boolean isManaging = false;
    private List<QuestionNote> displayedNotes = new ArrayList<>();
    private Set<Integer> selectedIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list_two);

        container = findViewById(R.id.ll_note_list);
        tvEmpty = findViewById(R.id.tv_empty);
        tvManage = findViewById(R.id.tv_manage);
        llBatchOp = findViewById(R.id.ll_batch_op);
        dao = new QuestionDao(this);

        currentType = getIntent().getStringExtra("type");
        currentKeyword = getIntent().getStringExtra("keyword");

        tvManage.setOnClickListener(v -> toggleManageMode());

        findViewById(R.id.btn_batch_delete).setOnClickListener(v -> performBatchDelete());

        // 关键回退点：最原始的导出触发
        findViewById(R.id.btn_batch_export).setOnClickListener(v -> {
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "请先勾选笔记", Toast.LENGTH_SHORT).show();
            } else {
                exportToPdf(new ArrayList<>(selectedIds));
            }
        });

        loadData();
    }

    private void loadData() {
        List<QuestionNote> all = dao.queryAll();
        displayedNotes.clear();
        if (currentType == null || currentKeyword == null) {
            displayedNotes.addAll(all);
        } else {
            for (QuestionNote n : all) {
                if (currentType.equals("question") && n.getQuestion().contains(currentKeyword)) {
                    displayedNotes.add(n);
                } else if (currentType.equals("tag") && n.getTag() != null && n.getTag().contains(currentKeyword)) {
                    displayedNotes.add(n);
                } else if (currentType.equals("category") && n.getCategory() != null && n.getCategory().contains(currentKeyword)) {
                    displayedNotes.add(n);
                }
            }
        }
        renderList();
    }

    private void renderList() {
        container.removeAllViews();
        tvEmpty.setVisibility(displayedNotes.isEmpty() ? View.VISIBLE : View.GONE);
        tvManage.setVisibility(displayedNotes.isEmpty() ? View.GONE : View.VISIBLE);

        for (QuestionNote note : displayedNotes) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(-1, -2);
            itemParams.setMargins(0, 10, 0, 10);
            item.setLayoutParams(itemParams);

            CheckBox cb = new CheckBox(this);
            cb.setVisibility(isManaging ? View.VISIBLE : View.GONE);
            cb.setChecked(selectedIds.contains(note.getId()));
            cb.setOnCheckedChangeListener((b, checked) -> {
                if (checked) selectedIds.add(note.getId());
                else selectedIds.remove(note.getId());
            });

            TextView tv = new TextView(this);
            tv.setText(note.getQuestion());
            tv.setTextSize(16);
            tv.setBackgroundResource(R.drawable.dialog_bg);
            LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
            tvParams.setMargins(10, 0, 10, 0);
            tv.setLayoutParams(tvParams);
            tv.setPadding(40, 30, 40, 30);

            tv.setOnClickListener(v -> {
                if (isManaging) cb.setChecked(!cb.isChecked());
                else startActivity(new Intent(this, DetailActivity.class).putExtra("id", note.getId()));
            });

            item.addView(cb);
            item.addView(tv);
            container.addView(item);
        }
    }

    // 关键回退点：恢复 5:31 之前的 HTML 构建逻辑
    private void exportToPdf(List<Integer> ids) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        for (QuestionNote n : displayedNotes) {
            if (ids.contains(n.getId())) {
                html.append("<h3>").append(n.getQuestion()).append("</h3>");
                html.append("<p>").append(n.getAnswer()).append("</p>");
                html.append("<hr/>");
            }
        }
        html.append("</body></html>");

        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                if (printManager != null) {
                    printManager.print("NoteMind_Export", view.createPrintDocumentAdapter("Export"), new PrintAttributes.Builder().build());
                }
            }
        });
        webView.loadDataWithBaseURL(null, html.toString(), "text/html", "utf-8", null);
    }

    private void toggleManageMode() {
        isManaging = !isManaging;
        tvManage.setText(isManaging ? "取消" : "管理");
        llBatchOp.setVisibility(isManaging ? View.VISIBLE : View.GONE);
        selectedIds.clear();
        renderList();
    }

    private void performBatchDelete() {
        if (selectedIds.isEmpty()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("确认删除")
                .setPositiveButton("确定", (d, w) -> {
                    for (Integer id : selectedIds) dao.deleteById(id);
                    isManaging = false;
                    loadData();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override protected void onDestroy() { super.onDestroy(); dao.close(); }
}