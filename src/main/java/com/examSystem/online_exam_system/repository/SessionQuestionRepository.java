package com.examSystem.online_exam_system.repository;

import com.examSystem.online_exam_system.model.ExamSession;
import com.examSystem.online_exam_system.model.SessionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionQuestionRepository
        extends JpaRepository<SessionQuestion, Long> {

    List<SessionQuestion> findBySession(ExamSession session);

    void deleteBySession(ExamSession session);
}

