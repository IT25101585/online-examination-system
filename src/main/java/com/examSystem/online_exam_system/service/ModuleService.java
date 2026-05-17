package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.Module;
import com.examSystem.online_exam_system.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleService {

    @Autowired
    private ModuleRepository moduleRepository;

    // ---- CREATE MODULE (admin only) ----
    public Module createModule(Module module) {
        if (moduleRepository.existsByName(module.getName())) {
            throw new RuntimeException(
                    "A module with this name already exists!");
        }
        return moduleRepository.save(module);
    }

    // ---- GET ALL MODULES ----
    public List<Module> getAllModules() {
        return moduleRepository.findAll();
    }

    // ---- GET MODULE BY ID ----
    public Module getModuleById(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found!"));
    }

    // ---- UPDATE MODULE ----
    public Module updateModule(Long id, Module updated) {
        Module existing = getModuleById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        return moduleRepository.save(existing);
    }

    // ---- DELETE MODULE ----
    public void deleteModule(Long id) {
        getModuleById(id);
        moduleRepository.deleteById(id);
    }
}