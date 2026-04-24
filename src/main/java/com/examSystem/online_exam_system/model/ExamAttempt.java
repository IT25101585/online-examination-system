package com.examSystem.online_exam_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// represents one student's attempt at one exam
@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // which student is attempting
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // which exam is being attempted
    @ManyToOne
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    // when the student started the exam
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    // when the student submitted the exam
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // current status of this attempt
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    // total score after grading
    @Column
    private Integer totalScore = 0;

    // automatically sets startedAt when attempt is first created
    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus status) { this.status = status; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
}