package com.example.bookapi.controller;

import com.example.bookapi.dto.TaskDTO;
import com.example.bookapi.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
public class TaskControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService service;

    @Test
    public void testGetAllTask() throws Exception {
        // TODO: Implement test logic
    }

    @Test
    public void testGetTaskById() throws Exception {
        // TODO: Implement test logic
    }

    @Test
    public void testCreateTask() throws Exception {
        // TODO: Implement test logic
    }
}
