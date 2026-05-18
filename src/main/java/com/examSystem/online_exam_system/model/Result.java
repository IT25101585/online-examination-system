package com.examSystem.online_exam_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// represents the final saved result of a student's exam attempt
@Entity
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // which student this result belongs to
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // which exam this result is for
    @ManyToOne
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    // the attempt this result came from
    @OneToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @Column(nullable = false)
    private Double totalScore;

    @Column(nullable = false)
    private Double totalMarks;

    @Column(nullable = false)
    private Double percentage;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private Boolean passed;

    // whether teacher has reviewed and approved this result
    // students cannot see results until this is true
    @Column(nullable = false)
    private Boolean teacherApproved = false;

    // when the teacher approved it
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // optional teacher note on the result
    @Column(columnDefinition = "TEXT")
    private String teacherNote;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }

    public ExamAttempt getAttempt() { return attempt; }
    public void setAttempt(ExamAttempt attempt) { this.attempt = attempt; }

    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }

    public Double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Double totalMarks) { this.totalMarks = totalMarks; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }

    public Boolean getTeacherApproved() { return teacherApproved; }
    public void setTeacherApproved(Boolean teacherApproved) {
        this.teacherApproved = teacherApproved;
    }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getTeacherNote() { return teacherNote; }
    public void setTeacherNote(String teacherNote) {
        this.teacherNote = teacherNote;
    }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
