# The Inception loop

Inception is not a one-shot meeting. It's a **driver + mob** loop that runs until the artifacts are clean enough for parallel construction.

## Roles

- **Driver** — one person. Holds the pen. Runs the `/inception` skill with their AI agent. Answers what they can, parks what they can't.
- **Mob** — the rest of the team (BE + Android + iOS, plus product/design as needed). Reviews drafts, answers parked questions, ratifies decisions.

The driver rotates by feature — usually whoever proposed it or whoever has the most context.

## The loop

```
┌─────────────────────────────────────────┐
│ Driver runs /inception with their agent │
│  → produces drafts + open-questions.md  │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ Driver posts hand-off summary to team   │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ Mob reviews artifacts, answers          │
│ open-questions.md, ratifies decisions   │
└────────────────┬────────────────────────┘
                 │
                 ▼
            ┌────────────┐
            │ Done?      │── yes ──▶ Cut issues, start Construction
            └────┬───────┘
                 │ no
                 ▼
        Driver re-runs /inception
        (loop iteration with mob's answers)
```

## Definition of done

The Inception phase ends when **all** of the following hold:

- [ ] `open-questions.md` has zero unresolved items (or only items explicitly deferred to a later phase)
- [ ] `api-contract.md` has zero `TBD` markers (or says "no BE work")
- [ ] Every issue under `issues/` has acceptance criteria
- [ ] PRD has at least one success metric
- [ ] `decisions.md` has been reviewed by the mob

## Anti-patterns

- **The driver answers everything alone.** Then the mob isn't the mob — it's a rubber stamp. Park questions even if you *think* you know the answer; the mob may surprise you.
- **The mob redesigns the PRD in review.** Mob review is for answering parked questions and ratifying, not for restarting. If the PRD's intent is wrong, kill the PRD and restart Inception — don't grind it.
- **Skipping the api-contract because "we'll figure it out".** This is the exact moment parallelization dies. The contract is the keystone — write it.
- **Issues without acceptance criteria.** They are not issues; they are wishes. Reject and rewrite.
- **One mega-issue per platform.** Vertical slices. If you can't ship the slice independently, split it.

## How long should the loop take?

Typically 2–3 iterations over a few days. If you hit iteration 5+ and `open-questions.md` keeps growing, the feature is too big or the team disagrees on the *intent*. Pause and have a focused product conversation — don't keep iterating Inception artifacts.
