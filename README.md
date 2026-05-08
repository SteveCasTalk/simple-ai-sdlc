# ai-sdlc

An AI-DLC framework for a 3-discipline team (Backend + Android + iOS) that produces enough specification during **Inception** for all three platforms to build **in parallel** without blocking each other.

Inspired by [AWS's AI-Driven Development Life Cycle](https://aws.amazon.com/blogs/devops/ai-driven-development-life-cycle/) and [Matt Pocock's Skills For Real Engineers](https://github.com/mattpocock/skills).

**Companion repo:** [orchestration-mcp](https://github.com/NguyenKhacPhuc/orchestration-mcp) — a remote MCP server that publishes the Inception artifacts to GitHub Issues + repo markdown, so multiple dev agents can pick up unblocked work in parallel without cloning the inception folder. Optional but recommended once the team grows past one driver.

## The keystone idea

The single most expensive bottleneck on a 3-discipline team is **sequential blocking**: mobile waits for BE, BE waits for spec, spec waits for someone to write it down. AI-SDLC's Inception phase exists to break that chain by producing one artifact: an **API contract** clear enough that BE, Android, and iOS can pick up independently-grabbable issues on day one.

## Phases

| Phase | Status | Purpose |
|---|---|---|
| **Inception** | Built (v0.2) | Intent → per-lane **stories** (acceptance only — no impl detail). Story is the contract Construction satisfies. |
| **Construction** | Built (v0.1) | Pick a story from the inception MCP, decompose per stack, build TDD-style, one PR per story. |
| Operations | Not built | Deploy, monitor, incident |

Stories are stack-agnostic on purpose: Inception names *what user-observable behavior* a slice delivers; Construction picks the libraries and writes the code, guided by a per-lane `CLAUDE.md` in the target project.

## Repo layout produced by Inception

Each feature gets its own dated folder under `inception/`. The project-wide `CONTEXT.md` lives at the project root and grows across features.

```
<project root>/
├── CONTEXT.md                              ← project-wide, shared across features
└── inception/
    ├── <YYMMDD-HHMM-feature-slug>/         ← one folder per feature
    │   ├── _index.md                       ← parallel-work plan (wave grouping)
    │   ├── PRD.md                          ← what we're building and why
    │   ├── api-contract.md                 ← endpoints, payloads (or "no BE work")
    │   ├── decisions.md                    ← ADR-lite log to ratify
    │   ├── out-of-scope.md                 ← explicit non-goals
    │   ├── open-questions.md               ← empty file = phase done
    │   └── issues/
    │       ├── backend/*.md                ← only if BE involved
    │       ├── android/*.md                ← only if Android in project
    │       └── ios/*.md                    ← only if iOS in project
    └── <next-feature>/
        └── …
```

Folder name format: `YYMMDD-HHMM-feature-slug`. Date prefix gives chronological sort and prevents collisions. Each issue carries acceptance criteria so "done" is testable.

**Obsidian-friendly by default.** All artifacts use YAML frontmatter, wikilinks, tags, and callouts — vanilla Obsidian, no plugins. Drop the project folder into an Obsidian vault and you get a Properties panel, backlinks, graph view, and tag pane for free. The `_index.md` per feature is the headline artifact — it groups issues into dependency **waves** so 2+ devs in the same lane can pick up day-1 issues in parallel.

## How it works (the loop)

Inception is a **driver + mob** loop:

1. **Driver** (one person) runs `/inception` with their agent. The agent grills them, produces a draft PRD, CONTEXT updates, api-contract, issues — and parks anything the driver can't answer in `open-questions.md`.
2. **Mob review** — the whole team (BE + Android + iOS) reads the drafts and answers open questions. Decisions land in `decisions.md`.
3. **Loop** — driver re-runs `/inception` with the mob's answers. New questions surface. Repeat.
4. **Done** when `open-questions.md` is empty *and* every issue has acceptance criteria *and* (if BE involved) the api-contract has no `TBD`s.

See [docs/inception-loop.md](./docs/inception-loop.md) for details.

## Installation

This is a Claude Code plugin. From a Claude Code session:

```
/plugin install <path-to-this-repo>
```

Then in any project repo:

```
/inception
```

## Publishing to GitHub (optional)

After the mob signs off on the Inception artifacts, push them to GitHub via the [orchestration-mcp](https://github.com/NguyenKhacPhuc/orchestration-mcp) server:

```bash
# from the orchestration-mcp checkout, with MCP_URL + MCP_TOKEN set:
bun src/cli/publish.ts /path/to/your-project/inception/<feature-slug>
```

This commits the markdown files to the target repo and creates real GitHub issues with labels and `Blocked by:` linkage. After publishing, multiple dev agents can pick up unblocked work in parallel via the MCP — see the [orchestration-mcp README](https://github.com/NguyenKhacPhuc/orchestration-mcp#daily-flow-for-devs) for the daily loop.

You can skip this entirely if you're a solo driver and prefer to keep the artifacts as files in your repo.

## Why three artifacts and not one giant spec?

- **PRD** is *intent* — readable by non-engineers, stable across iterations.
- **CONTEXT.md** is *language* — terminology, domain entities, data model. Reused across many features.
- **api-contract.md** is *interface* — the precise shape that decouples BE from mobile.

Mixing them produces a giant doc nobody reads. Separating them lets each artifact have one job and one audience.
