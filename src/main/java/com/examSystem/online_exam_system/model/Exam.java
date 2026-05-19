package com.examSystem.online_exam_system.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exams") // Specifies the database table name
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment ID
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, message = "Title must be at least 3 characters")
    @Column(nullable = false)
    private String title; // Title of the exam

    @Column(columnDefinition = "TEXT") // Allows storing long text for descriptions
    private String description; // Short overview of the exam

    @ManyToOne // Multiple exams can belong to a single module
    @JoinColumn(name = "module_id")
    private Module module; // The academic module/subject this exam belongs to

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(nullable = false)
    private Integer durationMins; // Total time allowed for the exam in minutes

    @NotNull(message = "Total marks is required")
    @Min(value = 1, message = "Total marks must be at least 1")
    @Column(nullable = false)
    private Double totalMarks; // Maximum possible marks for the exam

    // --- Question Configurations (Random Selection Count) ---

    @Column(name = "mcq_count")
    private Integer mcqCount = 0; // Number of Multiple Choice Questions to include

    @Column(name = "true_false_count")
    private Integer trueFalseCount = 0; // Number of True/False questions to include

    @Column(name = "short_answer_count")
    private Integer shortAnswerCount = 0; // Number of Short Answer questions to include

    @Enumerated(EnumType.STRING) // Stores the enum value as a String in the database
    @Column(nullable = false)
    private ExamStatus status = ExamStatus.DRAFT; // Current lifecycle state of the exam (e.g., DRAFT, PUBLISHED)

    @ManyToOne // Multiple exams can be created by a single user (e.g., Lecturer/Admin)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy; // The user who created this exam

    @Column(name = "created_at")
    private LocalDateTime createdAt; // Date and time when the exam record was created

    @PrePersist // Runs automatically right before saving the entity to the database
    protected void onCreate() {
        createdAt = LocalDateTime.now(); // Sets the current timestamp
    }

    // --- Dynamic Calculation Marks Configuration ---

    @Column(name = "mcq_marks_each")
    private Double mcqMarksEach = 1.0; // Marks allocated for each MCQ item

    @Column(name = "tf_marks_each")
    private Double tfMarksEach = 1.0; // Marks allocated for each True/False item

    @Column(name = "sa_marks_each")
    private Double saMarksEach = 2.0; // Marks allocated for each Short Answer item

    /**
     * Calculates the total number of questions configured for this exam.
     * @return Sum of all question types, handling null values safely.
     */
    public int getTotalQuestionCount() {
        return (mcqCount == null ? 0 : mcqCount) +
                (trueFalseCount == null ? 0 : trueFalseCount) +
                (shortAnswerCount == null ? 0 : shortAnswerCount);
    }

    // --- Getters and Setters ---

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