package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.repository.ExamAttemptRepository;
import com.examSystem.online_exam_system.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ExamAttemptService {

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamService examService;

    // ---- START EXAM ----
    // creates a new attempt for a student
    public ExamAttempt startExam(User student, Long examId) {
        Exam exam = examService.getExamById(examId);

        // check if student already attempted this exam
        if (examAttemptRepository.existsByStudentAndExam(student, exam)) {
            throw new RuntimeException("You have already attempted this exam!");
        }

        // check if exam is published
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("This exam is not available yet!");
        }

        // create a new attempt
        ExamAttempt attempt = new ExamAttempt();
        attempt.setStudent(student);
        attempt.setExam(exam);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        return examAttemptRepository.save(attempt);
    }

    // ---- SUBMIT EXAM ----
    // receives the student's answers, calculates score, saves result
    // answers is a map of questionId -> student's answer
    public ExamAttempt submitExam(Long attemptId, Map<String, String> answers) {
        ExamAttempt attempt = getAttemptById(attemptId);

        // check attempt is still in progress
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("This exam has already been submitted!");
        }

        // get all questions for this exam
        List<Question> questions = questionRepository.findByExam(attempt.getExam());

        // calculate score by comparing student answers to correct answers
        int totalScore = 0;
        for (Question question : questions) {
            // get student's answer for this question
            String studentAnswer = answers.get("answer_" + question.getId());
            if (studentAnswer != null &&
                    studentAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim())) {
                // answer is correct — add marks
                totalScore += question.getMarks();
            }
        }

        // update the attempt with score and submission time
        attempt.setTotalScore(totalScore);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.GRADED);
        return examAttemptRepository.save(attempt);
    }

    // ---- GET ATTEMPT BY ID ----
    public ExamAttempt getAttemptById(Long id) {
        return examAttemptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attempt not found!"));
    }

    // ---- GET ALL ATTEMPTS BY STUDENT ----
    public List<ExamAttempt> getAttemptsByStudent(User student) {
        return examAttemptRepository.findByStudent(student);
    }

    // ---- GET ALL ATTEMPTS FOR AN EXAM ----
    // for teachers and admins to see all students' results
    public List<ExamAttempt> getAttemptsByExam(Long examId) {
        Exam exam = examService.getExamById(examId);
        return examAttemptRepository.findByExam(exam);
    }

    // ---- GET STUDENT'S ATTEMPT FOR A SPECIFIC EXAM ----
    public ExamAttempt getAttemptByStudentAndExam(User student, Long examId) {
        Exam exam = examService.getExamById(examId);
        return examAttemptRepository.findByStudentAndExam(student, exam)
                .orElseThrow(() -> new RuntimeException("No attempt found!"));
    }
}