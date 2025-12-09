package com.example.bookapi.service;

import com.example.bookapi.dto.BookDTO;
import com.example.bookapi.entity.Book;
import com.example.bookapi.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository repository;

    @Override
    public List<BookDTO> findAll() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BookDTO> findById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    @Override
    public BookDTO save(BookDTO dto) {
        Book entity = toEntity(dto);
        Book saved = repository.save(entity);
        return toDTO(saved);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private BookDTO toDTO(Book entity) {
        BookDTO dto = new BookDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setAuthor(entity.getAuthor());
        return dto;
    }

    private Book toEntity(BookDTO dto) {
        Book entity = new Book();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setAuthor(dto.getAuthor());
        return entity;
    }
}
