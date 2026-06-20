# Module 2: Student Module

## Inspected

- `resources/templates/student/assessment/take.html`
- `resources/templates/student/assessment/result.html`
- `resources/templates/student/learning/node-detail.html`
- Student-specific selectors directly used from `resources/static/assets/css/admin-custom.css`
- Dashboard and classroom list/roadmap templates were intentionally deferred to later modules

## Findings

### [P1] Question choices are not programmatically grouped with the question

- **Location:** `student/assessment/take.html:50`, `student/assessment/take.html:56`, `student/assessment/take.html:58`
- **Category:** Accessibility / Forms
- **Evidence:** Each radio has a correctly associated label, but the group is a generic `div`; the question text is another `div` and is not a `legend` or referenced by `aria-labelledby`.
- **Impact:** Screen-reader users moving among answers may hear the options without the question they answer, especially when several question groups are on one page.
- **Standard:** WCAG 2.2 1.3.1 and 3.3.2.
- **Recommendation:** Render each question as a `fieldset` with the question in `legend`, or bind a `role="radiogroup"` to a stable question heading with `aria-labelledby`.
- **Suggested command:** `$impeccable harden`

### [P1] Embedded lesson video has no accessible title

- **Location:** `student/learning/node-detail.html:128`, `student/learning/node-detail.html:130`
- **Category:** Accessibility
- **Evidence:** The `iframe` only receives a source and `allowfullscreen`; it has no `title` derived from the content name.
- **Impact:** Screen-reader users cannot identify the frame's purpose before entering or skipping it.
- **Standard:** WCAG 2.2 2.4.1 and 4.1.2.
- **Recommendation:** Add a concise, content-specific `title`, for example `Video: {content.title}`, and define a meaningful fallback when the content title is absent.
- **Suggested command:** `$impeccable harden`

### [P1] Successful progress updates are not announced

- **Location:** `student/learning/node-detail.html:180`, `student/learning/node-detail.html:222`, `student/learning/node-detail.html:283`, `student/learning/node-detail.html:289`
- **Category:** Accessibility / Feedback
- **Evidence:** Completion status, button text, progress text, and the progressbar value are replaced after `fetch`, but no result container has `aria-live` or `role="status"`.
- **Impact:** A screen-reader user can activate completion and receive no reliable confirmation that the lesson or overall class progress changed.
- **Standard:** WCAG 2.2 4.1.3.
- **Recommendation:** Use a persistent `role="status"` live region for completion feedback and make the visible progress label programmatically name the progressbar.
- **Suggested command:** `$impeccable harden`

### [P2] Student pages lack a page landmark and clear top-level heading

- **Location:** `student/assessment/take.html:19`, `student/assessment/take.html:29`, `student/assessment/result.html:19`, `student/assessment/result.html:30`, `student/learning/node-detail.html:20`, `student/learning/node-detail.html:32`
- **Category:** Accessibility / Information Architecture
- **Evidence:** Content is nested in generic `div` elements and the first visible page title is an `h4`; no page defines `main` or an `h1`.
- **Impact:** Landmark and heading navigation does not expose a dependable page boundary or title, making long learning pages slower to understand and traverse.
- **Standard:** WCAG 2.2 1.3.1, 2.4.1, and 2.4.6.
- **Recommendation:** Use one `main` landmark with one descriptive `h1` per page, then preserve a logical heading sequence for questions and lesson sections.
- **Suggested command:** `$impeccable layout`

### [P2] Submission gives no answer-completeness context

- **Location:** `student/assessment/take.html:40`, `student/assessment/take.html:42`, `student/assessment/take.html:71`
- **Category:** Cognitive Load / Error Prevention
- **Evidence:** The page shows duration and score but no answered count, unanswered summary, or per-question completion state. Submission uses only `confirm('Nộp bài test này?')`.
- **Impact:** Students can reach a consequential action without knowing whether questions were skipped, increasing avoidable incomplete submissions and review effort.
- **Standard:** Supports WCAG 2.2 3.3.6 for error prevention, though the exact conformance requirement depends on assessment policy.
- **Recommendation:** Show answered/total progress, identify unanswered questions before final submission, and make the confirmation state the consequence clearly.
- **Suggested command:** `$impeccable clarify`

### [P2] Result page exposes system status without learning guidance

- **Location:** `student/assessment/result.html:31`, `student/assessment/result.html:33`, `student/assessment/result.html:41`, `student/assessment/result.html:45`
- **Category:** Content Design / Cognitive Load
- **Evidence:** The result consists of a large score, three boxed metadata values, and a raw `attempt.status`; it does not explain the localized status, pass condition, missed concepts, or recommended next step beyond returning to the roadmap.
- **Impact:** Students receive administrative metadata but limited help interpreting performance or deciding what to learn next.
- **Recommendation:** Localize status labels, lead with an outcome sentence, and provide only the metadata needed to understand the attempt plus a context-aware next action.
- **Suggested command:** `$impeccable clarify`

### [P2] External learning resources open new tabs without warning

- **Location:** `student/learning/node-detail.html:144`, `student/learning/node-detail.html:162`
- **Category:** Accessibility / Security
- **Evidence:** File and link actions use `target="_blank"` without `rel="noopener noreferrer"` or visible/accessible new-window context.
- **Impact:** The unexpected context change can disorient keyboard and screen-reader users; the missing relationship attribute also leaves avoidable opener access.
- **Standard:** WCAG 2.2 3.2.5 advisory context-change guidance.
- **Recommendation:** Indicate “mở trong tab mới” in accessible text and add `rel="noopener noreferrer"`; avoid a new tab when it is not necessary.
- **Suggested command:** `$impeccable harden`

### [P2] Lesson iframe is loaded eagerly

- **Location:** `student/learning/node-detail.html:128`, `student/learning/node-detail.html:130`
- **Category:** Performance
- **Evidence:** Every video iframe is created immediately with no `loading="lazy"` or preview/consent boundary.
- **Impact:** A content-heavy lesson can load multiple third-party players before the student reaches them, increasing network, script, and rendering cost.
- **Recommendation:** Add lazy loading for below-the-fold frames; for many videos, use a preview that instantiates the player on intent.
- **Suggested command:** `$impeccable optimize`

### [P3] Error and submission feedback use browser dialogs

- **Location:** `student/assessment/take.html:42`, `student/learning/node-detail.html:299`, `student/learning/node-detail.html:305`
- **Category:** Interaction / Anti-Pattern
- **Evidence:** Assessment submission uses native `confirm`, while completion failures use blocking `alert` dialogs.
- **Impact:** Feedback interrupts flow, cannot preserve helpful context near the action, and feels inconsistent with the otherwise in-page product UI.
- **Recommendation:** Use an accessible in-page confirmation pattern for unanswered-state review and a persistent error message adjacent to the completion action.
- **Suggested command:** `$impeccable polish`

## Verification Risk

- `student/learning/node-detail.html:123` renders mentor-authored HTML with `th:utext`. The UI audit cannot confirm whether this value is sanitized before rendering. Verify server-side allowlist sanitization before treating rich lesson content as safe.

## Positive Findings

- Pages declare Vietnamese language, responsive viewport metadata, contextual document titles, and consistent return navigation.
- Radio controls have stable IDs and explicit labels.
- The empty-question state removes the submit path rather than inviting an invalid action.
- Prerequisite, locked, pending, completed, and automatic-progress states include text, not color alone.
- The progressbar has numeric ARIA values and JavaScript keeps `aria-valuenow` synchronized.
- The completion button exposes visible loading text and remains disabled during the request.
- Learning resources use a responsive grid that collapses actions to full width below 768px.
- Previous/next lesson actions provide a clear linear learning path.

## Verification Limits

- Runtime remained unavailable; native validation, focus behavior after async completion, iframe behavior, computed contrast, and mobile reflow were not interactively tested.

## Module Summary

The student experience is more content-focused than a traditional admin UI and has clear state copy, responsive resources, and labeled controls. Its main weakness is semantic and feedback plumbing: question groups, embedded media, landmarks, and asynchronous completion are not fully exposed to assistive technology. Product guidance also drops at the most consequential points, particularly unanswered assessment submission and interpreting results.
