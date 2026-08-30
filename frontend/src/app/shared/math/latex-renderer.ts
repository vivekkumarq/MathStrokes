import katex from 'katex';

/**
 * Renders Iota question content to HTML.
 *
 * THIS FUNCTION IS A SECURITY BOUNDARY. Its output is injected with
 * bypassSecurityTrustHtml, because Angular's sanitizer strips the inline styles KaTeX
 * needs and would silently break every formula. That bypass is only sound because of the
 * two invariants below — if you change this file, keep both:
 *
 *   1. Every character OUTSIDE a maths delimiter is HTML-escaped. Content is never
 *      treated as markup.
 *   2. Maths is rendered by KaTeX with `trust: false`, so \htmlClass, \href and
 *      friends cannot emit raw HTML.
 *
 * The backend guarantees content is LaTeX-and-plain-text source with no markup, and
 * additionally rejects script/iframe/javascript:/on*= payloads on save. That is defence
 * in depth for a compromised admin account; the escaping here is the actual boundary.
 *
 * Delimiters are `$...$` (inline) and `$$...$$` (display), per the backend contract.
 * `\(...\)` and `\[...\]` are deliberately NOT supported.
 */

const ESCAPES: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
};

const BACKSLASH = '\\';

export function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (char) => ESCAPES[char]);
}

function renderMath(tex: string, displayMode: boolean): string {
  try {
    return katex.renderToString(tex, {
      displayMode,
      // Blocks \href, \htmlClass and every other command that can emit raw HTML.
      trust: false,
      // A malformed formula shows in red rather than throwing and blanking the question:
      // a student must still be able to read the rest of the stem.
      throwOnError: false,
      strict: false,
      output: 'html',
    });
  } catch {
    // renderToString should not throw with throwOnError false, but if it ever does the
    // source is shown as escaped text rather than losing the content entirely.
    return escapeHtml(displayMode ? `$$${tex}$$` : `$${tex}$`);
  }
}

/**
 * Finds the closing delimiter, ignoring one escaped with a backslash.
 * Returns -1 when the delimiter is never closed.
 */
function findClosing(source: string, from: number, delimiter: string): number {
  let index = from;
  while (index < source.length) {
    const next = source.indexOf(delimiter, index);
    if (next === -1) {
      return -1;
    }
    if (next > 0 && source[next - 1] === BACKSLASH) {
      index = next + delimiter.length;
      continue;
    }
    return next;
  }
  return -1;
}

/**
 * Converts LaTeX source to HTML.
 *
 * An unmatched or unknown delimiter is emitted as literal text rather than guessed at, so
 * a stray dollar sign in a stem renders as a dollar sign instead of swallowing the rest of
 * the paragraph.
 *
 * Newlines and blank lines are preserved verbatim; the container applies
 * `white-space: pre-wrap` so paragraph structure survives without generating markup.
 */
export function renderLatex(source: string | null | undefined): string {
  if (!source) {
    return '';
  }

  let html = '';
  let text = '';
  let i = 0;

  const flushText = (): void => {
    if (text) {
      html += escapeHtml(text);
      text = '';
    }
  };

  while (i < source.length) {
    const char = source[i];

    // An escaped dollar is a literal dollar, never a delimiter.
    if (char === BACKSLASH && source[i + 1] === '$') {
      text += '$';
      i += 2;
      continue;
    }

    if (char !== '$') {
      text += char;
      i += 1;
      continue;
    }

    const isDisplay = source[i + 1] === '$';
    const delimiter = isDisplay ? '$$' : '$';
    const contentStart = i + delimiter.length;
    const closing = findClosing(source, contentStart, delimiter);

    if (closing === -1) {
      // Never closed: literal text.
      text += delimiter;
      i += delimiter.length;
      continue;
    }

    const tex = source.slice(contentStart, closing);
    if (tex.trim() === '') {
      // `$$` or `$ $` with nothing inside is not maths.
      text += source.slice(i, closing + delimiter.length);
      i = closing + delimiter.length;
      continue;
    }

    flushText();
    html += renderMath(tex, isDisplay);
    i = closing + delimiter.length;
  }

  flushText();
  return html;
}
