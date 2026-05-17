package com.examSystem.online_exam_system.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, message = "Title must be at least 3 characters")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "module_id")
    private Module module;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(nullable = false)
    private Integer durationMins;

    @NotNull(message = "Total marks is required")
    @Min(value = 1, message = "Total marks must be at least 1")
    @Column(nullable = false)
    private Double totalMarks;

    // how many of each question type to pick randomly
    @Column(name = "mcq_count")
    private Integer mcqCount = 0;

    @Column(name = "true_false_count")
    private Integer trueFalseCount = 0;

    @Column(name = "short_answer_count")
    private Integer shortAnswerCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status = ExamStatus.DRAFT;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // marks per question type — stored so total can be recalculated
    @Column(name = "mcq_marks_each")
    private Double mcqMarksEach = 1.0;

    @Column(name = "tf_marks_each")
    private Double tfMarksEach = 1.0;

    @Column(name = "sa_marks_each")
    private Double saMarksEach = 2.0;

    // total question count
    public int getTotalQuestionCount() {
        return (mcqCount == null ? 0 : mcqCount) +
                (trueFalseCount == null ? 0 : trueFalseCount) +
                (shortAnswerCount == null ? 0 : shortAnswerCount);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public Integer getDurationMins() { return durationMins; }
    public void setDurationMins(Integer durationMins) { this.durationMins = durationMins; }

    public Double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Double totalMarks) { this.totalMarks = totalMarks; }

    public Integer getMcqCount() { return mcqCount; }
    public void setMcqCount(Integer mcqCount) { this.mcqCount = mcqCount; }

    public Integer getTrueFalseCount() { return trueFalseCount; }
    public void setTrueFalseCount(Integer trueFalseCount) {
        this.trueFalseCount = trueFalseCount;
    }

    public Integer getShortAnswerCount() { return shortAnswerCount; }
    public void setShortAnswerCount(Integer shortAnswerCount) {
        this.shortAnswerCount = shortAnswerCount;
    }

    public ExamStatus getStatus() { return status; }
    public void setStatus(ExamStatus status) { this.status = status; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Double getMcqMarksEach() { return mcqMarksEach; }
    public void setMcqMarksEach(Double mcqMarksEach) {
        this.mcqMarksEach = mcqMarksEach;
    }

    public Double getTfMarksEach() { return tfMarksEach; }
    public void setTfMarksEach(Double tfMarksEach) {
        this.tfMarksEach = tfMarksEach;
    }

    public Double getSaMarksEach() { return saMarksEach; }
    public void setSaMarksEach(Double saMarksEach) {
        this.saMarksEach = saMarksEach;
    }
}