package com.mathstrokes.question.repository;

import java.util.List;
import java.util.Optional;

import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionStatus;
import com.mathstrokes.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository
        extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    @Query("""
            select q from Question q
            left join fetch q.options
            join fetch q.chapter c
            join fetch q.subject s
            where q.id = :id
            """)
    Optional<Question> findByIdWithOptions(@Param("id") Long id);

    @Query("""
            select distinct q from Question q
            left join fetch q.options
            join fetch q.chapter
            join fetch q.subject
            where q.id in :ids
            """)
    List<Question> findAllByIdWithOptions(@Param("ids") List<Long> ids);

    /**
     * Random draw for test generation.
     *
     * ORDER BY random() is the right tool at this table size: the candidate set is one chapter of
     * one exam pattern and the supporting index keeps the scan small. Revisit only if a single
     * chapter grows past tens of thousands of published questions.
     *
     * {@code excludedIds} must never be empty - pass a sentinel such as List.of(-1L) - because
     * PostgreSQL rejects an empty IN list.
     */
    @Query(value = """
            SELECT q.id FROM questions q
            WHERE q.chapter_id = :chapterId
              AND q.exam_pattern = :examPattern
              AND q.status = 'PUBLISHED'
              AND (CAST(:difficulty AS text) IS NULL OR q.difficulty = CAST(:difficulty AS text))
              AND q.id NOT IN (:excludedIds)
            ORDER BY random()
            LIMIT :maxResults
            """, nativeQuery = true)
    List<Long> pickRandomPublishedIds(@Param("chapterId") Long chapterId,
                                      @Param("examPattern") String examPattern,
                                      @Param("difficulty") String difficulty,
                                      @Param("excludedIds") List<Long> excludedIds,
                                      @Param("maxResults") int maxResults);

    @Query("""
            select count(q) from Question q
            where q.chapter.id = :chapterId and q.examPattern = :examPattern
              and q.status = com.mathstrokes.common.enums.QuestionStatus.PUBLISHED
              and (:difficulty is null or q.difficulty = :difficulty)
            """)
    long countPublished(@Param("chapterId") Long chapterId,
                        @Param("examPattern") ExamPattern examPattern,
                        @Param("difficulty") Difficulty difficulty);

    long countByStatus(QuestionStatus status);

    @Query("""
            select count(q) from Question q
            where q.chapter.id = :chapterId
              and q.status = com.mathstrokes.common.enums.QuestionStatus.PUBLISHED
            """)
    long countPublishedInChapter(@Param("chapterId") Long chapterId);
}
