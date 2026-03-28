package com.example.NoteMind;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class NoteListTwoActivity extends AppCompatActivity {
    private LinearLayout container;
    private QuestionDao dao;
    private TextView tvEmpty;
    private String currentType;
    private String currentKeyword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list_two);

        container = findViewById(R.id.ll_note_list);
        tvEmpty = findViewById(R.id.tv_empty);
        dao = new QuestionDao(this);

        Intent intent = getIntent();
        currentType = intent.getStringExtra("type");
        currentKeyword = intent.getStringExtra("keyword");

        showFilteredList(currentType, currentKeyword);
    }

    private void showFilteredList(String type, String keyword) {
        List<QuestionNote> allNotes = dao.queryAll();
        List<QuestionNote> filteredList = new ArrayList<>();

        for (QuestionNote note : allNotes) {
            switch (type) {
                case "question":
                    if (note.getQuestion().contains(keyword)) {
                        filteredList.add(note);
                    }
                    break;
                case "tag":
                    if (note.getTag() != null && note.getTag().contains(keyword)) {
                        filteredList.add(note);
                    }
                    break;
                case "category":
                    if (note.getCategory() != null && note.getCategory().contains(keyword)) {
                        filteredList.add(note);
                    }
                    break;
                default:
                    finish();
                    return;
            }
        }

        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            container.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);
        container.removeAllViews();

        for (QuestionNote note : filteredList) {
            TextView tvItem = new TextView(this);
            tvItem.setTextSize(16);
            tvItem.setPadding(20, 20, 20, 20);
            tvItem.setBackgroundResource(R.drawable.dialog_bg);
            tvItem.setText("• " + note.getQuestion());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 8;
            params.leftMargin = 16;
            params.rightMargin = 16;
            tvItem.setLayoutParams(params);

            tvItem.setOnClickListener(v -> {
                Intent detail = new Intent(NoteListTwoActivity.this, DetailActivity.class);
                detail.putExtra("id", note.getId());
                startActivity(detail);
            });

            container.addView(tvItem);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dao.close();
    }
}