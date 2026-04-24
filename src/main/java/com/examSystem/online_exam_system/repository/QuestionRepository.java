package com.examSystem.online_exam_system.repository;

import com.examSystem.online_exam_system.model.Exam;
import com.examSystem.online_exam_system.model.Question;
import com.examSystem.online_exam_system.model.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // gets all questions belonging to a specific exam
    List<Question> findByExam(Exam exam);

    // gets all questions of a specific type in an exam
    // e.g. all MCQ questions in exam 1
    List<Question> findByExamAndQuestionType(Exam exam, QuestionType questionType);

    // counts how many questions an exam has
    long countByExam(Exam exam);
}