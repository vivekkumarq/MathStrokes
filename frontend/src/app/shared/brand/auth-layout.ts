import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Logo } from './logo';

/**
 * Split layout for the signed-out screens: a branded panel on the left, the form on the
 * right. The panel collapses to a slim header below 900px so the form stays the focus on
 * a phone.
 */
@Component({
  selector: 'app-auth-layout',
  imports: [Logo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="split">
      <aside class="panel on-dark">

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
      display: flex;
      align-items: center;
      background: linear-gradient(150deg, #4338ca 0%, #4f46e5 45%, #7c3aed 100%);
      color: #fff;
    }


    .panel-inner {
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
  readonly heading = input('For JEE Mathematics');
  readonly blurb = input(
    'Full-length timed papers with real exam marking, instant evaluation and your rank against everyone who sat the same paper.',
  );
}
