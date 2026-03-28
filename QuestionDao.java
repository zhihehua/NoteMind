package com.example.NoteMind;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class QuestionDao {
    private final DbHelper dbHelper;
    private final SQLiteDatabase db;

    public QuestionDao(Context context) {
        dbHelper = new DbHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    // 增加笔记
    public long addNote(QuestionNote note) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.QUESTION, note.getQuestion());
        values.put(DbHelper.ANSWER, note.getAnswer());
        values.put(DbHelper.TAG, note.getTag());
        values.put(DbHelper.USER_NOTE, note.getUserNote());
        values.put(DbHelper.CATEGORY, note.getCategory());
        values.put(DbHelper.KNOWLEDGE_ID, note.getKnowledgeId());
        values.put(DbHelper.PHOTO_PATHS, note.getPhotoPaths());
        return db.insert(DbHelper.TABLE_NAME, null, values);
    }

    // 查询所有
    public List<QuestionNote> queryAll() {
        List<QuestionNote> list = new ArrayList<>();
        Cursor cursor = db.query(DbHelper.TABLE_NAME, null, null, null, null, null, DbHelper.ID + " DESC");
        while (cursor.moveToNext()) {
            list.add(cursorToNote(cursor));
        }
        cursor.close();
        return list;
    }

    // 【新增】获取所有不重复的标签（用于弹窗筛选）
    public List<String> getAllUniqueTags() {
        List<String> tags = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + DbHelper.TAG + " FROM " + DbHelper.TABLE_NAME, null);
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String tag = cursor.getString(cursor.getColumnIndex(DbHelper.TAG));
            if (tag != null && !tag.isEmpty()) {
                tags.add(tag);
            }
        }
        cursor.close();
        return tags;
    }

    // 【新增】根据标签查出所有相关的笔记（用于图谱展示）
    public List<QuestionNote> queryByTag(String tag) {
        List<QuestionNote> list = new ArrayList<>();
        Cursor cursor = db.query(DbHelper.TABLE_NAME, null, DbHelper.TAG + "=?", new String[]{tag}, null, null, null);
        while (cursor.moveToNext()) {
            list.add(cursorToNote(cursor));
        }
        cursor.close();
        return list;
    }

    public void updateNote(QuestionNote note) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.QUESTION, note.getQuestion());
        values.put(DbHelper.ANSWER, note.getAnswer());
        values.put(DbHelper.TAG, note.getTag());
        values.put(DbHelper.USER_NOTE, note.getUserNote());
        values.put(DbHelper.CATEGORY, note.getCategory());
        values.put(DbHelper.PHOTO_PATHS, note.getPhotoPaths());
        db.update(DbHelper.TABLE_NAME, values, DbHelper.ID + "=?", new String[]{String.valueOf(note.getId())});
    }

    public void deleteById(int id) {
        db.delete(DbHelper.TABLE_NAME, DbHelper.ID + "=?", new String[]{String.valueOf(id)});
    }

    public QuestionNote queryById(int id) {
        Cursor cursor = db.query(DbHelper.TABLE_NAME, null, DbHelper.ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor.moveToFirst()) {
            QuestionNote note = cursorToNote(cursor);
            cursor.close();
            return note;
        }
        cursor.close();
        return null;
    }

    @SuppressLint("Range")
    private QuestionNote cursorToNote(Cursor cursor) {
        return new QuestionNote(
                cursor.getInt(cursor.getColumnIndex(DbHelper.ID)),
                cursor.getString(cursor.getColumnIndex(DbHelper.QUESTION)),
                cursor.getString(cursor.getColumnIndex(DbHelper.ANSWER)),
                cursor.getString(cursor.getColumnIndex(DbHelper.TAG)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_NOTE)),
                cursor.getString(cursor.getColumnIndex(DbHelper.CATEGORY)),
                cursor.getInt(cursor.getColumnIndex(DbHelper.KNOWLEDGE_ID)),
                cursor.getString(cursor.getColumnIndex(DbHelper.PHOTO_PATHS)),
                cursor.getString(cursor.getColumnIndex(DbHelper.CREATE_TIME)) // 【读取时间】
        );
    }
    public void close() {
        dbHelper.close();
    }
}