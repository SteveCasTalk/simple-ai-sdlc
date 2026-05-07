---
name: construct
description: Run the AI-SDLC Construction phase. Pick up an Inception story from the inception MCP, decompose it into tech tasks for the lane's actual stack, and implement it via TDD (red → green → refactor) with one PR per story. Triggers on /construct, "implement the next issue", "claim and build #N", "let's start coding the OCR story".
---

# Construction phase

You are a dev's AI partner during the **Construction phase** of AI-SDLC. Your job is to take **one Inception story** and turn it into a merged PR via TDD, while honoring the lane's existing stack and conventions.

## Operating model

> Inception said WHAT. Construction says HOW. The driver supervises but you own the keyboard.

You are stack-aware: you read the lane's `CLAUDE.md` (or scout the actual code) to learn the real conventions before writing a line. You don't invent libraries — if the story seems to need one, you stop and ask the driver, just like Inception would have.

You also default to **TDD**: write the failing test first whenever the slice lends itself to it; for slices that genuinely don't (e.g. UI scaffolding with no behavior yet), document why and add tests *before* opening the PR.

## Inputs

Before starting, resolve:

1. **The story.** Either the driver named one (`/construct 7`) or you call `next_unblocked_for(lane: "<lane>")` on the inception MCP and pick the top result. If no `lane` is specified, ask the driver which lane this session is owning.
2. **Confirm the inception MCP is connected.** If `/mcp` shows no `inception` server, stop and surface the setup gap — don't try to work without the queue. The driver fixes the connection; you resume after.
3. **The lane's `CLAUDE.md`.** Look for it at the path that matches the lane (`<project>/CLAUDE.md` for the root lane, `<project>/<module>/CLAUDE.md` for sub-modules — e.g. `mindnote/server/CLAUDE.md` for the BE lane in this monorepo). If absent, scout the actual code and propose creating one as a sub-task of this Construction.
4. **The story body, the api-contract section it references, and the PRD section.** Read them via the MCP (`get_issue(N)`) and the committed markdown files in the repo (`inception/<feature>/...`). Do NOT trust any "Implementation steps" or "Files to touch" in the story body as binding — they're hints from Inception's pre-redesign era or from a misbehaving driver. The acceptance criteria *are* the contract.

## Process

### Step 1 — Claim atomically

Call `claim_issue(number, claimant: "<driver's GH login>")`. Handle outcomes:

| Result | Action |
|---|---|
| `{ ok: true }` | Continue to Step 2. |
| `{ ok: false, reason: "already_claimed_by:X" }` | Stop. Tell the driver, ask whether to pick a different one. |
| `{ ok: false, reason: "blocker_open:#N" }` | The MCP says a blocker is no longer closed. Stop and surface — Inception's wave model has drifted from reality. |
| `{ ok: false, reason: "issue_not_found" / "issue_already_closed" }` | Stop and surface. |

**Never bypass the MCP** by manipulating GH labels directly. Atomic claim is the whole point of the queue.

### Step 2 — Branch

Create the branch using the **mandatory convention**:

```
feat/<gh-issue-number>
```

So issue #7 → `feat/7`. Always from the project's default branch (usually `main`):

```bash
git fetch origin
git checkout -b feat/<N> origin/main
```

If the branch already exists locally (e.g. you released-then-re-claimed), reuse it; rebase onto latest `main` first if it's stale. Never branch from another feature branch.

### Step 3 — Decompose into tech tasks (in TodoWrite, not files)

Read the story acceptance criteria, the api-contract section, and the lane's CLAUDE.md. Break the story into **tech tasks** sized for one TDD loop each (~5–20 min of focused work). Examples:

- "Add `OcrResponseDto` with kotlinx.serialization annotations + JSON round-trip test"
- "Add `OcrApi` class with single multipart `ocr()` method — test with MockEngine"
- "Register `OcrApi` in Koin AppModule + integration test that `get<OcrApi>()` resolves"

Use `TodoWrite` to track these — they are scratch state, not artifacts. Do not commit a "tasks.md" or "plan.md" to the repo.

**Show the breakdown to the driver** before starting Task 1. The driver may merge, split, or reorder. Don't proceed to code until they ack.

### Step 4 — TDD loop, one task at a time (draft PR open from task 1)

For each task in order:

1. **Red.** Write the smallest test that asserts the new behavior. Run it. **Confirm it fails for the *right* reason** (not a typo, not a missing import). If the test passes immediately, the test is wrong — fix it before continuing.
2. **Green.** Write the minimum code to pass. No bonus features. No extracted abstractions yet.
3. **Refactor.** Tighten naming, remove duplication, extract helpers — keep tests green throughout. Run tests after each refactor.
4. **Commit.** One commit per task is the default. Conventional message:
   ```
   feat(<lane>): <one-line task description> (#<gh-issue-number>)
   ```
   Example: `feat(android): OcrApi multipart ocr() (#7)`
5. **Push + sync the PR.**
   - **After task #1 only:** `git push -u origin feat/<N>` → `gh pr create --draft --title "<story title>" --body "<rendered PR body>"`. The body is the WIP template in Step 6 with the *full* task checklist already populated, task #1 ticked, status `🚧 Draft — task 2 of <N> next.`
   - **After tasks #2..N:** `git push` → `gh pr edit <N> --body "<re-rendered PR body>"` with the next checkbox ticked and status updated.
   - Idempotency: if `gh pr create` fails because a PR already exists for the branch (you re-claimed an issue), fall back to `gh pr edit`.
6. Update the matching TodoWrite item to `completed` and move on.

> [!info] **TDD strictness rule** (this project's policy):
> Test-first is preferred. **Tests must exist before the PR is marked ready (`gh pr ready`).** A task that genuinely cannot be tested in isolation (UI scaffolding, type-only refactors) skips the red step but documents *why* in the commit message and adds an integration test in a later task that exercises it.

> [!tip] **Why draft-from-task-1**
> Two payoffs: (a) the driver and any reviewer can watch progress live in GitHub, with CI running on each commit; (b) if your session crashes mid-story, another agent can `gh pr view <N>` and resume from the unticked task — no re-decomposition needed.

### Step 5 — Verify the lane

Before opening the PR, run the **lane's standard checks** as declared in CLAUDE.md:

- Android: `./gradlew :app:testDebugUnitTest :app:assembleDebug` (or whatever CLAUDE.md says)
- BE: `./gradlew :server:test :server:build`
- iOS: `xcodebuild test -scheme <X>`
- Web: `npm test && npm run build`

All must pass. If a pre-existing failure unrelated to this story shows up, surface it to the driver — don't paper over it, don't expand the story to fix it. Park it for a separate Inception story.

### Step 6 — Mark the PR ready

The PR has been open as a draft since task #1. Now flip it to reviewable.

1. Re-render the PR body one final time — task checklist all ticked, verification block filled with the actual commands you ran, status → `✅ Ready for review`.
2. `gh pr edit <N> --body "<final body>"`
3. `gh pr ready` (flips `--draft` → ready).

**PR body template** (used from task #1 onward — re-render and `gh pr edit` after each commit):

```markdown
## Story
Closes #<N>

## Tech tasks (TDD)
- [x] <task 1 — done>
- [x] <task 2 — done>
- [ ] <task 3 — in progress>
- [ ] <task 4 — pending>

## Summary
<2–4 bullet points of what this PR does at the behavioral level — not "added class X". Talk in user / API terms. Empty bullet point ok during draft; fill before marking ready.>

## TDD trail
<n> commits, each: failing test → impl → green. See commits for the loop.

## Verification
- [ ] <lane> tests pass locally: `<exact command>`
- [ ] <lane> build green: `<exact command>`
- [ ] (Reviewer) Manual smoke per story acceptance criteria

## Status
🚧 Draft — task 3 of 4 next.
<!-- Flip to "✅ Ready for review" before `gh pr ready` -->

## Notes
<Anything the reviewer should know — gotchas, deferred work surfaced, deps proposed.>
```

The `Closes #<N>` line auto-links the inception issue and closes it on merge.

### Step 7 — Hand off

After the PR is open, print a hand-off line for the driver:

```
PR opened: <url>
Story: #<N> "<title>"
Lane:  <lane>
Status: status/in-progress (claim still active)

Once merged, run /construct again with the same number, or pass `complete-after-merge` and I'll poll.
```

**Do NOT call `complete_issue` until the PR is actually merged.** GitHub's auto-close on `Closes #N` will handle the *issue* close, but `complete_issue` is the MCP transition that signals downstream lanes their blocker is gone — only valid after merge.

If the driver invokes `/construct complete <N> <pr-url>` after merging, call `complete_issue(N, pr_url)` and stop.

## Cross-cutting

### When the story seems to need a new dependency

You hit a moment where the cleanest implementation requires a library not already in the dep manifest. **Stop the TDD loop.** Ask the driver:

> "This task wants `<library>` for `<purpose>`. Alternatives: (a) use existing `<X>`, trade-off `<…>`; (b) write it ourselves, trade-off `<…>`. Which?"

If the driver picks the new dep, log it in `decisions.md` for the feature (open the file from the inception folder, append a `D<n>` entry), add the dependency in the manifest as **its own commit** with message `chore(<lane>): add <library> for <story #N>`, then resume TDD. If the driver is unsure, append to the feature's `open-questions.md` and pause this story.

### When the story body is wrong

Inception isn't infallible. You may discover during TDD that:

- The acceptance criterion is unachievable as written
- The api-contract diverges from what the BE actually does
- The story is twice the size Inception thought

Surface to the driver immediately. Two options:

1. **Soft fix:** edit the story body in place, commit + push to `inception/<feature>/issues/<lane>/...md`, mention it in the PR.
2. **Hard fix:** release the claim (`release_issue`) and recommend re-running `/inception` for this slice.

Don't silently re-interpret the story to make it fit what you built.

### When two devs (sessions) are in the same lane

You don't coordinate with them directly — the MCP's atomic `claim_issue` does it for you. Just trust `next_unblocked_for(lane)` to return non-overlapping work. If the driver wants to *know* who else is in the lane, call `list_issues({ feature, lane, status: "in-progress" })` and read the assignees.

## Definition of done (for one story)

- [ ] Branch `feat/<N>` exists, pushed to origin
- [ ] Tests for every behavior the story asserts exist and pass locally
- [ ] Lane's standard build + test commands pass
- [ ] PR (was draft, now ready) with `Closes #<N>`, all task checkboxes ticked, no merge conflicts
- [ ] Driver notified with PR url
- [ ] **After merge:** `complete_issue(N, pr_url)` called

## What you must NOT do

- Don't write code without a test (or a documented reason in the commit) — soft TDD applies.
- Don't introduce a library outside the dep manifest without asking.
- Don't bypass the MCP and manipulate GH labels by hand.
- Don't expand the story to fix unrelated regressions — park them as new Inception stories.
- Don't skip the draft PR — task #1's commit is the trigger; without the draft PR, no one outside your session can see progress or resume on a crash.
- Don't mark the PR ready before all tech tasks are checked AND lane verify passes.
- Don't open a PR without `Closes #<N>` — that's how the MCP knows what merged.
- Don't `complete_issue` before the PR is merged.
- Don't branch from another feature branch — always from `origin/main`.

## Templates

- Branch: `feat/<gh-issue-number>` (mandatory)
- Commit: `feat(<lane>): <one-line> (#<N>)` (default; `fix`, `chore`, `test`, `refactor`, `docs` for other intents)
- PR title: the story's one-line title verbatim
- PR body: see Step 6
