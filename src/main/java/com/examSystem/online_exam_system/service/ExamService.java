package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private SessionQuestionRepository sessionQuestionRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private QuestionRepository questionRepository;

    // ---- CREATE EXAM ----
    public Exam createExam(Exam exam, User createdBy) {
        exam.setCreatedBy(createdBy);
        exam.setStatus(ExamStatus.DRAFT);
        return examRepository.save(exam);
    }

    // ---- GET ALL EXAMS ----
    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    // ---- GET PUBLISHED EXAMS ----
    public List<Exam> getPublishedExams() {
        return examRepository.findByStatus(ExamStatus.PUBLISHED);
    }

    // ---- GET PENDING EXAMS ----
    // only admin sees these — awaiting approval
    public List<Exam> getPendingExams() {
        return examRepository.findByStatus(ExamStatus.PENDING);
    }

    // ---- GET EXAMS BY CREATOR ----
    public List<Exam> getExamsByCreator(User createdBy) {
        return examRepository.findByCreatedBy(createdBy);
    }

    // ---- GET EXAM BY ID ----
    public Exam getExamById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));
    }

    // ---- UPDATE EXAM ----
    public Exam updateExam(Long id, Exam updatedExam, Long moduleId) {
        Exam existing = getExamById(id);
        existing.setTitle(updatedExam.getTitle());
        existing.setDescription(updatedExam.getDescription());
        existing.setDurationMins(updatedExam.getDurationMins());
        existing.setTotalMarks(updatedExam.getTotalMarks());
        existing.setMcqCount(updatedExam.getMcqCount());
        existing.setTrueFalseCount(updatedExam.getTrueFalseCount());
        existing.setShortAnswerCount(updatedExam.getShortAnswerCount());
        existing.setMcqMarksEach(updatedExam.getMcqMarksEach());
        existing.setTfMarksEach(updatedExam.getTfMarksEach());
        existing.setSaMarksEach(updatedExam.getSaMarksEach());

        if (moduleId != null) {
            existing.setModule(moduleService.getModuleById(moduleId));
        } else {
            existing.setModule(null);
        }

        return examRepository.save(existing);
    }

    // ---- SUBMIT FOR APPROVAL ----
    public Exam submitForApproval(Long id) {
        Exam exam = getExamById(id);

        if (exam.getStatus() != ExamStatus.DRAFT &&
                exam.getStatus() != ExamStatus.REJECTED) {
            throw new RuntimeException(
                    "Only DRAFT or REJECTED exams can be submitted!");
        }

        if (exam.getModule() == null) {
            throw new RuntimeException(
                    "Please tag this exam to a module before submitting. " +
                            "Questions are drawn from the module's question bank.");
        }

        // check question bank has enough questions
        int mcqNeeded = exam.getMcqCount() == null ? 0 : exam.getMcqCount();
        int tfNeeded = exam.getTrueFalseCount() == null ?
                0 : exam.getTrueFalseCount();
        int saNeeded = exam.getShortAnswerCount() == null ?
                0 : exam.getShortAnswerCount();

        long mcqAvailable = questionRepository
                .countByModuleAndQuestionType(
                        exam.getModule(), QuestionType.MCQ);
        long tfAvailable = questionRepository
                .countByModuleAndQuestionType(
                        exam.getModule(), QuestionType.TRUE_FALSE);
        long saAvailable = questionRepository
                .countByModuleAndQuestionType(
                        exam.getModule(), QuestionType.SHORT_ANSWER);

        StringBuilder errors = new StringBuilder();
        if (mcqAvailable < mcqNeeded) {
            errors.append("Not enough MCQ questions in module '")
                    .append(exam.getModule().getName())
                    .append("'. Need ").append(mcqNeeded)
                    .append(", have ").append(mcqAvailable).append(". ");
        }
        if (tfAvailable < tfNeeded) {
            errors.append("Not enough True/False questions. Need ")
                    .append(tfNeeded).append(", have ")
                    .append(tfAvailable).append(". ");
        }
        if (saAvailable < saNeeded) {
            errors.append("Not enough Short Answer questions. Need ")
                    .append(saNeeded).append(", have ")
                    .append(saAvailable).append(". ");
        }

        if (errors.length() > 0) {
            throw new RuntimeException(errors.toString().trim());
        }

        if (mcqNeeded == 0 && tfNeeded == 0 && saNeeded == 0) {
            throw new RuntimeException(
                    "Please set at least one question type count " +
                            "before submitting.");
        }

        exam.setStatus(ExamStatus.PENDING);
        return examRepository.save(exam);
    }

    // ---- APPROVE EXAM (admin only) ----
    // changes status from PENDING to PUBLISHED
    public Exam approveExam(Long id) {
        Exam exam = getExamById(id);
        if (exam.getStatus() != ExamStatus.PENDING) {
            throw new RuntimeException("Only PENDING exams can be approved!");
        }
        exam.setStatus(ExamStatus.PUBLISHED);
        return examRepository.save(exam);
    }

    // ---- REJECT EXAM (admin only) ----
    // changes status from PENDING back to REJECTED
    public Exam rejectExam(Long id) {
        Exam exam = getExamById(id);
        if (exam.getStatus() != ExamStatus.PENDING) {
            throw new RuntimeException("Only PENDING exams can be rejected!");
        }
        exam.setStatus(ExamStatus.REJECTED);
        return examRepository.save(exam);
    }

    // ---- FORCE UNPUBLISH (admin only) ----
    // admin can pull any published exam at any time
    public Exam forceUnpublish(Long id) {
        Exam exam = getExamById(id);
        exam.setStatus(ExamStatus.DRAFT);
        return examRepository.save(exam);
    }

    // ---- DELETE EXAM ----
// must delete all related records first due to foreign key constraints
    @Transactional
    public void deleteExam(Long id) {
        Exam exam = getExamById(id);

        // delete session questions first, then sessions
        List<ExamSession> sessions = examSessionRepository.findByExam(exam);
        for (ExamSession session : sessions) {
            List<SessionQuestion> sessionQuestions =
                    sessionQuestionRepository.findBySession(session);
            sessionQuestionRepository.deleteAll(sessionQuestions);
        }
        examSessionRepository.deleteAll(sessions);

        // delete results
        resultRepository.deleteAll(resultRepository.findByExam(exam));

        // delete attempts
        examAttemptRepository.deleteAll(
                examAttemptRepository.findByExam(exam));

        // now safe to delete the exam
        examRepository.deleteById(id);
    }
}