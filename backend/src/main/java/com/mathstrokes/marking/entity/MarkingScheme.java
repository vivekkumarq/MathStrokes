package com.mathstrokes.marking.entity;

import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A named, versionable set of marking rules bound to an (examPattern, questionType) pair.
 * At most one scheme per pair may be active, enforced by a partial unique index, so resolution
 * during test generation is deterministic.
 */
@Entity
@Table(name = "marking_schemes")
@Getter
@Setter
@NoArgsConstructor
public class MarkingScheme extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_pattern", nullable = false, length = 30)
    private ExamPattern examPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 40)
    private QuestionType questionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", nullable = false, columnDefinition = "jsonb")
    private MarkingConfig configuration;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
