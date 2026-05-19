package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ExamService {

    // --- Injecting Repositories and Services ---
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

    /**
     * Creates a new exam and sets its initial status to DRAFT.
     * @Transactional ensures the operation rolls back if any database error occurs.
     */
    @Transactional
    public Exam createExam(Exam exam, User createdBy) {
        exam.setCreatedBy(createdBy); // Assign the lecturer/admin who created it
        exam.setStatus(ExamStatus.DRAFT); // Set default status as DRAFT
        return examRepository.save(exam); // Save to the database
    }

    /**
     * Retrieves all exams available in the system.
     */
    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    /**
     * Retrieves only the exams that are currently live/published for students.
     */
    public List<Exam> getPublishedExams() {
        return examRepository.findByStatus(ExamStatus.PUBLISHED);
    }

    /**
     * Retrieves exams waiting for admin approval.
     */
    public List<Exam> getPendingExams() {
        return examRepository.findByStatus(ExamStatus.PENDING);
    }

    /**
     * Retrieves all exams created by a specific lecturer/user.
     */
    public List<Exam> getExamsByCreator(User createdBy) {
        return examRepository.findByCreatedBy(createdBy);
    }

    /**
     * Finds an exam by its ID, throws an exception if not found.
     */
    public Exam getExamById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));
    }

    /**
     * Updates an existing exam's details and links it to a module if provided.
     */
    @Transactional
    public Exam updateExam(Long id, Exam updatedExam, Long moduleId) {
        Exam existing = getExamById(id); // Find the existing exam record

        // Map updated fields to the existing entity
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

        // Update module reference if moduleId is provided
        if (moduleId != null) {
            existing.setModule(moduleService.getModuleById(moduleId));
        } else {
            existing.setModule(null);
        }

        return examRepository.save(existing); // Save updated details
    }

    /**
     * Validates and submits a DRAFT exam to an admin for approval.
     * Checks if the module has enough questions before changing status to PENDING.
     */
    @Transactional
    public Exam submitForApproval(Long id) {
        Exam exam = getExamById(id);

        // Rule 1: Validate current exam status
        if (exam.getStatus() != ExamStatus.DRAFT &&
                exam.getStatus() != ExamStatus.REJECTED &&
                exam.getStatus() != ExamStatus.UNPUBLISHED) {
            throw new RuntimeException(
                    "Only DRAFT, REJECTED or UNPUBLISHED exams can be submitted!");
        }

        // Rule 2: Ensure the exam belongs to a specific module
        if (exam.getModule() == null) {
            throw new RuntimeException(
                    "Please tag this exam to a module before submitting. " +
                            "Questions are drawn from the module's question bank.");
        }

        // Get required question counts safely (handles null values)
        int mcqNeeded = exam.getMcqCount() == null ? 0 : exam.getMcqCount();
        int tfNeeded = exam.getTrueFalseCount() == null ?
                0 : exam.getTrueFalseCount();
        int saNeeded = exam.getShortAnswerCount() == null ?
                0 : exam.getShortAnswerCount();

        // Check actual question availability in the module's question bank
        long mcqAvailable = questionRepository
                .countByModuleAndQuestionType(
                        exam.getModule(), QuestionType.MCQ);
        long tfAvailable = questionRepository
                .countByModuleAndQuestionType(
                        exam.getModule(), QuestionType.TRUE_FALSE);
        long saAvailable = questionRepository
                .countByModuleAndQuestionType(
                        exam.getModule(), QuestionType.SHORT_ANSWER);

        // Rule 3: Validate if there are enough available questions to pick from randomly
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

        // Throw error if any validation check fails
        if (errors.length() > 0) {
            throw new RuntimeException(errors.toString().trim());
        }

        // Rule 4: Total questions configured must be greater than zero
        if (mcqNeeded == 0 && tfNeeded == 0 && saNeeded == 0) {
            throw new RuntimeException(
                    "Please set at least one question type count " +
                            "before submitting.");
        }

        exam.setStatus(ExamStatus.PENDING); // Promote status to PENDING
        return examRepository.save(exam);
    }

    /**
     * Approves a PENDING exam, making it PUBLISHED and live for students.
     * (Admin restricted operation)
     */
    @Transactional
    public Exam approveExam(Long id) {
        Exam exam = getExamById(id);
        if (exam.getStatus() != ExamStatus.PENDING) {
            throw new RuntimeException("Only PENDING exams can be approved!");
        }
        exam.setStatus(ExamStatus.PUBLISHED);
        return examRepository.save(exam);
    }

    /**
     * Rejects a PENDING exam request.
     * (Admin restricted operation)
     */
    @Transactional
    public Exam rejectExam(Long id) {
        Exam exam = getExamById(id);
        if (exam.getStatus() != ExamStatus.PENDING) {
            throw new RuntimeException("Only PENDING exams can be rejected!");
        }
        exam.setStatus(ExamStatus.REJECTED);
        return examRepository.save(exam);
    }

    /**
     * Forces a published exam to become hidden/unpublished from the student view.
     * (Admin restricted operation)
     */
    @Transactional
    public Exam forceUnpublish(Long id) {
        Exam exam = getExamById(id);
        exam.setStatus(ExamStatus.UNPUBLISHED);
        return examRepository.save(exam);
    }

    /**
     * Deletes an exam completely along with all its related dependencies cascadingly.
     * (Removes session questions, exam sessions, results, and attempts first to prevent foreign key errors)
     */
    @Transactional
    public void deleteExam(Long id) {
        Exam exam = getExamById(id);

        // Step 1: Find and delete all session-specific questions tied to this exam's sessions
        List<ExamSession> sessions = examSessionRepository.findByExam(exam);
        for (ExamSession session : sessions) {
            List<SessionQuestion> sessionQuestions =
                    sessionQuestionRepository.findBySession(session);
            sessionQuestionRepository.deleteAll(sessionQuestions);
        }

        // Step 2: Delete all student exam sessions linked to this exam
        examSessionRepository.deleteAll(sessions);

        // Step 3: Delete calculated final results records for this exam
        resultRepository.deleteAll(resultRepository.findByExam(exam));

        // Step 4: Delete student attempt histories tracking this exam
        examAttemptRepository.deleteAll(
                examAttemptRepository.findByExam(exam));

        // Step 5: Finally, remove the main exam record itself safely
        examRepository.deleteById(id);
    }
}