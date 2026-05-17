package com.examSystem.online_exam_system.repository;

import com.examSystem.online_exam_system.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    // check if module name already exists
    boolean existsByName(String name);

    // find by name
    Optional<Module> findByName(String name);
}