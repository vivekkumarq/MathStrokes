package com.mathstrokes.exam.repository;

import java.util.List;
import java.util.Optional;

import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestStatus;
import com.mathstrokes.exam.entity.ExamTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamTestRepository extends JpaRepository<ExamTest, Long> {

    @Query("""
            select t from ExamTest t
            left join fetch t.chapter c
            join fetch t.subject s
            where t.id = :id
            """)
    Optional<ExamTest> findByIdWithCatalog(@Param("id") Long id);

    /** Student-facing browse: published tests only, optionally narrowed by chapter and pattern. */
    @Query("""
            select t from ExamTest t
            left join fetch t.chapter c
            join fetch t.subject s
            where t.status = com.mathstrokes.common.enums.TestStatus.PUBLISHED
              and (:chapterId is null or c.id = :chapterId)
              and (:examPattern is null or t.examPattern = :examPattern)
              and (:subjectId is null or s.id = :subjectId)
            order by t.publishedAt desc, t.id desc
            """)
    List<ExamTest> findPublished(@Param("subjectId") Long subjectId,
                                 @Param("chapterId") Long chapterId,
                                 @Param("examPattern") ExamPattern examPattern);

    @Query(value = """
            select t from ExamTest t
            left join fetch t.chapter c
            join fetch t.subject s
            where (:status is null or t.status = :status)
              and (:chapterId is null or c.id = :chapterId)
              and (:examPattern is null or t.examPattern = :examPattern)
            """,
            countQuery = """
            select count(t) from ExamTest t
            where (:status is null or t.status = :status)
              and (:chapterId is null or t.chapter.id = :chapterId)
              and (:examPattern is null or t.examPattern = :examPattern)
            """)
    Page<ExamTest> findForAdmin(@Param("status") TestStatus status,
                                @Param("chapterId") Long chapterId,
                                @Param("examPattern") ExamPattern examPattern,
                                Pageable pageable);

    long countByStatus(TestStatus status);
}
