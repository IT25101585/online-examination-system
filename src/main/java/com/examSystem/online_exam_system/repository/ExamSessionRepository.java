package com.examSystem.online_exam_system.repository;

import com.examSystem.online_exam_system.model.Exam;
import com.examSystem.online_exam_system.model.ExamSession;
import com.examSystem.online_exam_system.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

    // all sessions for a specific exam
    List<ExamSession> findByExam(Exam exam);

    // all sessions with a specific status
    List<ExamSession> findByStatus(SessionStatus status);

    // find sessions that have started but not yet ended
    // used to auto-close sessions
    List<ExamSession> findByStatusAndEndTimeBefore(
            SessionStatus status, LocalDateTime now);
}
