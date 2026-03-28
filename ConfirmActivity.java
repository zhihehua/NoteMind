package com.example.NoteMind;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ConfirmActivity extends AppCompatActivity {
    private EditText etQuestion, etAnswer, etTag, etUserNote, etCategory;
    private Button btnCancel, btnSave;
    private QuestionDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        etQuestion = findViewById(R.id.et_question);
        etAnswer = findViewById(R.id.et_answer);
        etTag = findViewById(R.id.et_tag);
        etUserNote = findViewById(R.id.et_user_note);
        etCategory = findViewById(R.id.et_category);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        dao = new QuestionDao(this);

        String question = getIntent().getStringExtra("question");
        String answer = getIntent().getStringExtra("answer");
        etQuestion.setText(question);
        etAnswer.setText(answer);

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> {
            String q = etQuestion.getText().toString().trim();
            String a = etAnswer.getText().toString().trim();
            String tag = etTag.getText().toString().trim();
            String note = etUserNote.getText().toString().trim();
            String cate = etCategory.getText().toString().trim();

            // ========== 三项强制校验 ==========
            if (q.isEmpty()) {
                Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (tag.isEmpty()) {
                Toast.makeText(this, "标签不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cate.isEmpty()) {
                Toast.makeText(this, "分类不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            QuestionNote noteObj = new QuestionNote(q, a, tag, note, cate);
            long id = dao.addNote(noteObj);
            if (id > 0) {
                Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dao.close();
    }
}