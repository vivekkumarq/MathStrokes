import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Logo } from './logo';

/**
 * Split layout for the signed-out screens: a branded panel on the left, the form on the
 * right. The panel collapses to a slim header below 900px so the form stays the focus on
 * a phone.
 *
 * The decorative formulas are inert SVG text, not rendered LaTeX — they are wallpaper,
 * and running them through KaTeX would cost a render pass for no benefit.
 */
@Component({
  selector: 'app-auth-layout',
  imports: [Logo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="split">
      <aside class="panel on-dark">
        <svg class="wallpaper" viewBox="0 0 400 600" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
          <g fill="#ffffff" font-family="Georgia, serif" font-style="italic">
            <text x="24" y="70" font-size="34">∫ x² dx</text>
            <!-- No braces: Angular parses '{' in a template as ICU message syntax. -->
            <text x="210" y="140" font-size="26">e^iπ + 1 = 0</text>
            <text x="40" y="215" font-size="30">√(a² + b²)</text>
            <text x="235" y="290" font-size="32">Σ n²</text>
            <text x="30" y="360" font-size="27">sin²θ + cos²θ</text>
            <text x="215" y="430" font-size="30">lim x→0</text>
            <text x="34" y="505" font-size="28">ax² + bx + c</text>
            <text x="225" y="570" font-size="26">dy/dx</text>
          </g>
        </svg>

        <div class="panel-inner">
          <a class="panel-brand" href="/">
            <app-logo [size]="44" />
          </a>

          <div class="pitch">
            <h2>{{ heading() }}</h2>
            <p>{{ blurb() }}</p>
          </div>

          <ul class="points">
            <li><span class="dot"></span> 25 questions in 60 minutes, timed by the server</li>
            <li><span class="dot"></span> JEE Main and JEE Advanced marking schemes</li>
            <li><span class="dot"></span> Answers autosave — a refresh never loses work</li>
            <li><span class="dot"></span> Rank, percentile and accuracy the moment you submit</li>
          </ul>
        </div>
      </aside>

      <main class="form-side">
        <div class="form-wrap">
          <ng-content />
        </div>
      </main>
    </div>
  `,
  styles: `
    .split {
      display: grid;
      grid-template-columns: minmax(0, 5fr) minmax(0, 7fr);
      min-height: 100dvh;
    }

    .panel {
      position: relative;
      overflow: hidden;
      display: flex;
      align-items: center;
      background: linear-gradient(150deg, #4338ca 0%, #4f46e5 45%, #7c3aed 100%);
      color: #fff;
    }

    .wallpaper {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      opacity: 0.09;
      pointer-events: none;
    }

    .panel-inner {
      position: relative;
      display: flex;
      flex-direction: column;
      gap: 34px;
      padding: 48px;
      max-width: 460px;
    }

    .panel-brand {
      display: inline-flex;
      text-decoration: none;
    }

    .pitch h2 {
      font-size: 30px;
      line-height: 1.2;
      letter-spacing: -0.02em;
    }

    .pitch p {
      margin-top: 12px;
      font-size: 15px;
      line-height: 1.6;
      color: rgb(255 255 255 / 78%);
    }

    .points {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin: 0;
      padding: 0;
      list-style: none;
      font-size: 14px;
      color: rgb(255 255 255 / 86%);
    }

    .points li {
      display: flex;
      align-items: flex-start;
      gap: 11px;
    }

    .dot {
      flex-shrink: 0;
      width: 7px;
      height: 7px;
      margin-top: 7px;
      background: rgb(255 255 255 / 65%);
      border-radius: 50%;
    }

    .form-side {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 32px 24px;
      background: var(--ms-surface-sunken);
    }

    .form-wrap {
      width: 100%;
      max-width: 440px;
    }

    @media (max-width: 900px) {
      .split {
        grid-template-columns: 1fr;
      }
      .panel {
        min-height: auto;
      }
      .panel-inner {
        gap: 18px;
        padding: 28px 24px;
        max-width: none;
      }
      /* The selling points are noise on a phone; the form is what matters. */
      .points {
        display: none;
      }
      .pitch h2 {
        font-size: 22px;
      }
      .form-side {
        padding: 28px 20px 44px;
      }
    }
  `,
})
export class AuthLayout {
  readonly heading = input('Practise JEE Mathematics properly.');
  readonly blurb = input(
    'Full-length timed papers with real exam marking, instant evaluation and your rank against everyone who sat the same paper.',
  );
}
