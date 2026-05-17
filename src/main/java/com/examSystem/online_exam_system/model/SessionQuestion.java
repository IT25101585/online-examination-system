package com.examSystem.online_exam_system.model;

import jakarta.persistence.*;

// a copy of a question specifically for one exam session
// so editing here doesn't affect the original question bank
@Entity
@Table(name = "session_questions")
public class SessionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // which session this question belongs to
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession session;

    // reference to original question (for tracking only)
    @ManyToOne
    @JoinColumn(name = "original_question_id")
    private Question originalQuestion;

    // all fields are copied from the original question
    // editing these does NOT affect the question bank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    @Column(nullable = false)
    private String correctAnswer;

    @Column(nullable = false)
    private Integer marks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExamSession getSession() { return session; }
    public void setSession(ExamSession session) { this.session = session; }

    public Question getOriginalQuestion() { return originalQuestion; }
    public void setOriginalQuestion(Question originalQuestion) {
        this.originalQuestion = originalQuestion;
    }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Integer getMarks() { return marks; }
    public void setMarks(Integer marks) { this.marks = marks; }
}