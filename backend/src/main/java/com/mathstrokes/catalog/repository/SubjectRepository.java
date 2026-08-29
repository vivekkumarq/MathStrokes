package com.mathstrokes.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.mathstrokes.catalog.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Subject> findAllByOrderByDisplayOrderAscNameAsc();

    Optional<Subject> findByCode(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);
}
