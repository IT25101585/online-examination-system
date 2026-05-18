package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.model.Module;
import com.examSystem.online_exam_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamSessionService {

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private SessionQuestionRepository sessionQuestionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamRepository examRepository;

    // ---- CREATE SESSION ----
    public ExamSession createSession(Long examId,
                                     LocalDateTime startTime,
                                     String label) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));

        ExamSession session = new ExamSession();
        session.setExam(exam);
        session.setStartTime(startTime);
        session.setEndTime(
                startTime.plusMinutes(exam.getDurationMins()));
        session.setLabel(label);
        session.setStatus(SessionStatus.SCHEDULED);

        ExamSession saved = examSessionRepository.save(session);

        // try to generate questions — fail silently if not enough
        try {
            generateSessionQuestions(saved);
        } catch (RuntimeException e) {
            System.out.println("Warning generating questions: "
                    + e.getMessage());
        }

        return saved;
    }

    // ---- GENERATE SESSION QUESTIONS ----
    @Transactional
    public void generateSessionQuestions(ExamSession session) {
        // delete existing questions for this session first
        List<SessionQuestion> existing =
                sessionQuestionRepository.findBySession(session);
        sessionQuestionRepository.deleteAll(existing);
        sessionQuestionRepository.flush();

        Exam exam = session.getExam();

        if (exam.getModule() == null) {
            throw new RuntimeException(
                    "Exam must be tagged to a module!");
        }

        Module module = exam.getModule();

        List<Question> mcqs = questionRepository
                .findByModuleAndQuestionType(module, QuestionType.MCQ);
        List<Question> trueFalse = questionRepository
                .findByModuleAndQuestionType(
                        module, QuestionType.TRUE_FALSE);
        List<Question> shortAnswers = questionRepository
                .findByModuleAndQuestionType(
                        module, QuestionType.SHORT_ANSWER);

        Collections.shuffle(mcqs);
        Collections.shuffle(trueFalse);
        Collections.shuffle(shortAnswers);

        int mcqCount = exam.getMcqCount() == null ?
                0 : exam.getMcqCount();
        int tfCount = exam.getTrueFalseCount() == null ?
                0 : exam.getTrueFalseCount();
        int saCount = exam.getShortAnswerCount() == null ?
                0 : exam.getShortAnswerCount();

        if (mcqs.size() < mcqCount)
            throw new RuntimeException("Not enough MCQ questions in '"
                    + module.getName() + "'! Need " + mcqCount
                    + ", have " + mcqs.size());
        if (trueFalse.size() < tfCount)
            throw new RuntimeException(
                    "Not enough True/False questions in '"
                            + module.getName() + "'! Need " + tfCount
                            + ", have " + trueFalse.size());
        if (shortAnswers.size() < saCount)
            throw new RuntimeException(
                    "Not enough Short Answer questions in '"
                            + module.getName() + "'! Need " + saCount
                            + ", have " + shortAnswers.size());

        List<Question> selected = new ArrayList<>();
        if (mcqCount > 0) selected.addAll(mcqs.subList(0, mcqCount));
        if (tfCount > 0)
            selected.addAll(trueFalse.subList(0, tfCount));
        if (saCount > 0)
            selected.addAll(shortAnswers.subList(0, saCount));
        Collections.shuffle(selected);

        for (Question q : selected) {
            SessionQuestion sq = new SessionQuestion();
            sq.setSession(session);
            sq.setOriginalQuestion(q);
            sq.setQuestionText(q.getQuestionText());
            sq.setQuestionType(q.getQuestionType());
            sq.setOptionA(q.getOptionA());
            sq.setOptionB(q.getOptionB());
            sq.setOptionC(q.getOptionC());
            sq.setOptionD(q.getOptionD());
            sq.setCorrectAnswer(q.getCorrectAnswer());
            sq.setMarks(q.getMarks());
            sessionQuestionRepository.save(sq);
        }
    }

    // ---- REGENERATE QUESTIONS ----
    @Transactional
    public ExamSession regenerateQuestions(Long sessionId) {
        ExamSession session = getSessionById(sessionId);
        generateSessionQuestions(session);
        return session;
    }

    // ---- GET SESSION BY ID ----
    public ExamSession getSessionById(Long id) {
        return examSessionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Session not found!"));
    }

    // ---- GET SESSIONS BY EXAM ----
    public List<ExamSession> getSessionsByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));
        return examSessionRepository.findByExam(exam);
    }

    // ---- GET SESSION QUESTIONS ----
    public List<SessionQuestion> getSessionQuestions(Long sessionId) {
        ExamSession session = getSessionById(sessionId);
        return sessionQuestionRepository.findBySession(session);
    }

    // ---- GET SESSION QUESTION BY ID ----
    public SessionQuestion getSessionQuestionById(Long id) {
        return sessionQuestionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Question not found!"));
    }

    // ---- UPDATE SESSION QUESTION ----
    public SessionQuestion updateSessionQuestion(Long id,
                                                 SessionQuestion updated) {
        SessionQuestion existing = getSessionQuestionById(id);
        existing.setQuestionText(updated.getQuestionText());
        existing.setOptionA(updated.getOptionA());
        existing.setOptionB(updated.getOptionB());
        existing.setOptionC(updated.getOptionC());
        existing.setOptionD(updated.getOptionD());
        existing.setCorrectAnswer(updated.getCorrectAnswer());
        existing.setMarks(updated.getMarks());
        return sessionQuestionRepository.save(existing);
    }

    // ---- AUTO CLOSE EXPIRED SESSIONS ----
    public void autoCloseSessions() {
        List<ExamSession> scheduled =
                examSessionRepository.findByStatusAndEndTimeBefore(
                        SessionStatus.SCHEDULED, LocalDateTime.now());
        List<ExamSession> active =
                examSessionRepository.findByStatusAndEndTimeBefore(
                        SessionStatus.ACTIVE, LocalDateTime.now());
        for (ExamSession s : scheduled) {
            s.setStatus(SessionStatus.CLOSED);
            examSessionRepository.save(s);
        }
        for (ExamSession s : active) {
            s.setStatus(SessionStatus.CLOSED);
            examSessionRepository.save(s);
        }
    }

    // ---- CHECK IF SESSION IS CLOSED ----
    public boolean isSessionClosed(Long sessionId) {
        ExamSession session = getSessionById(sessionId);
        if (session.isOver() &&
                session.getStatus() != SessionStatus.CLOSED) {
            session.setStatus(SessionStatus.CLOSED);
            examSessionRepository.save(session);
        }
        return session.getStatus() == SessionStatus.CLOSED;
    }

    // ---- DELETE SESSION ----
    @Transactional
    public void deleteSession(Long id) {
        ExamSession session = getSessionById(id);
        List<SessionQuestion> questions =
                sessionQuestionRepository.findBySession(session);
        sessionQuestionRepository.deleteAll(questions);
        examSessionRepository.delete(session);
    }
}