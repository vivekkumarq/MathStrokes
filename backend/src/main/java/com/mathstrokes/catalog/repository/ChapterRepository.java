package com.mathstrokes.catalog.repository;

import java.util.List;

import com.mathstrokes.catalog.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    @Query("""
            select c from Chapter c join fetch c.subject s
            where s.id = :subjectId and c.active = true and s.active = true
            order by c.displayOrder asc, c.name asc
            """)
    List<Chapter> findActiveBySubject(@Param("subjectId") Long subjectId);

    @Query("""
            select c from Chapter c join fetch c.subject s
            where (:subjectId is null or s.id = :subjectId)
            order by s.displayOrder asc, c.displayOrder asc, c.name asc
            """)
    List<Chapter> findAllForAdmin(@Param("subjectId") Long subjectId);

    boolean existsBySubjectIdAndNameIgnoreCase(Long subjectId, String name);

    long countBySubjectId(Long subjectId);
}
