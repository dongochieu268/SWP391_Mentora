# Mentora UI Improvement Roadmap

## Goal

Move Mentora from a customized admin template toward a trustworthy, clear, academic product that helps students and mentors complete the next task with low cognitive load. Accessibility and data integrity come before visual polish.

## Priority Model

- **Now:** P1 data integrity and WCAG AA failures.
- **Next:** Information architecture and task guidance.
- **Then:** Responsive scalability, performance, and design-system consistency.
- **Last:** Visual and interaction polish, followed by a full re-audit.

## Phase 0: Trust and Accessibility Blockers

**Target:** Remove all nine consolidated P1 findings before release.

### Work

1. Replace hard-coded admin metrics with authoritative queries or remove the metrics.
2. Build one accessible mobile navigation controller with synchronized state, Escape, and focus return.
3. Remove global overflow suppression and identify actual reflow sources.
4. Standardize form field, question group, modal title, search control, and progressbar naming.
5. Open learning-content modal through a real dialog lifecycle.
6. Add iframe titles and live completion/progress feedback.
7. Remove automatic subject-filter submission.

### Acceptance Criteria

- Admin dashboard never displays a number not supplied by an authoritative service.
- Every visible control has a programmatically determinable name.
- Assessment radio groups announce their question with each answer context.
- Mobile navigation reports open/closed state, closes on Escape, and returns focus.
- No visible modal or descendant is inside `aria-hidden="true"`.
- Completion success/error and changed progress are announced without moving focus unexpectedly.
- Pages reflow at 320 CSS pixels and 400% zoom without hidden actionable content.
- Automated accessibility scan has no critical/serious issue on representative pages; keyboard smoke tests pass.

**Commands:** `$impeccable harden`, `$impeccable adapt`

## Phase 1: Task-First Information Architecture

**Target:** Make the product explain context, priority, and next action before adding more UI.

### Work

1. Shape a student dashboard around “continue learning”, pending assessment, and classroom status.
2. Shape a mentor dashboard around pending joins, draft setup, and recent classes.
3. Replace generic dashboard shortcut grids with one dominant next action and compact secondary links.
4. Add one `main`/`h1` page composition and normalized section headings.
5. Add answered/total assessment review and result interpretation.
6. Hide unassigned roadmap branches until relevant and present the route as an ordered learning sequence.
7. Separate learning-path outline work from detailed assessment/question authoring.

### Acceptance Criteria

- A student can identify the next learning action within five seconds of opening the dashboard.
- A mentor sees pending work without opening three separate modules.
- Dashboard content is not a duplicate of sidebar navigation.
- Each page has one clear title and a logical heading outline.
- Assessment submission identifies unanswered questions before the final action.
- Results explain outcome and provide a context-aware next step.
- Roadmap shows only actionable/current-path content by default.

**Commands:** `$impeccable shape`, `$impeccable distill`, `$impeccable layout`

## Phase 2: Language and Interaction Consistency

**Target:** Remove implementation vocabulary and inconsistent consequence feedback.

### Work

1. Centralize Vietnamese labels for statuses and roles.
2. Replace `node`, `PASS`, `FAIL`, `BRANCH TEST`, `DRAFT`, `PUBLISHED`, `SELF_PACED`, `VISIBLE`, and `HIDDEN` in user-facing copy.
3. Standardize irreversible-action confirmation with affected item and consequence.
4. Add explicit new-tab context for external learning resources.
5. Add invite-code copy feedback and dependent-select status feedback.

### Acceptance Criteria

- No raw enum value appears in student or mentor UI.
- The same state uses the same label across dashboard, list, detail, and modal.
- Confirmation is reserved for meaningful irreversible actions and names the affected object.
- New-window actions disclose the context change.
- Copy/share and dependent-choice changes provide polite status feedback.

**Commands:** `$impeccable clarify`, `$impeccable harden`, `$impeccable delight`

## Phase 3: Responsive and Performance Scalability

**Target:** Keep common workflows fast and usable as data volume grows.

### Work

1. Paginate/search question-bank and classroom-member datasets server-side.
2. Replace per-question edit modals with one reusable surface.
3. Render one responsive classroom item instead of separate mobile/desktop copies.
4. Add compact mobile representations for member and node tables.
5. Load shared CSS in `head` and defer scripts.
6. Lazy-load below-the-fold embedded media.
7. Validate touch targets, zoom, long names/emails, and empty/error/loading states.

### Acceptance Criteria

- Initial question-bank DOM contains at most one edit dialog and one page of results.
- List response and DOM sizes are bounded independently of total record count.
- Student classrooms are represented once in the DOM.
- Identity and action remain adjacent at 320px without page-level horizontal clipping.
- Shared CSS is discovered before visible content paint.
- Below-the-fold players do not load until near viewport or explicit intent.

**Commands:** `$impeccable optimize`, `$impeccable adapt`, `$impeccable harden`

## Phase 4: Design System and Visual Hierarchy

**Target:** Establish a restrained product vocabulary without generic admin-template styling.

### Work

1. Document semantic color, spacing, radius, elevation, focus, state, and component tokens.
2. Migrate question-bank and remaining hard-coded feature styles to tokens.
3. Define when a container uses a border, elevation, or no enclosure.
4. Remove decorative side stripes and wide default shadows.
5. Replace repeated identical card grids with hierarchy appropriate to task importance.
6. Preserve the existing strong focus, reduced-motion, state-text, and mobile resource patterns.

### Acceptance Criteria

- Feature styles consume semantic tokens for text, surface, border, focus, and states.
- Default cards do not combine a border with a wide decorative shadow.
- No decorative 3-4px side stripe remains.
- Primary, secondary, and supporting content have visibly different hierarchy.
- Color contrast meets WCAG 2.2 AA in every state, including muted text and placeholders.

**Commands:** `$impeccable document`, `$impeccable quieter`, `$impeccable typeset`

## Phase 5: Verification and Release Gate

### Test Matrix

- **Roles:** student, mentor, admin.
- **Viewports:** 320x568, 390x844, 768x1024, 1280x800, 1440x900.
- **Zoom:** 200% and 400% for representative pages.
- **Input:** keyboard-only, touch, mouse.
- **Preferences:** reduced motion; high-contrast mode where supported.
- **States:** empty, loading, error, long content, large data volume, locked, completed, pending.
- **Flows:** join class, continue lesson, submit assessment, interpret result, approve member, build path, edit content, search question bank.

### Release Gates

- Zero P0 and P1 findings.
- WCAG 2.2 AA automated checks pass, followed by manual keyboard and screen-reader checks.
- No hidden horizontal content at target viewports/zoom.
- No hard-coded dashboard metrics or raw enum labels.
- Representative pages meet agreed performance budgets after measurement.
- Re-audit score reaches at least 14/20 before release; target 18/20 after polish.

**Commands:** `$impeccable polish`, then `$impeccable audit`

## Recommended Sequence

1. Week/iteration 1: Phase 0 blockers.
2. Week/iteration 2: Dashboard, assessment guidance, and page semantics from Phase 1.
3. Week/iteration 3: Builder/roadmap distillation and language consistency.
4. Week/iteration 4: Pagination, responsive tables, asset loading, and media performance.
5. Week/iteration 5: Token migration, visual hierarchy, full verification, and re-audit.

Run work as vertical slices where possible: fix markup, behavior, responsive state, and verification for one shared pattern before spreading it across every page.
