package com.example.bookapi.service;

import com.example.bookapi.dto.TaskDTO;
import java.util.List;
import java.util.Optional;

public interface TaskService {
    List<TaskDTO> findAll();
    Optional<TaskDTO> findById(Long id);
    TaskDTO save(TaskDTO dto);
    void deleteById(Long id);
}
