package com.mathstrokes.question.service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A narrow guard against markup reaching a question stem, an option or a solution.
 *
 * These fields are LaTeX and plain text by contract - the server stores the source verbatim and
 * KaTeX renders it in the browser with HTML trust disabled, so escaping in the client is the real
 * boundary. This exists as defence in depth for a compromised admin account, so a stored
 * cross-site payload cannot be planted for every student who later sits the paper.
 *
 * Deliberately narrow. LaTeX legitimately contains angle brackets - $a < b$ is ordinary
 * mathematics - so rejecting them wholesale would corrupt real content. Only the handful of
 * constructs that could not be anything but markup are refused.
 */
final class ContentSafetyGuard {

    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("<\\s*script", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*iframe", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*object", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*embed", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE));

    private ContentSafetyGuard() {
    }

    /** \\return true when the text contains something that could only be markup */
    static boolean containsMarkup(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return FORBIDDEN.stream().anyMatch(pattern -> pattern.matcher(content).find());
    }
}
