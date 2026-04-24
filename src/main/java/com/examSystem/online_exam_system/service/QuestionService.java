package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.Exam;
import com.examSystem.online_exam_system.model.Question;
import com.examSystem.online_exam_system.model.QuestionType;
import com.examSystem.online_exam_system.repository.ExamRepository;
import com.examSystem.online_exam_system.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamRepository examRepository;

    // ---- ADD QUESTION TO EXAM ----
    public Question addQuestion(Question question, Long examId) {
        // find the exam this question belongs to
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));
        question.setExam(exam);
        return questionRepository.save(question);
    }

    // ---- GET ALL QUESTIONS FOR AN EXAM ----
    public List<Question> getQuestionsByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));
        return questionRepository.findByExam(exam);
    }

    // ---- GET QUESTION BY ID ----
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found!"));
    }

    // ---- GET QUESTIONS BY TYPE ----
    public List<Question> getQuestionsByType(Long examId, QuestionType type) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));
        return questionRepository.findByExamAndQuestionType(exam, type);
    }

    // ---- COUNT QUESTIONS IN EXAM ----
    public long countQuestions(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));
        return questionRepository.countByExam(exam);
    }

    // ---- UPDATE QUESTION ----
    public Question updateQuestion(Long id, Question updatedQuestion) {
        Question existing = getQuestionById(id);
        existing.setQuestionText(updatedQuestion.getQuestionText());
        existing.setQuestionType(updatedQuestion.getQuestionType());
        existing.setOptionA(updatedQuestion.getOptionA());
        existing.setOptionB(updatedQuestion.getOptionB());
        existing.setOptionC(updatedQuestion.getOptionC());
        existing.setOptionD(updatedQuestion.getOptionD());
        existing.setCorrectAnswer(updatedQuestion.getCorrectAnswer());
        existing.setMarks(updatedQuestion.getMarks());
        return questionRepository.save(existing);
    }

    // ---- DELETE QUESTION ----
    public void deleteQuestion(Long id) {
        getQuestionById(id);
        questionRepository.deleteById(id);
    }
}