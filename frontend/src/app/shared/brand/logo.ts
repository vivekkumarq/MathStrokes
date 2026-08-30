import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * The iota mark: the Greek letter ι — a dot above a stem that curves away at the foot.
 *
 * The name carries real meaning for this product. In Indian mathematics teaching the
 * imaginary unit i = √-1 is called "iota", and ι also denotes the smallest quantity.
 *
 * Inline SVG rather than an image file: crisp at any size, no network request, cannot
 * 404, and a rebrand is one file rather than a set of exported assets.
 */
@Component({
  selector: 'app-logo',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="logo" [class.logo--stacked]="stacked()" [style.gap.px]="gapPx()">
      <svg
        class="mark"
        [attr.width]="size()"
        [attr.height]="size()"
        viewBox="0 0 48 48"
        role="img"
        [attr.aria-label]="wordmark() ? null : 'iota'"
        [attr.aria-hidden]="wordmark() ? 'true' : null"
      >
        <defs>
          <linearGradient [attr.id]="gradientId()" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#6366f1" />
            <stop offset="55%" stop-color="#4338ca" />
            <stop offset="100%" stop-color="#7c3aed" />
          </linearGradient>
        </defs>

        <rect x="0" y="0" width="48" height="48" rx="12" [attr.fill]="'url(#' + gradientId() + ')'" />

        <!-- The tittle. Sized to read at 16px as well as at 44px. -->
        <circle cx="20.4" cy="14.2" r="3.5" fill="#ffffff" />

        <!-- Stem falling into the iota's tail. -->
        <path
          d="M20.4 21.6 L20.4 28.4 C20.4 33.4 24.2 35.8 29.4 34.6"
          fill="none"
          stroke="#ffffff"
          stroke-width="3.5"
          stroke-linecap="round"
        />

        <!-- √-1: the quantity iota names, kept faint so it reads as texture, not clutter. -->
        <path
          d="M31.5 15.5 L33.6 15.5 L35.4 20.4 L38.4 12.6"
          fill="none"
          stroke="#ffffff"
          stroke-opacity="0.5"
          stroke-width="1.7"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>

      @if (wordmark()) {
        <span class="word" [style.font-size.px]="wordSize()">iota</span>
      }
    </span>
  `,
  styles: `
    .logo {
      display: inline-flex;
      align-items: center;
    }
    .logo--stacked {
      flex-direction: column;
    }
    .mark {
      display: block;
      flex-shrink: 0;
      border-radius: 12px;
    }
    .word {
      font-weight: 700;
      letter-spacing: -0.01em;
      white-space: nowrap;
      line-height: 1;
      color: var(--ms-ink);
      /*
       * Optical centring. "iota" has no ascender and no descender — its ink runs from
       * the baseline to the x-height only. With line-height 1 the baseline sits about
       * 0.865em down the box, so that ink centres roughly 0.1em BELOW the box centre
       * and the word reads low against the square mark. The capitalised wordmark did
       * not need this: cap-height ink centred almost exactly on the box.
       * Nudged back by slightly less than the full 0.1em, because the t and the dot on
       * the i carry a little ink above the x-height.
       */
      transform: translateY(-0.08em);
    }
    /* On the gradient brand panel the wordmark has to read against it. */
    :host-context(.on-dark) .word {
      color: #fff;
    }
  `,
})
export class Logo {
  readonly size = input(36);
  readonly wordmark = input(true);
  readonly stacked = input(false);

  /**
   * 0.56 was tuned for the capitalised wordmark, where the ink was cap-height. Lowercase
   * "iota" is x-height ink — roughly 0.52em against cap-height's 0.727em — so the same
   * ratio renders visibly lighter and smaller beside the mark. 0.64 brings the tallest
   * features (the t and the dot on the i) back to about the old cap height without
   * letting the word start to dominate the square.
   */
  protected readonly wordSize = computed(() => Math.round(this.size() * 0.64));

  /**
   * The gap scales with the mark. A fixed 10px looked right at 36px but crowded the
   * 26px mark in the exam runner header and looked slack against the 44px auth panel.
   */
  protected readonly gapPx = computed(() => Math.round(this.size() * 0.27));

  /** Unique per instance so two logos on one page cannot share a gradient id. */
  protected readonly gradientId = computed(
    () => `iota-logo-${Math.random().toString(36).slice(2, 9)}`,
  );
}
