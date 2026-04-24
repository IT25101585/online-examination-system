package com.examSystem.online_exam_system.repository;

import com.examSystem.online_exam_system.model.Exam;
import com.examSystem.online_exam_system.model.ExamStatus;
import com.examSystem.online_exam_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    // finds all exams created by a specific user (teacher/admin)
    List<Exam> findByCreatedBy(User createdBy);

    // finds all exams with a specific status
    // e.g. findByStatus(ExamStatus.PUBLISHED) gets all published exams
    List<Exam> findByStatus(ExamStatus status);

    // finds all published exams created by a specific user
    List<Exam> findByCreatedByAndStatus(User createdBy, ExamStatus status);
}