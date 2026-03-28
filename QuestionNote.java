package com.example.NoteMind;

public class QuestionNote {
    private int id;
    private String question;    // 标题
    private String answer;      // ai解答
    private String tag;         // 标签：高数/食谱/旅游日记
    private String userNote;    // 用户笔记
    private String category;    // 细分范围：方程/数列/不等式
    private int knowledgeId;    // 预留：知识图谱ID
    private String photoPaths;  // 照片路径，多张用|分隔
    private String createTime;  // 新增：创建时间（用于数学模型概率分析和阶段总结）

    public QuestionNote() {}

    // 【添加用】（无ID，无时间，由数据库自动生成时间）
    public QuestionNote(String question, String answer, String tag, String userNote, String category) {
        this.question = question;
        this.answer = answer;
        this.tag = tag;
        this.userNote = userNote;
        this.category = category;
        this.knowledgeId = 0;
        this.photoPaths = "";
        this.createTime = ""; // 插入时可为空
    }

    // 【查询用】（旧版兼容：带ID，无照片路径，无时间）
    public QuestionNote(int id, String question, String answer, String tag, String userNote, String category, int knowledgeId) {
        this(id, question, answer, tag, userNote, category, knowledgeId, "", "");
    }

    // 【查询用】（过渡版：带ID+照片路径，无时间）
    public QuestionNote(int id, String question, String answer, String tag, String userNote, String category, int knowledgeId, String photoPaths) {
        this(id, question, answer, tag, userNote, category, knowledgeId, photoPaths, "");
    }

    // 【全能构造函数】（带ID+照片路径+创建时间，核心完整版）
    public QuestionNote(int id, String question, String answer, String tag, String userNote, String category, int knowledgeId, String photoPaths, String createTime) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.tag = tag;
        this.userNote = userNote;
        this.category = category;
        this.knowledgeId = knowledgeId;
        this.photoPaths = photoPaths;
        this.createTime = createTime;
    }

    // ———— Getter & Setter ————
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getUserNote() { return userNote; }
    public void setUserNote(String userNote) { this.userNote = userNote; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(int knowledgeId) { this.knowledgeId = knowledgeId; }

    public String getPhotoPaths() { return photoPaths; }
    public void setPhotoPaths(String photoPaths) { this.photoPaths = photoPaths; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}