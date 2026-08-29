package com.mathstrokes.marking.dto;

import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.marking.entity.MarkingConfig;

public record MarkingSchemeResponse(Long id, String name, String description,
                                    ExamPattern examPattern, QuestionType questionType,
                                    MarkingConfig configuration, boolean active) {
}
