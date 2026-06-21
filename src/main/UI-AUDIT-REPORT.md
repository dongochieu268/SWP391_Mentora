# Mentora UI Audit Report

**Audit date:** 2026-06-18  
**Target:** Product UI, WCAG 2.2 AA, desktop and mobile  
**Method:** Sequential static review of five modules with runtime verification attempted but unavailable

## Anti-Patterns Verdict

**Fail.** Source code cannot prove that an interface was AI-generated, but Mentora currently reads as a customized admin template rather than a distinctive learning product. The strongest tells are repeated icon-heading-description card grids, the admin hero-metric grid, default border-plus-wide-shadow cards, decorative 3-4px side stripes, dense tables, and internal enum vocabulary exposed to users.

This is not a call for more decoration. The product needs fewer competing containers, more role-specific next actions, progressive disclosure, and a calmer content hierarchy.

## Audit Health Score

| # | Dimension | Score | Key Finding |
|---|---|---:|---|
| 1 | Accessibility | 2/4 | Good focus/touch baseline, but major form, modal, navigation, media, and live-status gaps remain |
| 2 | Performance | 2/4 | Lightweight dashboards, but late CSS, eager iframes, duplicated responsive DOM, and unpaged modal-heavy lists |
| 3 | Responsive Design | 2/4 | Several good mobile layouts, undermined by global overflow hiding and wide management tables |
| 4 | Theming | 2/4 | Shared semantic tokens exist, but coverage is partial and feature CSS still hard-codes palettes |
| 5 | Anti-Patterns | 1/4 | Repeated card grids, hero metrics, side stripes, generic admin density, and raw system language |
| **Total** | | **9/20** | **Poor - major overhaul of shared behavior and hierarchy** |

## Executive Summary

- **Audit Health Score:** 9/20 (Poor)
- **Module observations:** 42 total: 0 P0, 12 P1, 25 P2, 5 P3
- **Consolidated findings:** 26 total: 0 P0, 9 P1, 14 P2, 3 P3
- **Highest-risk areas:** mobile navigation, reflow clipping, form semantics, visible-but-hidden modal state, dynamic progress feedback, unnamed question-bank search, and false admin metrics
- **Strongest product issue:** dashboards and roadmap surfaces expose navigation or system structure instead of prioritizing the user's next learning/teaching task
- **Release position:** fix P1 findings before release; do not polish visuals before data integrity, keyboard, form, and modal behavior are corrected

## P1 Major Findings

### 1. Mobile navigation lacks an accessible open/close lifecycle

- **Location:** `resources/templates/layout/admin-header.html:13`; `resources/static/assets/js/app.min.js:24`
- **Category:** Accessibility / Responsive
- **Impact:** Keyboard and screen-reader users cannot determine sidebar state, close it with Escape, or reliably recover focus.
- **Standard:** WCAG 2.2 2.1.1, 2.4.3, 4.1.2.
- **Recommendation:** Implement one labelled disclosure/off-canvas controller with `aria-controls`, synchronized `aria-expanded`, Escape, initial focus, and return focus.
- **Suggested command:** `$impeccable harden`

### 2. Page-level overflow hiding can make zoomed content unreachable

- **Location:** `resources/static/assets/css/admin-custom.css:5`, `:12`, `:17`
- **Category:** Accessibility / Responsive
- **Impact:** Controls clipped at narrow widths or 400% zoom cannot be reached through page scrolling.
- **Standard:** WCAG 2.2 1.4.10 and 1.4.4.
- **Recommendation:** Remove global `overflow-x: hidden`; fix the overflowing component and constrain only intentional scrollers.
- **Suggested command:** `$impeccable adapt`

### 3. Core forms and answer groups lack reliable labels

- **Location:** `student/assessment/take.html:50`; `lecturer/assessment/form.html:35`; `lecturer/assessment/detail.html:205`; `lecturer/question-bank/list.html:97`; `lecturer/class/form.html:43`
- **Category:** Accessibility / Forms
- **Impact:** Users can hear controls without their question or field meaning, especially inside assessment and question-bank modals.
- **Standard:** WCAG 2.2 1.3.1 and 3.3.2.
- **Recommendation:** Bind every label to a unique control ID; group question/answer controls with `fieldset` and `legend` or equivalent labelled-group semantics.
- **Suggested command:** `$impeccable harden`

### 4. Visible learning-content modal remains `aria-hidden`

- **Location:** `lecturer/learning/node-contents.html:116`, `:117`, `:119`, `:190`
- **Category:** Accessibility / Modal
- **Impact:** A visible create/edit task is absent from the accessibility tree and lacks focus containment, potentially blocking completion.
- **Standard:** WCAG 2.2 2.1.1, 2.4.3, 4.1.2.
- **Recommendation:** Open through one dialog controller, expose a labelled modal state, and manage initial/return focus.
- **Suggested command:** `$impeccable harden`

### 5. Embedded lesson videos have no accessible titles

- **Location:** `student/learning/node-detail.html:128`, `:130`
- **Category:** Accessibility
- **Impact:** Screen-reader users cannot identify an embedded player's purpose before entering it.
- **Standard:** WCAG 2.2 2.4.1 and 4.1.2.
- **Recommendation:** Add a content-specific iframe title with a meaningful fallback.
- **Suggested command:** `$impeccable harden`

### 6. Progress and completion changes are not fully named or announced

- **Location:** `student/learning/node-detail.html:180`, `:222`, `:283`; `student/classroom/roadmap.html:32`, `:38`
- **Category:** Accessibility / Feedback
- **Impact:** Users may activate completion without hearing confirmation, and classroom percentages can be announced without context.
- **Standard:** WCAG 2.2 1.3.1, 4.1.2, 4.1.3.
- **Recommendation:** Add a persistent polite status region and programmatically label progressbars from visible progress headings.
- **Suggested command:** `$impeccable harden`

### 7. Question-bank search controls are unnamed

- **Location:** `lecturer/question-bank/list.html:40`, `:51`
- **Category:** Accessibility / Search
- **Impact:** The keyword field and icon-only submit action are not reliably identifiable to assistive technology.
- **Standard:** WCAG 2.2 3.3.2 and 4.1.2.
- **Recommendation:** Add a persistent keyword label and visible or visually-hidden “Tìm kiếm” button text.
- **Suggested command:** `$impeccable harden`

### 8. Subject selection triggers unexpected navigation

- **Location:** `lecturer/question-bank/list.html:35`
- **Category:** Accessibility / Interaction
- **Impact:** Exploring a select option immediately reloads the page and loses focus/context.
- **Standard:** WCAG 2.2 3.2.2.
- **Recommendation:** Apply filters through the existing submit action or explicitly disclose and preserve context for automatic filtering.
- **Suggested command:** `$impeccable harden`

### 9. Admin dashboard displays fabricated operational metrics

- **Location:** `admin/dashboard.html:33`, `:46`, `:59`, `:72`; `DashboardController.java:12`
- **Category:** Data Integrity / Trust
- **Impact:** Administrators can act on false student, mentor, subject, or banned-account counts.
- **Recommendation:** Bind authoritative counts with an unavailable/error state, or remove the cards until real data exists.
- **Suggested command:** `$impeccable harden`

## P2 Minor Findings

### 10. Most pages lack `main`, `h1`, and a stable heading sequence

- **Location:** Student, mentor, classroom, and dashboard templates; stronger exception at `lecturer/question-bank/list.html:18`
- **Category:** Accessibility / Information Architecture
- **Impact:** Landmark and heading navigation varies by route and makes dense pages harder to scan.
- **Recommendation:** Standardize one `main`, one page `h1`, and section `h2` structure in the shared page composition.
- **Suggested command:** `$impeccable layout`

### 11. Sidebar current-location semantics are incorrect and brittle

- **Location:** `layout/student-sidebar.html:31`; `layout/lecturer-sidebar.html:31`; `static/assets/js/sidebarmenu.js:17`
- **Category:** Accessibility / Navigation
- **Impact:** Current location is visual-only; exact-URL matching fails on detail/filter routes, and plain links claim expansion semantics.
- **Recommendation:** Render `aria-current="page"` from server section state and reserve `aria-expanded`/JavaScript toggling for real submenus.
- **Suggested command:** `$impeccable harden`

### 12. Assessment submission and result pages provide weak learning guidance

- **Location:** `student/assessment/take.html:40`; `student/assessment/result.html:31`, `:41`
- **Category:** Error Prevention / Content Design
- **Impact:** Students cannot see unanswered count before submission and receive raw status/metadata instead of outcome interpretation and a next step.
- **Recommendation:** Add answered/total review, localize outcome language, explain the result, and provide a context-aware learning action.
- **Suggested command:** `$impeccable clarify`

### 13. External lesson resources change context and load eagerly

- **Location:** `student/learning/node-detail.html:128`, `:144`, `:162`
- **Category:** Accessibility / Performance
- **Impact:** New tabs can disorient users, and multiple embedded players can load before they are viewed.
- **Recommendation:** Indicate new-tab behavior, add `rel="noopener noreferrer"`, and lazy-load or preview below-the-fold players.
- **Suggested command:** `$impeccable optimize`

### 14. Builder and wizard states rely on visual classes

- **Location:** `lecturer/learning-path/builder.html:471`, `:662`; `lecturer/course-setup/stepper.html:5`
- **Category:** Accessibility / State
- **Impact:** Selected node type and current course-setup step are not announced.
- **Recommendation:** Use radio/pressed-button semantics and an ordered step list with `aria-current="step"`.
- **Suggested command:** `$impeccable harden`

### 15. Learning-path builder exposes excessive actions and implementation language

- **Location:** `lecturer/learning-path/builder.html:86`, `:146`, `:239`, `:316`, `:461`
- **Category:** Cognitive Load / Anti-Pattern
- **Impact:** Mentors must understand `node`, branch rules, `PASS/FAIL`, `PUBLISHED`, and `minScore` while managing several editing modes on one page.
- **Recommendation:** Separate outline work from assessment authoring, progressively reveal branch controls, and translate system concepts into teaching language.
- **Suggested command:** `$impeccable distill`

### 16. Question bank is unpaged and duplicates one edit modal per row

- **Location:** `LecturerQuestionBankController.java:38`; `lecturer/question-bank/list.html:59`, `:88`
- **Category:** Performance / Scalability
- **Impact:** Response size and DOM cost grow linearly with questions and answers.
- **Recommendation:** Add server pagination/search and populate one reusable edit surface for the selected question.
- **Suggested command:** `$impeccable optimize`

### 17. Dependent classroom choices update silently

- **Location:** `lecturer/class/form.html:114`, `:127`, `:152`
- **Category:** Accessibility / Forms
- **Impact:** Learning-path options can change or clear without announcement after subject selection.
- **Recommendation:** Disable path selection until a subject is chosen and announce result count/reset through a polite status region.
- **Suggested command:** `$impeccable harden`

### 18. Student classroom content is duplicated and table-heavy

- **Location:** `student/classroom/list.html:41`, `:81`, `:119`, `:139`
- **Category:** Cognitive Load / Performance
- **Impact:** Desktop resembles an admin record view, while mobile/desktop copies double DOM work and can drift.
- **Recommendation:** Use one responsive, content-first list focused on class, progress/status, and next action; disclose secondary metadata.
- **Suggested command:** `$impeccable distill`

### 19. Roadmap exposes irrelevant branches and lacks list structure

- **Location:** `student/classroom/roadmap.html:62`, `:73`, `:120`, `:168`, `:215`
- **Category:** Cognitive Load / Accessibility
- **Impact:** Students see technical branch paths before assignment, while assistive technology receives no ordered sequence or branch grouping.
- **Recommendation:** Render labelled ordered/nested lists and disclose only the assigned path after assessment.
- **Suggested command:** `$impeccable layout`

### 20. Member and node management rely on wide, unpaged tables

- **Location:** `lecturer/class/members.html:67`, `:125`; `lecturer/class/nodes.html:39`
- **Category:** Responsive / Performance
- **Impact:** Large classes create long pages and force mobile users to pan between identity and actions.
- **Recommendation:** Add pagination/search and a compact mobile representation that keeps actions beside the relevant person or lesson.
- **Suggested command:** `$impeccable adapt`

### 21. Student and mentor dashboards do not prioritize real work

- **Location:** `student/dashboard.html:22`; `lecturer/dashboard.html:21`; `DashboardController.java:19`
- **Category:** Information Architecture / Task Completion
- **Impact:** The entry screen duplicates sidebar links instead of answering what the user should do next.
- **Recommendation:** Shape role-specific continue-learning, pending-request, recent-class, and draft-work sections with one dominant next action.
- **Suggested command:** `$impeccable shape`

### 22. Shared visual rules overuse elevated cards and side stripes

- **Location:** `admin-custom.css:247`, `:254`, `:609`, `:814`; dashboard templates
- **Category:** Anti-Pattern
- **Impact:** Similar visual weight across containers reduces hierarchy and makes learning surfaces feel like generic admin/SaaS templates.
- **Recommendation:** Choose border or modest elevation by hierarchy, remove decorative side stripes, and use fewer containers.
- **Suggested command:** `$impeccable quieter`

### 23. Feature styling bypasses shared semantic tokens

- **Location:** `static/assets/css/question-bank.css:1`; remaining hard-coded values in `admin-custom.css`
- **Category:** Theming / Maintainability
- **Impact:** Contrast, focus, and palette behavior drift across modules and block coherent future theme changes.
- **Recommendation:** Document semantic tokens and migrate feature colors/surfaces/states to them; define dark mode only if product requirements call for it.
- **Suggested command:** `$impeccable document`

### 24. Shared CSS is loaded after visible content

- **Location:** `layout/admin-scripts.html:12`; end-of-body usages across templates
- **Category:** Performance
- **Impact:** Mentora overrides can arrive after base-theme paint, causing restyling and render delay.
- **Recommendation:** Load shared CSS in `head`; keep scripts deferred or at the end of body.
- **Suggested command:** `$impeccable optimize`

## P3 Polish Findings

### 25. Linked logos use generic alternative text and no intrinsic size

- **Location:** all three sidebar fragments at line 14
- **Impact:** “logo” weakly describes the home link and missing dimensions can contribute to layout shift.
- **Recommendation:** Use `alt="Mentora"` and matching intrinsic dimensions.
- **Suggested command:** `$impeccable polish`

### 26. Confirm/alert dialogs and invite-code sharing need product-level feedback

- **Location:** assessment, builder, class, and question-bank actions; invite code in class list/member/form
- **Impact:** Blocking dialogs provide little consequence context, while invite codes require error-prone manual selection.
- **Recommendation:** Standardize contextual confirmation for irreversible actions and add a labelled copy action with polite success feedback.
- **Suggested command:** `$impeccable polish`

## Patterns and Systemic Issues

- Accessibility is being patched after render in `admin-scripts.html` instead of authored in component markup.
- Semantic page composition is inconsistent: question bank uses `main`/`h1`, while most routes start at `h4`/`h5`.
- Internal enums and implementation terms leak into student and mentor copy.
- Admin-template cards and tables remain the default information architecture even where learning tasks need progressive disclosure.
- Responsive support is uneven: student cards and resource grids are good, but tables and page-level overflow hiding avoid rather than solve reflow.
- Feature CSS and inline scripts make behavior local and difficult to keep consistent across modules.

## Positive Findings

- Shared focus-visible styling, 44px mobile targets, reduced-motion support, and readable muted text provide a solid baseline.
- Semantic `aside`/`nav`, real buttons, explicit action names, and text-based status labels are common.
- Empty and no-result states are distinguished, with clear-filter recovery in the shared component.
- Student learning states explain prerequisites and do not rely on color alone.
- Course creation uses progressive steps and a review gate.
- Async question-bank pickers use live regions, disabled states, safe `textContent`, and loading/error/empty feedback.
- Responsive resource grids and student classroom cards prioritize mobile tasks effectively.
- Dashboards are technically light and avoid unnecessary chart libraries.

## Recommended Actions

1. **[P1] `$impeccable harden`**: Fix navigation state/focus, all labels/groups, modal semantics, progress announcements, search names, and accurate admin states.
2. **[P1] `$impeccable adapt`**: Remove global overflow hiding and verify 320px plus 400% zoom reflow.
3. **[P2] `$impeccable shape`**: Redefine student and mentor dashboards around next tasks instead of shortcuts.
4. **[P2] `$impeccable distill`**: Reduce builder, classroom, and roadmap density through progressive disclosure.
5. **[P2] `$impeccable clarify`**: Localize enums, assessment outcomes, branch vocabulary, and destructive-action consequences.
6. **[P2] `$impeccable optimize`**: Move CSS to head, paginate question/member lists, reuse modals, lazy-load media, and remove duplicate responsive DOM.
7. **[P2] `$impeccable document`**: Capture and enforce the semantic token/component system.
8. **[P2] `$impeccable layout`**: Normalize landmarks/headings and semantic roadmap structure.
9. **[P2] `$impeccable quieter`**: Remove default border-plus-shadow cards, side stripes, and undifferentiated card grids.
10. **[P3] `$impeccable polish`**: Complete copy/share feedback and final interaction details after structural fixes.

You can ask me to run these one at a time, all at once, or in any order you prefer.

Re-run `$impeccable audit` after fixes to see the score improve.

## Audit Limitations

- `http://localhost:8080` was not running.
- The local browser runtime was unavailable, so no interactive keyboard, screen-reader, viewport, computed contrast, layout-shift, or performance trace was completed.
- WCAG references were checked against the audit's WCAG 2.2 model; live access to the official W3C source was unavailable during report generation.
- Findings distinguish confirmed source defects from runtime-dependent risks; no screenshot-only claim is included.
