package com.example.bookapi.service;

import com.example.bookapi.dto.TaskDTO;
import com.example.bookapi.entity.Task;
import com.example.bookapi.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository repository;

    @Override
    public List<TaskDTO> findAll() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TaskDTO> findById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    @Override
    public TaskDTO save(TaskDTO dto) {
        Task entity = toEntity(dto);
        Task saved = repository.save(entity);
        return toDTO(saved);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private TaskDTO toDTO(Task entity) {
        TaskDTO dto = new TaskDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setCompleted(entity.getCompleted());
        return dto;
    }

    private Task toEntity(TaskDTO dto) {
        Task entity = new Task();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setCompleted(dto.getCompleted());
        return entity;
    }
}
