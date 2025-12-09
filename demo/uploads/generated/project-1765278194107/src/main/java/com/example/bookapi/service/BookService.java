package com.example.bookapi.service;

import com.example.bookapi.dto.BookDTO;
import java.util.List;
import java.util.Optional;

public interface BookService {
    List<BookDTO> findAll();
    Optional<BookDTO> findById(Long id);
    BookDTO save(BookDTO dto);
    void deleteById(Long id);
}
