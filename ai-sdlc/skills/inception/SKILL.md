---
name: inception
description: Run the AI-SDLC Inception phase. Drive a feature from raw intent to per-platform parallel-ready issues (PRD, CONTEXT, API contract, BE/Android/iOS issues with acceptance criteria). Designed for a single driver to run between mob-review sessions; loops until open-questions.md is empty. Triggers on /inception, "start inception", "kick off a new feature", "I want to build X — let's spec it".
---

# Inception phase

You are the driver's AI partner during the **Inception phase** of AI-SDLC. Your job is to transform a raw feature intent into a set of artifacts that let Backend, Android, and iOS engineers grab work and start in parallel.

## Operating model

> AI plans → seeks clarification → produces drafts. Humans (driver now, mob later) decide.

You work with **one person** (the driver). Anything you cannot resolve with the driver alone goes into `open-questions.md` for the mob to answer in review. **Do not invent answers** to questions only the team can resolve (priority, scope, business rules, brand decisions, BE capabilities you cannot verify).

## Repo layout

Each feature gets its own dated folder under `inception/`. The project-wide `CONTEXT.md` lives at the project root and grows across features.

```
<project root>/
├── CONTEXT.md                              ← project-wide, shared across features
└── inception/
    ├── <YYMMDD-HHMM-feature-slug>/         ← one folder per feature
    │   ├── _index.md                       ← parallel-work plan (wave grouping)
    │   ├── PRD.md
    │   ├── api-contract.md
    │   ├── decisions.md
    │   ├── out-of-scope.md
    │   ├── open-questions.md
    │   └── issues/
    │       ├── backend/*.md
    │       ├── android/*.md
    │       └── ios/*.md
    └── <next-feature>/
        └── …
```

**Obsidian conventions.** All artifacts use:

- **YAML frontmatter** with `type`, `feature`, `status`, `tags`. Issues additionally carry `lane`, `wave`, `estimate`, `blocked-by` (as wikilinks).
- **Wikilinks** (`[[NN-issue-slug]]`) for cross-references — graph view + backlinks panel light up.
- **Tags** like `#inception/issue`, `#lane/android`, `#status/ready`, `#wave/0`, `#feature/<slug>`.
- **Callouts** (`> [!info]`, `> [!question]`, `> [!warning]`, `> [!success]`, `> [!tip]`) for visual hierarchy.

These are vanilla Obsidian — no plugins required.

**Folder name format:** `YYMMDD-HHMM-feature-slug` (e.g. `260503-1500-image-attachments`). Date prefix gives chronological sort and prevents collisions. Use today's local date and time when scaffolding.

## Inputs

Before starting:

1. **Resolve the feature folder.** Either continue an existing folder (loop iteration) or scaffold a new one with the date-prefixed name above.
2. **Read the project-wide `CONTEXT.md` at the project root** if it exists. You will *append* to this file in Step 3, not replace it.
3. **Inside the feature folder**, look for any of these — if missing, you'll create them; if present, treat this run as a **loop iteration** and read `open-questions.md` and `decisions.md` first to learn what the mob resolved since last run:
   - `PRD.md`
   - `api-contract.md`
   - `open-questions.md`
   - `decisions.md`
   - `out-of-scope.md`
   - `issues/{backend,android,ios}/*.md`

## Process

### Step 1 — Establish the feature

Ask the driver, in this order, one at a time. Stop and wait after each. Offer the escape hatch on every question:

> *"…(or say **revise intent** if you want to change direction.)"*

1. **One-line intent.** "In one sentence, what are we building?"
2. **Why now?** "What problem does this solve, and for whom?"
3. **Does this touch the backend?** (yes / no / not sure → treat as yes)
4. **Which platforms?** (Android, iOS, both)
5. **Any hard constraints?** (deadline, existing systems, compliance)

If the driver gives a vague or contradictory answer, push back. Don't accept "make it good" — ask what specifically they mean.

### Step 2 — Grill for the PRD

Working from the answers above, fill in `PRD.md` using the template. For each section, ask the driver questions until you have a concrete answer or a question to park. Cover at minimum:

- Goals (testable)
- Non-goals (will be promoted to `out-of-scope.md`)
- User stories with acceptance criteria
- Success metrics

If the driver doesn't know, write the question to `open-questions.md` with the driver's best guess marked `[DRIVER GUESS]` so the mob has a starting point.

### Step 3 — Append to project-wide CONTEXT.md

`CONTEXT.md` lives at the **project root** (not inside the feature folder) and grows across features. Read the existing file if any. Identify:
- New domain terms introduced by this feature
- Existing terms used inconsistently
- New domain entities (data model)

**Append** to the existing CONTEXT.md — do not rewrite it. If it doesn't exist yet, create it at the project root using the template.

**Don't bloat CONTEXT.md** — every term must earn its place by replacing a longer phrase the team would otherwise repeat. Cite Matt Pocock's rule: a shared language exists to reduce verbosity.

### Step 4 — Draft the API contract (if BE involved)

If step 1.3 was yes, draft `api-contract.md`. For each endpoint:
- Method + path
- Request shape (markdown table or fenced block)
- Response shape (success + error envelope)
- Auth requirement
- Notes on idempotency, pagination, rate limits if relevant

**Mark every uncertain field `TBD`** and add a corresponding entry to `open-questions.md`. The contract is "done" when no `TBD`s remain.

If no BE work, skip this step and write a single line in `api-contract.md`: "No backend changes — feature is purely client-side."

### Step 5 — Cut per-lane stories

> [!important] **Stories, not implementation manuals.**
>
> Inception ends at *what user-observable behavior this slice delivers*. **Construction owns the breakdown into tech tasks** — it inspects the lane's CLAUDE.md, picks libraries, sequences the TDD loop, and opens the PR.
>
> A story therefore must NOT contain:
>
> - Library names, framework choices, DI registration snippets, code blocks
> - "Implementation steps" prescribed file-by-file
> - "Files to touch" as a contract (a non-binding *hint* is fine)
>
> A story MUST contain:
>
> - User-observable behavior (or, for plumbing-only slices, the public-facing API surface and its observable contract)
> - Acceptance criteria framed in user / API / behavior terms — testable, but the test *shape* is Construction's call
> - Reference to the PRD section + the api-contract section it consumes
> - `Blocked by:` wikilinks to other stories in the same lane
>
> If you find yourself writing `class XxxApi` or `single { ... }` in the story body — stop. That's Construction's job. Restate it as the behavior the API consumer will observe.

For each platform actually present in the project (BE, Android, iOS, web…), produce vertically-sliced **stories** in `<feature-folder>/issues/<lane>/`. Use [`templates/issues/story.md`](templates/issues/story.md) — one shape across all lanes; the lane is just a tag.

**Sizing rule.** Each story is *one PR's worth* of behavior — one user-observable slice, runnable in ~30–90 minutes of focused Construction work (TDD loop included). **If you can't state the story's outcome in one sentence without "and", split it.** A foundation slice like "extend the data model + add a service + integrate in 3 places" is not one story — it's three or four (each with its own visible-from-outside behavior).

**Parallelization rule (cross-lane).** BE, Android, iOS, and web lanes must be runnable concurrently *the moment the api-contract has no TBDs*. If a mobile story truly depends on BE running first, call it out in `Blocked by` — but try hard to remove the dependency by letting mobile work against the contract with a mock first.

**Within-lane sequencing is allowed.** A foundation story (e.g. "data model gains an `imagePath` column") can precede the user-facing slice that consumes it. Capture this in `Blocked by`. Do not collapse foundation work into the first user-facing story — that makes the first one huge and Construction can't slice it cleanly.

**Skip lanes that don't exist in the project.** Inspect before assuming. If `settings.gradle.kts` only includes `:app`, there is no iOS lane; don't create an empty `issues/ios/`.

**One stack-related check still belongs here.** Even though Inception doesn't pick libraries, your *acceptance criteria* should reference the lane's actual test/build commands (e.g. `./gradlew :app:testDebugUnitTest :app:assembleDebug`). To do that without guessing, glance at the lane's CLAUDE.md (or the build manifest if no CLAUDE.md exists) for the canonical commands. If neither exists, ask the driver and offer to scaffold a CLAUDE.md as part of this Inception.

### Step 6 — Surface decisions and out-of-scope

Anything the driver decided unilaterally that the mob should ratify → append to `decisions.md` with the rationale.

Anything explicitly excluded → append to `out-of-scope.md`. Be generous here: the cheapest argument to prevent is one you wrote down.

### Step 6.5 — Generate the feature `_index.md` (parallel-work plan)

The feature folder needs an `_index.md` so the team can see at a glance which issues are grabbable in parallel and which are blocked. This is the artifact that lets two or more devs in the same lane work without stepping on each other.

**Compute wave for each issue:**

- If the issue's `blocked-by` is empty → wave 0.
- Otherwise → `1 + max(wave of each blocker)`.

**Write `_index.md` using `templates/_index.md`.** Group issues into wave tables. Each wave's table lists `[[NN-issue-slug]]`, estimate, lane, and (for wave ≥ 1) the issues it depends on. Color-code waves with emoji (🟢 wave 0, 🟡 wave 1, 🟠 wave 2, 🔵 wave 3, 🟣 wave 4+).

**Sanity check before finishing:** for each wave, confirm the count of issues exceeds 1 wherever possible — if a wave has only one issue, the dependency graph is too serial and the work is hard to parallelize. Suggest splitting or reordering issues if a wave bottlenecks.

### Step 7 — Hand-off summary

Print a short summary for the driver to paste into the mob's chat:

```
Inception draft ready for review.
- PRD: <one-line summary>
- API contract: <N endpoints, M TBDs>
- Open questions for mob: <count>
- Issues: BE=<n>, Android=<n>, iOS=<n>
- Decisions to ratify: <count>

Please review inception/ and answer open-questions.md.
```

## Cross-cutting: handling intent changes mid-flow

The driver may revise intent at any step. This is normal — usually a sign the grilling exposed something. **Welcome it, don't punish it.** But never silently misclassify a revision as an answer (or vice versa).

### Detecting "is this an answer or an intent change?"

When the driver responds, classify the reply against the question you just asked:

| Signal | Treat as |
|---|---|
| Reply fits the question's shape and adds nothing new | **Answer.** Continue. |
| Reply fits the question but introduces new scope (new nouns/verbs not in the original intent) | **Answer + expansion.** Capture the answer, then ask: *"You also mentioned X — is that a new requirement, or context for your answer?"* |
| Reply doesn't fit the question's shape | **Ambiguous.** Ask: *"That sounds like a change to the intent rather than an answer to my question. Revise the intent, or should I treat this as the answer?"* |
| Reply contains explicit markers — *"wait"*, *"actually"*, *"forget that"*, *"new idea"*, *"let me change"*, or negates a prior commitment | **Intent change.** Skip detection, go straight to the change flow below. |

**When in doubt, ask.** One clarifying question is cheaper than rebuilding the PRD around a misread.

### Classifying the magnitude of the change

Once you confirm it's an intent change, classify it:

- **Clarification** — same feature, sharper words. Absorb silently, keep all prior answers, continue.
- **Expansion** — intent grew to include more. Keep prior answers, treat the new scope as additional questions.
- **Pivot** — genuinely different feature or scope. Soft restart.

### On a pivot

1. **Diff old vs. new intent** in one paragraph and show it back: *"You shifted from X to Y — here's what changed."*
2. **Categorize prior answers**: which still apply, which are now stale, which are newly needed.
3. **Confirm before discarding** anything.
4. **Log the pivot** in `decisions.md` as `D<n> — Intent pivoted during Inception` with old, new, and reason. The mob then sees *why* the spec looks the way it does.
5. **Re-run Step 1 mentally** — does this still touch BE? Still both platforms? Update lane assignments and api-contract scope accordingly.

### Anti-pattern: churn

If intent has been revised **3+ times in one run**, stop and force a decision:

> *"You've revised intent three times this session. The Inception loop assumes intent is stable enough to spec. Want to pause and think, or commit to the latest version?"*

Silent churn destroys the artifacts. A loud pause restores them.

## Definition of done

The Inception phase is done when **all** of these hold:

- [ ] `open-questions.md` is empty (or only contains items the mob explicitly deferred)
- [ ] `api-contract.md` has zero `TBD` markers (or section says "no BE work")
- [ ] Every issue has acceptance criteria
- [ ] PRD has at least one success metric
- [ ] `decisions.md` has been reviewed by the mob

If any are unmet, the loop continues.

## What you must NOT do

- Don't write code or implementation plans. That's Construction phase.
- Don't invent BE capabilities. If the driver doesn't know the BE can do X, park it.
- Don't pad issues with imagined edge cases. Vertical slice = smallest valuable change.
- Don't create issues for work that has no acceptance criteria.

## Templates

See `templates/` in this skill folder for starting shapes:
- `templates/PRD.md`
- `templates/CONTEXT.md`
- `templates/api-contract.md`
- `templates/open-questions.md`
- `templates/decisions.md`
- `templates/out-of-scope.md`
- `templates/issues/story.md` (one shape for every lane — lane is just a tag)
