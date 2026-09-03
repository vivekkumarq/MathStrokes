import { describe, expect, it } from 'vitest';

import { escapeHtml, hasUnclosedDelimiter, renderLatex } from './latex-renderer';

describe('renderLatex', () => {
  describe('security: content is never treated as markup', () => {
    it('escapes HTML in plain text', () => {
      const html = renderLatex('<script>alert(1)</script>');
      expect(html).not.toContain('<script');
      expect(html).toContain('&lt;script&gt;');
    });

    it('escapes an img/onerror payload', () => {
      const html = renderLatex('<img src=x onerror="alert(1)">');
      expect(html).not.toContain('<img');
      expect(html).toContain('&lt;img');
    });

    it('does not emit a javascript: anchor from \\href, since trust is disabled', () => {
      const html = renderLatex('$\\href{javascript:alert(1)}{x}$');
      expect(html).not.toContain('<a href="javascript:');
    });

    it('escapes text surrounding a formula', () => {
      const html = renderLatex('<b>before</b> $x$ <b>after</b>');
      expect(html).toContain('&lt;b&gt;before&lt;/b&gt;');
      expect(html).toContain('&lt;b&gt;after&lt;/b&gt;');
    });
  });

  describe('delimiters', () => {
    it('renders inline maths', () => {
      expect(renderLatex('$x^2$')).toContain('katex');
    });

    it('renders display maths as a block', () => {
      expect(renderLatex('$$3x^2 + 7x + 2 = 0$$')).toContain('katex-display');
    });

    it('renders inline and display in the same content', () => {
      const source = 'If $\\alpha$ and $\\beta$ are roots of\n\n$$3x^2+7x+2=0$$\n\nfind it.';
      const html = renderLatex(source);
      expect(html).toContain('katex-display');
      expect(html).toContain('find it.');
    });

    it('does not support \\( \\), which is not part of the contract', () => {
      expect(renderLatex('\\(x^2\\)')).not.toContain('katex');
    });
  });

  describe('a stray dollar must not swallow the paragraph', () => {
    it('treats an unclosed dollar as literal text', () => {
      const html = renderLatex('The price is $5 and the rest of this sentence must survive.');
      expect(html).not.toContain('katex');
      expect(html).toContain('the rest of this sentence must survive.');
    });

    it('treats an unclosed double dollar as literal text', () => {
      const html = renderLatex('$$ never closed');
      expect(html).not.toContain('katex');
      expect(html).toContain('never closed');
    });

    it('treats an escaped dollar as a literal dollar', () => {
      const html = renderLatex('costs \\$5 today');
      expect(html).not.toContain('katex');
      expect(html).toContain('$5 today');
    });

    it('does not treat empty delimiters as maths', () => {
      expect(renderLatex('$$')).not.toContain('katex');
      expect(renderLatex('a $ $ b')).not.toContain('katex');
    });
  });

  describe('structure and robustness', () => {
    it('preserves blank lines so paragraph structure survives', () => {
      expect(renderLatex('one\n\ntwo')).toContain('one\n\ntwo');
    });

    it('renders angle brackets inside maths, which are ordinary LaTeX', () => {
      expect(renderLatex('$a < b$')).toContain('katex');
    });

    it('does not throw on malformed LaTeX', () => {
      expect(() => renderLatex('$\\frac{1}{$')).not.toThrow();
      expect(() => renderLatex('$\\nonexistentcommand{x}$')).not.toThrow();
    });

    it('returns empty string for null, undefined and empty input', () => {
      expect(renderLatex(null)).toBe('');
      expect(renderLatex(undefined)).toBe('');
      expect(renderLatex('')).toBe('');
    });
  });

  describe('real seeded content from the API', () => {
    // Copied verbatim from GET /attempts/2/review so the renderer is pinned to the
    // shape the backend actually produces, not to invented examples.
    const STEM =
      'If $\\alpha$ and $\\beta$ are the roots of\n\n$$3x^2 + 7x + 2 = 0$$\n\nfind the product of the roots.';
    const SOLUTION =
      'For $ax^2 + bx + c = 0$ the product of the roots is $\\frac{c}{a}$.\n\nHere $a = 3$, $b = 7$, $c = 2$, so the product is $\\frac{2}{3}$.';

    it('renders a real question stem with mixed inline and display maths', () => {
      const html = renderLatex(STEM);
      expect(html).toContain('katex-display');
      expect(html).toContain('find the product of the roots.');
      // The blank lines between prose and the display block are real structure.
      expect(html).toContain('\n\n');
    });

    it('renders a real option', () => {
      expect(renderLatex('$\\frac{2}{3}$')).toContain('katex');
    });

    it('renders a real solution with several inline formulas', () => {
      const html = renderLatex(SOLUTION);
      expect(html).toContain('katex');
      expect(html).toContain('Here ');
      expect(html).not.toContain('katex-display');
    });
  });

  describe('escapeHtml', () => {
    it('escapes all five significant characters', () => {
      expect(escapeHtml(`&<>"'`)).toBe('&amp;&lt;&gt;&quot;&#39;');
    });
  });
});

describe('hasUnclosedDelimiter', () => {
  it('accepts balanced inline and display maths', () => {
    expect(hasUnclosedDelimiter('Evaluate $x^2$ and $$y = 1$$.')).toBe(false);
  });

  it('accepts source with no maths at all', () => {
    expect(hasUnclosedDelimiter('Which of the following is true?')).toBe(false);
  });

  it('treats empty and nullish source as balanced', () => {
    expect(hasUnclosedDelimiter('')).toBe(false);
    expect(hasUnclosedDelimiter(null)).toBe(false);
    expect(hasUnclosedDelimiter(undefined)).toBe(false);
  });

  /**
   * The shape that reached the live bank: a delimiter opened mid-sentence and never closed,
   * which renders as raw LaTeX to a student without erroring anywhere.
   */
  it('flags a delimiter that is opened and never closed', () => {
    expect(hasUnclosedDelimiter('the differential equation$\\mathrm{y}=\\left(\\mathrm{x}')).toBe(
      true,
    );
  });

  it('flags a lone dollar', () => {
    expect(hasUnclosedDelimiter('costs $5 per item')).toBe(true);
  });

  it('flags an unclosed display block', () => {
    expect(hasUnclosedDelimiter('$$x = 1')).toBe(true);
  });

  // A literal dollar written correctly must not be reported, or the warning cries wolf on
  // valid content and authors learn to ignore it.
  it('accepts escaped dollars as literal text', () => {
    expect(hasUnclosedDelimiter('costs \\$5 and \\$10')).toBe(false);
  });

  it('accepts an escaped dollar alongside real maths', () => {
    expect(hasUnclosedDelimiter('\\$5 buys $x$ apples')).toBe(false);
  });

  // A CLOSED pair with nothing inside is literal text to the renderer, so it is balanced.
  it('accepts an empty but closed delimiter pair', () => {
    expect(hasUnclosedDelimiter('a $ $ b')).toBe(false);
  });

  // '$$' with no second '$$' anywhere is a display block that never closes, not an empty
  // pair. The renderer emits it literally for that reason, so flagging it is agreement.
  it('flags a lone $$ as an unclosed display block', () => {
    expect(hasUnclosedDelimiter('a $$ b')).toBe(true);
  });

  it('agrees with the renderer: flagged source renders as raw text, not maths', () => {
    const broken = 'equation$\\mathrm{y}=1';
    expect(hasUnclosedDelimiter(broken)).toBe(true);
    expect(renderLatex(broken)).not.toContain('katex');
  });
});
