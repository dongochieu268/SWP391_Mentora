# Mentora UI Audit Design

## Objective

Audit Mentora's current product UI without changing implementation files. Produce evidence-based findings and a prioritized improvement roadmap for a trustworthy, clear, academic learning experience with low cognitive load.

## Scope

Review the project sequentially in five bounded modules:

1. Layout and Fragments
2. Student Module
3. Mentor Module
4. Classroom Module
5. Dashboard Module

Shared assets are read only when directly used by the active module. After each module, findings are condensed into a short checkpoint before moving on so source context does not accumulate across modules.

## Review Method

Use a hybrid audit:

- Static code review of relevant Thymeleaf templates, CSS, JavaScript, and direct layout dependencies.
- Runtime browser verification when the application can be started and the reviewed surface is reachable.
- Evidence anchored to specific files and lines. Runtime-only observations identify the tested route and viewport.
- No fixes or refactors during the audit.

If runtime verification is blocked by authentication, data, or environment dependencies, record the limitation and do not present unverified behavior as fact.

## Evaluation Model

Score each dimension from 0 to 4, for a total of 20:

- Accessibility: WCAG 2.2 AA, semantics, keyboard operation, focus, accessible names, forms, contrast, and alternatives.
- Performance: asset loading, image optimization, rendering cost, animation cost, and avoidable duplication.
- Responsive design: structural breakpoints, overflow, text scaling, content reflow, and 44-by-44 CSS pixel touch targets.
- Theming: tokens, hard-coded values, semantic colors, consistency, and theme behavior where supported.
- Anti-patterns: density, competing actions, nested cards, decorative effects, inconsistent component vocabulary, and generic admin-dashboard patterns.

Findings use four priorities:

- P0: blocks a core task.
- P1: major usability barrier or WCAG AA failure.
- P2: meaningful friction with a workaround.
- P3: polish with limited user impact.

## Module Checkpoints

Each module checkpoint contains:

- Files and surfaces inspected.
- Highest-impact findings with severity and evidence.
- Positive patterns worth preserving.
- Verification limits.
- A compact handoff summary used by the final synthesis.

## Deliverables

Create two reports in the workspace:

- `UI-AUDIT-REPORT.md`: anti-pattern verdict, health score, executive summary, detailed findings by severity, systemic patterns, positive findings, and audit limitations.
- `UI-IMPROVEMENT-ROADMAP.md`: sequenced remediation phases, dependencies, acceptance criteria, suggested Impeccable commands, and re-audit gates.

The roadmap prioritizes task completion, information hierarchy, progressive disclosure, understandable navigation, and accessibility before visual polish.

## Quality Controls

- Do not report a finding without user impact and concrete evidence.
- Distinguish confirmed defects from risks requiring runtime verification.
- Avoid noisy P3 inventories.
- Check for duplicate findings across modules and elevate recurring issues as systemic.
- Preserve the user's uncommitted changes and do not edit existing UI implementation files.
