package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.Module;
import com.examSystem.online_exam_system.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleService {

    // --- Injecting Module Repository ---
    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Creates a new subject module in the system.
     * Validates that the module name is completely unique before saving.
     * (Admin restricted operation)
     */
    public Module createModule(Module module) {
        // Rule: Prevent adding duplicate modules with the exact same name
        if (moduleRepository.existsByName(module.getName())) {
            throw new RuntimeException(
                    "A module with this name already exists!");
        }
        return moduleRepository.save(module); // Persist new module to database
    }

    /**
     * Retrieves all subject modules registered in the system.
     */
    public List<Module> getAllModules() {
        return moduleRepository.findAll();
    }

    /**
     * Finds a specific module by its primary key ID.
     * Throws an exception if no matching record is found.
     */
    public Module getModuleById(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found!"));
    }

    /**
     * Updates an existing module's basic details (name and description).
     */
    public Module updateModule(Long id, Module updated) {
        Module existing = getModuleById(id); // Fetch the existing record or throw exception

        // Update fields with new incoming data
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());

        return moduleRepository.save(existing); // Save the updated entity
    }

    /**
     * Deletes a specific module from the database by its ID.
     * Verifies existence first to avoid silent failures.
     */
    public void deleteModule(Long id) {
        getModuleById(id); // Ensure the module exists first before attempting deletion
        moduleRepository.deleteById(id); // Remove the module record
    }
}