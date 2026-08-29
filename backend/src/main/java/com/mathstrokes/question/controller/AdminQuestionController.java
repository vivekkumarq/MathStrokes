package com.mathstrokes.question.controller;

import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionStatus;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.question.dto.QuestionRequest;
import com.mathstrokes.question.dto.QuestionResponse;
import com.mathstrokes.question.dto.QuestionSummaryResponse;
import com.mathstrokes.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Question authoring. Every response here includes the answer key, which is exactly why the
 * whole path sits behind ROLE_ADMIN.
 */
@RestController
@RequestMapping("/admin/questions")
@Tag(name = "Admin - Questions", description = "LaTeX question bank authoring")
public class AdminQuestionController {

    private final QuestionService questionService;

    public AdminQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    @Operation(summary = "Search the question bank",
            description = "Paginated and filterable. Returns summaries without option content.")
    public PageResponse<QuestionSummaryResponse> search(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) ExamPattern examPattern,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return questionService.search(subjectId, chapterId, examPattern, difficulty, questionType,
                status, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one question with its options and solution")
    public QuestionResponse get(@PathVariable Long id) {
        return questionService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a question as a draft")
    public QuestionResponse create(@Valid @RequestBody QuestionRequest request) {
        return questionService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a question",
            description = "Historical attempts are unaffected: each attempt keeps its own snapshot.")
    public QuestionResponse update(@PathVariable Long id,
                                   @Valid @RequestBody QuestionRequest request) {
        return questionService.update(id, request);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a question so tests may draw on it")
    public QuestionResponse publish(@PathVariable Long id) {
        return questionService.publish(id);
    }

    @PostMapping("/{id}/draft")
    @Operation(summary = "Return a published question to draft")
    public QuestionResponse revertToDraft(@PathVariable Long id) {
        return questionService.revertToDraft(id);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a question",
            description = "Withdraws it from future tests. Existing attempts keep working.")
    public QuestionResponse archive(@PathVariable Long id) {
        return questionService.archive(id);
    }
}
