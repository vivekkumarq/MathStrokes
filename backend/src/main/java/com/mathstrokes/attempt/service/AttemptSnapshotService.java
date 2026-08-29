package com.mathstrokes.attempt.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mathstrokes.attempt.entity.AttemptQuestion;
import com.mathstrokes.attempt.entity.AttemptQuestionOption;
import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.common.exception.BusinessRuleException;
import com.mathstrokes.marking.entity.MarkingConfig;
import com.mathstrokes.marking.entity.MarkingScheme;
import com.mathstrokes.marking.service.MarkingSchemeService;
import com.mathstrokes.question.entity.Question;
import com.mathstrokes.question.entity.QuestionOption;
import com.mathstrokes.question.repository.QuestionRepository;
import org.springframework.stereotype.Service;

/**
 * Copies live questions into an attempt as an immutable snapshot.
 *
 * This is what makes historical results reproducible. After this runs, evaluation never reads
 * the questions table again: the text, the options, which option is correct, and the marking
 * configuration all live on the attempt. Editing, re-keying, archiving or re-scheming a question
 * afterwards changes nothing for attempts that already exist.
 */
@Service
public class AttemptSnapshotService {

    private final QuestionRepository questionRepository;
    private final MarkingSchemeService markingSchemeService;

    public AttemptSnapshotService(QuestionRepository questionRepository,
                                  MarkingSchemeService markingSchemeService) {
        this.questionRepository = questionRepository;
        this.markingSchemeService = markingSchemeService;
    }

    /**
     * @param questionIds the paper, in the order it will be presented
     */
    public void snapshotOnto(TestAttempt attempt, List<Long> questionIds) {
        List<Question> questions = questionRepository.findAllByIdWithOptions(questionIds);
        Map<Long, Question> byId = new HashMap<>();
        questions.forEach(question -> byId.put(question.getId(), question));

        int order = 1;
        for (Long questionId : questionIds) {
            Question question = byId.get(questionId);
            if (question == null) {
                throw new BusinessRuleException(
                        "Question " + questionId + " disappeared while the paper was being built. "
                                + "Please start the test again.");
            }
            attempt.addQuestion(snapshot(question, order++));
        }
    }

    private AttemptQuestion snapshot(Question question, int order) {
        MarkingScheme scheme = question.getMarkingScheme() != null
                ? question.getMarkingScheme()
                : markingSchemeService.resolveFor(question.getExamPattern(),
                        question.getQuestionType());
        MarkingConfig config = scheme.getConfiguration();

        AttemptQuestion snapshot = new AttemptQuestion();
        snapshot.setQuestion(question);
        snapshot.setQuestionOrder(order);
        snapshot.setQuestionVersion(question.getVersion() == null ? 0 : question.getVersion());
        snapshot.setChapter(question.getChapter());
        snapshot.setExamPattern(question.getExamPattern());
        snapshot.setDifficulty(question.getDifficulty());
        snapshot.setQuestionType(question.getQuestionType());
        snapshot.setQuestionContent(question.getQuestionContent());
        snapshot.setSolutionContent(question.getSolutionContent());
        snapshot.setMarkingScheme(scheme);
        snapshot.setMarkingSchemeName(scheme.getName());
        snapshot.setMarkingConfig(config);
        snapshot.setMaxMarks(config.maxMarks());

        for (QuestionOption option : question.getOptions()) {
            AttemptQuestionOption copy = new AttemptQuestionOption();
            copy.setSourceOptionId(option.getId());
            copy.setOptionKey(option.getOptionKey());
            copy.setContent(option.getContent());
            copy.setDisplayOrder(option.getDisplayOrder());
            copy.setCorrect(option.isCorrect());
            snapshot.addOption(copy);
        }
        return snapshot;
    }
}
