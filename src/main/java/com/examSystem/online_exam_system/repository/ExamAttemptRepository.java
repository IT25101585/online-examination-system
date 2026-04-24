package com.examSystem.online_exam_system.repository;

import com.examSystem.online_exam_system.model.Exam;
import com.examSystem.online_exam_system.model.ExamAttempt;
import com.examSystem.online_exam_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    // gets all attempts by a specific student
    List<ExamAttempt> findByStudent(User student);

    // gets all attempts for a specific exam
    List<ExamAttempt> findByExam(Exam exam);

    // gets a specific student's attempt for a specific exam
    Optional<ExamAttempt> findByStudentAndExam(User student, Exam exam);

    // checks if a student has already attempted an exam
    boolean existsByStudentAndExam(User student, Exam exam);
}