package com.example.NoteMind;



import android.content.Intent;

import android.net.Uri;

import android.os.Bundle;

import android.view.Gravity;

import android.view.View;

import android.widget.CheckBox;

import android.widget.LinearLayout;

import android.widget.TextView;

import android.widget.Toast;



import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.FileProvider;



import com.itextpdf.io.font.PdfEncodings;

import com.itextpdf.kernel.font.PdfFont;

import com.itextpdf.kernel.font.PdfFontFactory;

import com.itextpdf.kernel.pdf.PdfDocument;

import com.itextpdf.kernel.pdf.PdfWriter;



import java.io.ByteArrayOutputStream;

import java.io.File;

import java.io.FileOutputStream;

import java.io.InputStream;

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

        findViewById(R.id.btn_batch_export).setOnClickListener(v -> {

            if (selectedIds.isEmpty()) {

                Toast.makeText(this, "请先勾选需要导出的笔记", Toast.LENGTH_SHORT).show();

            } else {

                exportToPdf(new ArrayList<>(selectedIds));

            }

        });



        loadData();

    }



    private void loadData() {

        List<QuestionNote> all = dao.queryAll();

        displayedNotes.clear();

        for (QuestionNote n : all) {

            if (currentType != null && currentKeyword != null) {

                if (currentType.equals("question") && n.getQuestion().contains(currentKeyword)) displayedNotes.add(n);

                else if (currentType.equals("tag") && n.getTag() != null && n.getTag().contains(currentKeyword)) displayedNotes.add(n);

                else if (currentType.equals("category") && n.getCategory() != null && n.getCategory().contains(currentKeyword)) displayedNotes.add(n);

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

            item.setPadding(0, 15, 0, 15);



            CheckBox cb = new CheckBox(this);

            cb.setVisibility(isManaging ? View.VISIBLE : View.GONE);

            cb.setChecked(selectedIds.contains(note.getId()));

            cb.setOnCheckedChangeListener((b, checked) -> {

                if (checked) selectedIds.add(note.getId());

                else selectedIds.remove(note.getId());

            });



            TextView tv = new TextView(this);

            tv.setText("• " + note.getQuestion());

            tv.setTextSize(16);

            tv.setPadding(20, 30, 20, 30);

            tv.setBackgroundResource(R.drawable.dialog_bg);

            tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));



            tv.setOnClickListener(v -> {

                if (isManaging) cb.setChecked(!cb.isChecked());

                else startActivity(new Intent(this, DetailActivity.class).putExtra("id", note.getId()));

            });



            item.addView(cb);

            item.addView(tv);

            container.addView(item);

        }

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

        new AlertDialog.Builder(this).setTitle("确认删除").setMessage("确定要删除选中的笔记吗？")

                .setPositiveButton("确定", (d, w) -> {

                    for (Integer id : selectedIds) dao.deleteById(id);

                    isManaging = false;

                    loadData();

                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();

                }).setNegativeButton("取消", null).show();

    }



    private void exportToPdf(List<Integer> ids) {

        String fileName = "NoteMind_Export_" + System.currentTimeMillis() + ".pdf";

        File file = new File(getExternalFilesDir("Documents"), fileName);



        try {

            PdfWriter writer = new PdfWriter(new FileOutputStream(file));

            PdfDocument pdf = new PdfDocument(writer);

            com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);



            byte[] fontByte = readAsset("SimsunExtG.ttf");

            PdfFont font = PdfFontFactory.createFont(fontByte, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);



// 标题居中（全路径引用）

            doc.add(new com.itextpdf.layout.element.Paragraph("知识复盘清单")

                    .setFont(font)

                    .setFontSize(22));



            doc.add(new com.itextpdf.layout.element.Paragraph("\n-------------------\n").setFont(font));



            for (QuestionNote n : displayedNotes) {

                if (ids.contains(n.getId())) {

                    doc.add(new com.itextpdf.layout.element.Paragraph("【问】" + n.getQuestion()).setFont(font).setBold().setFontSize(16));

                    String ans = (n.getAnswer() != null) ? n.getAnswer() : "暂无内容";

                    doc.add(new com.itextpdf.layout.element.Paragraph("【答】" + ans).setFont(font).setFontSize(14));

                    doc.add(new com.itextpdf.layout.element.Paragraph("\n"));

                }

            }

            doc.close();

            Toast.makeText(this, "PDF 导出成功，请选择程序打开", Toast.LENGTH_SHORT).show();

            sharePdf(file);

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(this, "导出出错: " + e.getMessage(), Toast.LENGTH_LONG).show();

        }

    }



    private byte[] readAsset(String name) throws Exception {

        try (InputStream is = getAssets().open(name);

             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] b = new byte[1024];

            int len;

            while ((len = is.read(b)) != -1) bos.write(b, 0, len);

            return bos.toByteArray();

        }

    }



    private void sharePdf(File f) {

        if (!f.exists()) return;



        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);

        Intent i = new Intent(Intent.ACTION_SEND);

        i.setType("application/pdf");

        i.putExtra(Intent.EXTRA_STREAM, uri);

        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);



        try {

// 启动分享选择器

            startActivity(Intent.createChooser(i, "分享或预览笔记 PDF"));

        } catch (Exception e) {

// 虚拟机上最容易触发这个错误

            new AlertDialog.Builder(this)

                    .setTitle("无法打开文件")

                    .setMessage("检测到您的系统中没有安装 PDF 阅读器（如 WPS 或 Adobe Reader）。请安装相关应用后再试。")

                    .setPositiveButton("我知道了", null)

                    .show();

        }

    }



    @Override

    protected void onDestroy() {

        super.onDestroy();

        if (dao != null) dao.close();

    }

}