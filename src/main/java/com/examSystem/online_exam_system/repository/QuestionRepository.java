package com.examSystem.online_exam_system.repository;

import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // get all questions for a module
    List<Question> findByModule(Module module);

    // get questions by module and type
    // used when randomly selecting questions for a session
    List<Question> findByModuleAndQuestionType(
            Module module, QuestionType questionType);

    // count questions in a module
    long countByModule(Module module);

    // count by module and type
    long countByModuleAndQuestionType(Module module,
                                      QuestionType questionType);
}