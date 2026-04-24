package com.examSystem.online_exam_system.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // the actual question text
    @NotBlank(message = "Question text is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    // type of question — MCQ, TRUE_FALSE, or SHORT_ANSWER
    @NotNull(message = "Question type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType;

    // for MCQ questions — the four options
    // these are optional for TRUE_FALSE and SHORT_ANSWER
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    // the correct answer
    // for MCQ: "A", "B", "C" or "D"
    // for TRUE_FALSE: "TRUE" or "FALSE"
    // for SHORT_ANSWER: the expected answer text
    @NotBlank(message = "Correct answer is required")
    @Column(nullable = false)
    private String correctAnswer;

    // marks awarded for this question
    @NotNull(message = "Marks is required")
    @Min(value = 1, message = "Marks must be at least 1")
    @Column(nullable = false)
    private Integer marks;

    // which exam this question belongs to
    // ManyToOne — many questions can belong to one exam
    @ManyToOne
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public Integer getMarks() { return marks; }
    public void setMarks(Integer marks) { this.marks = marks; }

    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }
}