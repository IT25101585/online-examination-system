package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExamAttemptService {

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private SessionQuestionRepository sessionQuestionRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    // ---- START EXAM ----
    // in ExamAttemptService.java — update startExam
    public ExamAttempt startExam(User student, Long examId,
                                 Long sessionId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));

        // check per SESSION not per exam
        if (examAttemptRepository
                .existsByStudentAndExamAndExamSessionId(
                        student, exam, sessionId)) {
            throw new RuntimeException(
                    "You have already attempted this session!");
        }

        ExamAttempt attempt = new ExamAttempt();
        attempt.setStudent(student);
        attempt.setExam(exam);
        attempt.setExamSessionId(sessionId);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        return examAttemptRepository.save(attempt);
    }

    // ---- SUBMIT EXAM ----
    // scores based on session questions, not exam questions
    public ExamAttempt submitExam(Long attemptId,
                                  Map<String, String> answers) {
        ExamAttempt attempt = getAttemptById(attemptId);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException(
                    "This exam has already been submitted!");
        }

        // get session questions for scoring
        int totalScore = 0;

        if (attempt.getExamSessionId() != null) {
            ExamSession examSession = examSessionRepository
                    .findById(attempt.getExamSessionId())
                    .orElse(null);

            if (examSession != null) {
                List<SessionQuestion> questions =
                        sessionQuestionRepository
                                .findBySession(examSession);

                for (SessionQuestion q : questions) {
                    String studentAnswer =
                            answers.get("answer_" + q.getId());
                    if (studentAnswer != null &&
                            studentAnswer.trim().equalsIgnoreCase(
                                    q.getCorrectAnswer().trim())) {
                        totalScore += q.getMarks();
                    }
                }
            }
        }

        attempt.setTotalScore((double) totalScore);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.GRADED);
        return examAttemptRepository.save(attempt);
    }

    // ---- GET ATTEMPT BY ID ----
    public ExamAttempt getAttemptById(Long id) {
        return examAttemptRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Attempt not found!"));
    }

    // ---- GET ATTEMPTS BY STUDENT ----
    public List<ExamAttempt> getAttemptsByStudent(User student) {
        return examAttemptRepository.findByStudent(student);
    }

    // ---- GET ATTEMPTS BY EXAM ----
    public List<ExamAttempt> getAttemptsByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new RuntimeException("Exam not found!"));
        return examAttemptRepository.findByExam(exam);
    }
}
