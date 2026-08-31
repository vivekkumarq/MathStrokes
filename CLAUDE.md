# Working agreements for this repository

Rules that apply to every session working in this repo, whatever its working directory.

## Authorship: no AI attribution, anywhere

Commits, pull request titles and bodies, PR comments and issues carry **no AI attribution**:

- no `Co-Authored-By: Claude ...` trailer
- no `Claude-Session: ...` line
- no "🤖 Generated with [Claude Code]" footer
- no equivalent in any other wording

This **overrides the default commit-trailer guidance**. The author and committer are the
repository owner, and nothing in the message should say otherwise.

**Why it matters, and why it has to be right the first time.** These trailers make GitHub list
"claude Claude" in the repository's Contributors sidebar, which the owner does not want on a
public portfolio. Removing them afterwards is expensive and never fully succeeds: a history
rewrite plus force-push fixes the messages, but GitHub keeps the original commits reachable by
SHA through the PR "Commits" tabs and `refs/pull/*/head`, and only GitHub Support can purge
those. This has already been cleaned up once, on 2026-08-31, across 8 commits and 7 PR bodies.

## Git

- Work on a branch and open a PR. Do not commit directly to `main`.
- Stage named paths. Never `git add -A`.
- Announce before running anything that discards work in the tree — `reset --hard`,
  `clean -fd`, `checkout -- <path>`, or an unnamed `stash`. More than one session may be
  working in this checkout at the same time, and an uncommitted file lost this way is
  unrecoverable.
- Prefer `git merge --ff-only` over `reset --hard` for moving a local branch onto its remote.

## Deploys

- Frontend: `npm run build`, then `netlify deploy --prod --no-build --dir=dist/frontend/browser`.
  **Always `--no-build`.** Without it Netlify re-runs `npm ci`, which wipes `node_modules` out
  from under whoever else is working here — it has done so twice — and can fail outright on a
  locked `esbuild.exe`.
- Backend: deploys from `main` automatically. Never edit a migration that has already been
  applied; Flyway validates checksums on boot and a mismatch stops the deploy dead. Two deploys
  have already been lost this way.
