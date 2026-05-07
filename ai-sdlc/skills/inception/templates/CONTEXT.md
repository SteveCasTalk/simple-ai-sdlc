---
type: context
project: <project-name>
tags:
  - context
  - project/<project-name>
---

# CONTEXT

> [!note] **Project-wide.** This file lives at the **project root**, not inside any feature's Inception folder. It is the project's shared language and grows across features. Each feature's Inception run *appends* to it.

Shared language for this project. Every term here must earn its place by replacing a longer phrase the team would otherwise repeat. **If a term is only used once, delete it.**

## Domain terms

| Term | Meaning | Replaces |
|---|---|---|
| <Term> | <definition> | <the long phrase this shortens> |

## Domain entities (data model)

The shape of the things our system reasons about. Not API shapes — those live in `api-contract.md`. These are the conceptual entities.

### <Entity name>

- **What it is:** <one sentence>
- **Identifier:** <how it's identified>
- **Key fields:** <fields the team needs to agree on>
- **Lifecycle:** <created when, retired when>
- **Relationships:** <linked to which other entities>

### <Entity name>

...

## Glossary of process terms

Optional. Use only if the team has process-specific words (e.g., "the materialization cascade", "a soft delete").

| Term | Meaning |
|---|---|
| <Term> | <definition> |
