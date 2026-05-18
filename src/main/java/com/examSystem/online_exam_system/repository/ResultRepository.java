package com.examSystem.online_exam_system.repository;

import com.examSystem.online_exam_system.model.Exam;
import com.examSystem.online_exam_system.model.ExamAttempt;
import com.examSystem.online_exam_system.model.Result;
import com.examSystem.online_exam_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByStudent(User student);
    List<Result> findByExam(Exam exam);
    Optional<Result> findByStudentAndExam(User student, Exam exam);
    Optional<Result> findByAttempt(ExamAttempt attempt);
    long countByStudentAndPassed(User student, Boolean passed);

    // results waiting for teacher review
    List<Result> findByTeacherApproved(Boolean approved);

    // results for a specific exam waiting for review
    List<Result> findByExamAndTeacherApproved(Exam exam, Boolean approved);

    // only approved results for a student (what student can see)
    List<Result> findByStudentAndTeacherApproved(User student, Boolean approved);
}