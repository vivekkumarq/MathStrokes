package com.mathstrokes.attempt.controller;

import java.util.List;

import com.mathstrokes.attempt.dto.ActiveAttemptResponse;
import com.mathstrokes.attempt.dto.AttemptResultResponse;
import com.mathstrokes.attempt.dto.AttemptSummaryResponse;
import com.mathstrokes.attempt.dto.QuestionReviewResponse;
import com.mathstrokes.attempt.dto.SaveAnswerRequest;
import com.mathstrokes.attempt.dto.SaveAnswerResponse;
import com.mathstrokes.attempt.dto.StartAttemptRequest;
import com.mathstrokes.attempt.service.AnswerService;
import com.mathstrokes.attempt.service.AttemptService;
import com.mathstrokes.attempt.service.ResultService;
import com.mathstrokes.attempt.service.SubmissionService;
import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.security.service.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The examination API.
 *
 * No endpoint here takes a student id. The caller's identity always comes from the security
 * context, and every attempt is ownership-checked in the service layer, so passing somebody
 * else's attempt id gets a 403 rather than their paper.
 */
@RestController
@RequestMapping("/attempts")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Attempts", description = "Taking a test: start, answer, submit, review")
public class AttemptController {

    private final AttemptService attemptService;
    private final AnswerService answerService;
    private final SubmissionService submissionService;
    private final ResultService resultService;

    public AttemptController(AttemptService attemptService, AnswerService answerService,
                             SubmissionService submissionService, ResultService resultService) {
        this.attemptService = attemptService;
        this.answerService = answerService;
        this.submissionService = submissionService;
        this.resultService = resultService;
    }

    @PostMapping
    @Operation(summary = "Start a test, or resume the one already in progress",
            description = "Returns the whole paper plus the server clock. Calling it again while "
                    + "an attempt is live resumes that attempt rather than starting a new one, so "
                    + "a refresh never redraws the questions or restarts the timer.")
    public ActiveAttemptResponse start(@Valid @RequestBody StartAttemptRequest request) {
        return attemptService.startOrResume(SecurityUtils.requireUserId(), request.testId());
    }

    @GetMapping("/active")
    @Operation(summary = "The attempt currently in progress, if any",
            description = "204 when the student has nothing in flight.")
    public ResponseEntity<ActiveAttemptResponse> active() {
        return attemptService.findActiveAttempt(SecurityUtils.requireUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{attemptId}")
    @Operation(summary = "Reload an attempt in progress")
    public ActiveAttemptResponse get(@PathVariable Long attemptId) {
        return attemptService.getActiveAttempt(attemptId, SecurityUtils.requireUserId());
    }

    @PutMapping("/{attemptId}/answers")
    @Operation(summary = "Autosave an answer",
            description = "Send the complete selection, not a delta; an empty list clears the "
                    + "answer. The response echoes the state the server holds, along with the "
                    + "refreshed palette and the server clock.")
    public SaveAnswerResponse saveAnswer(@PathVariable Long attemptId,
                                         @Valid @RequestBody SaveAnswerRequest request) {
        return answerService.save(attemptId, SecurityUtils.requireUserId(), request);
    }

    @PutMapping("/{attemptId}/questions/{attemptQuestionId}/visited")
    @Operation(summary = "Record that a question has been seen")
    public SaveAnswerResponse markVisited(@PathVariable Long attemptId,
                                          @PathVariable Long attemptQuestionId) {
        return answerService.markVisited(attemptId, SecurityUtils.requireUserId(),
                attemptQuestionId);
    }

    @PostMapping("/{attemptId}/submit")
    @Operation(summary = "Submit the test",
            description = "Idempotent. An attempt already submitted, or auto-submitted by the "
                    + "expiry sweep, returns its result rather than an error.")
    public AttemptResultResponse submit(@PathVariable Long attemptId) {
        return submissionService.submit(attemptId, SecurityUtils.requireUserId());
    }

    @GetMapping("/{attemptId}/result")
    @Operation(summary = "The scored result for a finished attempt")
    public AttemptResultResponse result(@PathVariable Long attemptId) {
        return resultService.getResult(attemptId, SecurityUtils.requireUserId());
    }

    @GetMapping("/{attemptId}/review")
    @Operation(summary = "Question-by-question review",
            description = "The only endpoint that returns the answer key and the worked solution, "
                    + "and only for the caller's own finished attempt.")
    public List<QuestionReviewResponse> review(@PathVariable Long attemptId) {
        return resultService.getReview(attemptId, SecurityUtils.requireUserId());
    }

    @GetMapping("/history")
    @Operation(summary = "The student's previous attempts")
    public PageResponse<AttemptSummaryResponse> history(
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return attemptService.history(SecurityUtils.requireUserId(), pageable);
    }
}
