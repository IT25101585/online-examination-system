package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.model.Module;
import com.examSystem.online_exam_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    // ---- ADD QUESTION TO MODULE ----
    public Question addQuestion(Question question, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found!"));
        question.setModule(module);
        return questionRepository.save(question);
    }

    // ---- GET ALL QUESTIONS FOR A MODULE ----
    public List<Question> getQuestionsByModule(Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found!"));
        return questionRepository.findByModule(module);
    }

    // ---- GET QUESTIONS BY MODULE AND TYPE ----
    public List<Question> getQuestionsByModuleAndType(
            Long moduleId, QuestionType type) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found!"));
        return questionRepository.findByModuleAndQuestionType(module, type);
    }

    // ---- COUNT QUESTIONS IN MODULE ----
    public long countQuestions(Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found!"));
        return questionRepository.countByModule(module);
    }

    // ---- COUNT BY MODULE AND TYPE ----
    public long countByModuleAndType(Long moduleId, QuestionType type) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found!"));
        return questionRepository.countByModuleAndQuestionType(module, type);
    }

    // ---- GET QUESTION BY ID ----
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found!"));
    }

    // ---- UPDATE QUESTION ----
    public Question updateQuestion(Long id, Question updated,
                                   Long moduleId) {
        Question existing = getQuestionById(id);
        existing.setQuestionText(updated.getQuestionText());
        existing.setQuestionType(updated.getQuestionType());
        existing.setOptionA(updated.getOptionA());
        existing.setOptionB(updated.getOptionB());
        existing.setOptionC(updated.getOptionC());
        existing.setOptionD(updated.getOptionD());
        existing.setCorrectAnswer(updated.getCorrectAnswer());
        existing.setMarks(updated.getMarks());

        if (moduleId != null) {
            Module module = moduleRepository.findById(moduleId)
                    .orElseThrow(() ->
                            new RuntimeException("Module not found!"));
            existing.setModule(module);
        }

        return questionRepository.save(existing);
    }

    // ---- DELETE QUESTION ----
    public void deleteQuestion(Long id) {
        getQuestionById(id);
        questionRepository.deleteById(id);
    }
}