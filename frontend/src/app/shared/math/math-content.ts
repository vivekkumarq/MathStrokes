import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { renderLatex } from './latex-renderer';

/**
 * Renders a question stem, option or solution.
 *
 * The bypassSecurityTrustHtml here is deliberate and load-bearing: Angular's sanitizer
 * strips the inline styles KaTeX emits, which silently breaks every formula's layout.
 * It is safe ONLY because renderLatex escapes all non-maths text and runs KaTeX with
 * trust disabled. Do not pass anything through this component that has not gone through
 * renderLatex.
 */
@Component({
  selector: 'app-math-content',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="math-content" [innerHTML]="html()"></span>`,
  styles: `
    .math-content {
      display: block;
      /* Content is multi-line: prose, blank line, display formula, more prose. The blank
         lines carry paragraph structure, so preserve them rather than collapsing. */
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }
    /* A long formula must scroll inside its own block rather than widening the page. */
    .math-content ::ng-deep .katex-display {
      margin: 0.6em 0;
      overflow-x: auto;
      overflow-y: hidden;
      padding-bottom: 2px;
    }
    .math-content ::ng-deep .katex {
      font-size: 1.05em;
    }
  `,
})
export class MathContent {
  private readonly sanitizer = inject(DomSanitizer);

  readonly content = input<string | null | undefined>('');

  protected readonly html = computed<SafeHtml>(() =>
    this.sanitizer.bypassSecurityTrustHtml(renderLatex(this.content())),
  );
}
