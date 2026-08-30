import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * The Iota mark: the Greek letter ι — a dot above a stem that curves away at the foot.
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
    <span class="logo" [class.logo--stacked]="stacked()">
      <svg
        class="mark"
        [attr.width]="size()"
        [attr.height]="size()"
        viewBox="0 0 48 48"
        role="img"
        [attr.aria-label]="wordmark() ? null : 'Iota'"
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
        <span class="word" [style.font-size.px]="wordSize()">Iota</span>
      }
    </span>
  `,
  styles: `
    .logo {
      display: inline-flex;
      align-items: center;
      gap: 10px;
    }
    .logo--stacked {
      flex-direction: column;
      gap: 12px;
    }
    .mark {
      display: block;
      flex-shrink: 0;
      border-radius: 12px;
    }
    .word {
      font-weight: 700;
      letter-spacing: -0.015em;
      white-space: nowrap;
      line-height: 1;
      color: var(--ms-ink);
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

  protected readonly wordSize = computed(() => Math.round(this.size() * 0.56));

  /** Unique per instance so two logos on one page cannot share a gradient id. */
  protected readonly gradientId = computed(
    () => `iota-logo-${Math.random().toString(36).slice(2, 9)}`,
  );
}
