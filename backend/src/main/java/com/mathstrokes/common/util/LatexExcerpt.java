package com.mathstrokes.common.util;

/**
 * Shortens LaTeX question content for list views without cutting through a formula.
 *
 * The admin grids render their excerpts as maths. A plain character cut that lands inside
 * {@code $...$} drops the closing delimiter, and the renderer then shows the rest of the
 * excerpt as raw source - {@code $O: x^2 + y^2 - 2x -...} instead of a formula. So a cut that
 * would split a formula moves to just before it, or, when the formula opens the text, to just
 * after it.
 *
 * Delimiters are matched exactly as the frontend renderer matches them: {@code $$} for display,
 * {@code $} for inline, and a backslash-escaped dollar is literal text.
 */
public final class LatexExcerpt {

    private static final String ELLIPSIS = "...";

    private LatexExcerpt() {
    }

    public static String of(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String text = content.replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLength) {
            return text;
        }

        int i = 0;
        while (i < text.length() && i < maxLength) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && text.charAt(i + 1) == '$') {
                i += 2;
                continue;
            }
            if (c != '$') {
                i++;
                continue;
            }
            String delimiter = text.startsWith("$$", i) ? "$$" : "$";
            int closing = findClosing(text, i + delimiter.length(), delimiter);
            if (closing == -1) {
                // Never closed: the renderer prints it literally anyway, so any cut is as good.
                break;
            }
            int end = closing + delimiter.length();
            if (end > maxLength) {
                return cut(text, i > 0 ? i : end);
            }
            i = end;
        }
        return cut(text, maxLength);
    }

    private static String cut(String text, int at) {
        return at >= text.length() ? text : text.substring(0, at).stripTrailing() + ELLIPSIS;
    }

    private static int findClosing(String text, int from, String delimiter) {
        int next = text.indexOf(delimiter, from);
        while (next > 0 && text.charAt(next - 1) == '\\') {
            next = text.indexOf(delimiter, next + delimiter.length());
        }
        return next;
    }
}
