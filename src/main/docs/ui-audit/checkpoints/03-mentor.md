# Module 3: Mentor Module

## Inspected

- Assessment: `lecturer/assessment/list.html`, `form.html`, and targeted regions of `detail.html`
- Learning paths: `lecturer/learning-path/list.html`, `form.html`, and targeted regions of `builder.html`
- Learning content: `lecturer/learning/node-contents.html`
- Course setup: `lecturer/course-setup/stepper.html`, `step1.html`, `step2.html`, and `review.html`
- Question bank: `lecturer/question-bank/list.html` and `static/assets/css/question-bank.css`
- Direct controller evidence for question-bank result size: `LecturerQuestionBankController.java`
- Classroom and dashboard templates were intentionally deferred to later modules

## Findings

### [P1] Server-opened content modal remains hidden from assistive technology

- **Location:** `lecturer/learning/node-contents.html:116`, `lecturer/learning/node-contents.html:117`, `lecturer/learning/node-contents.html:119`, `lecturer/learning/node-contents.html:190`
- **Category:** Accessibility / Modal
- **Evidence:** When `openModal` is true, the template adds `show`, `display: block`, and a backdrop, but the modal retains `aria-hidden="true"`. It is not opened through Bootstrap's dialog lifecycle.
- **Impact:** A visible create/edit form is removed from the accessibility tree; focus is not moved into or contained by the modal, which can make the task impossible for screen-reader and keyboard users.
- **Standard:** WCAG 2.2 2.1.1, 2.4.3, and 4.1.2.
- **Recommendation:** Open the modal through one dialog controller after render, bind it to a labelled title, expose `aria-modal="true"`, manage initial/return focus, and remove contradictory manual visibility state.
- **Suggested command:** `$impeccable harden`

### [P1] Form labels are inconsistently associated with controls

- **Location:** `lecturer/assessment/form.html:35`, `lecturer/assessment/form.html:39`, `lecturer/assessment/form.html:48`, `lecturer/assessment/detail.html:205`, `lecturer/assessment/detail.html:207`, `lecturer/question-bank/list.html:97`, `lecturer/question-bank/list.html:138`
- **Category:** Accessibility / Forms
- **Evidence:** The standalone assessment form has visible labels without `for`/`th:for`. Per-question edit modals and the question-bank create/edit forms contain textareas, selects, number fields, answer radios, and answer inputs with labels that are absent or not linked.
- **Impact:** Clicking labels does not reliably focus controls, and assistive technology can announce inputs without their field meaning, particularly inside dense modals.
- **Standard:** WCAG 2.2 1.3.1 and 3.3.2.
- **Recommendation:** Give every field a stable unique ID and explicit label; group correct-answer radios and answer text inputs with `fieldset`/`legend` or equivalent labelled group semantics.
- **Suggested command:** `$impeccable harden`

### [P1] Question-bank keyword and search action have no accessible names

- **Location:** `lecturer/question-bank/list.html:40`, `lecturer/question-bank/list.html:51`
- **Category:** Accessibility / Search
- **Evidence:** The keyword input uses only a placeholder, while the submit button contains only an icon and has no text, title, or `aria-label`.
- **Impact:** Screen-reader users cannot identify the keyword field or search action reliably; placeholder text also disappears while typing.
- **Standard:** WCAG 2.2 3.3.2 and 4.1.2.
- **Recommendation:** Add a persistent label for the keyword and visible or visually-hidden “Tìm kiếm” text for the submit button.
- **Suggested command:** `$impeccable harden`

### [P1] Subject filter triggers an unexpected context change on selection

- **Location:** `lecturer/question-bank/list.html:35`
- **Category:** Accessibility / Interaction
- **Evidence:** `onchange="this.form.submit()"` immediately reloads the result set when a subject is selected.
- **Impact:** Keyboard and screen-reader users can trigger navigation while merely exploring options, losing focus and current context without an explicit apply action.
- **Standard:** WCAG 2.2 3.2.2.
- **Recommendation:** Apply all filters through the existing submit button, or clearly disclose and preserve focus for automatic filtering.
- **Suggested command:** `$impeccable harden`

### [P2] Most mentor pages lack a main landmark and top-level heading

- **Location:** `lecturer/assessment/list.html:19`, `lecturer/assessment/list.html:34`, `lecturer/learning-path/list.html:34`, `lecturer/learning-path/builder.html:29`; compare the stronger `lecturer/question-bank/list.html:18` and `:24`
- **Category:** Accessibility / Information Architecture
- **Evidence:** Most surfaces start in generic wrappers with an `h4` or `h5`; the question-bank page is the exception with `main` and `h1`.
- **Impact:** Page-level navigation and hierarchy vary between mentor workflows, increasing orientation cost in already dense tools.
- **Standard:** WCAG 2.2 1.3.1, 2.4.1, and 2.4.6.
- **Recommendation:** Replicate the question-bank page's `main` plus `h1` structure across mentor surfaces and normalize subsequent heading levels.
- **Suggested command:** `$impeccable layout`

### [P2] Builder selection and wizard progress are visual-only

- **Location:** `lecturer/learning-path/builder.html:469`, `lecturer/learning-path/builder.html:471`, `lecturer/learning-path/builder.html:476`, `lecturer/learning-path/builder.html:662`, `lecturer/course-setup/stepper.html:5`
- **Category:** Accessibility / State
- **Evidence:** Node type buttons toggle a `selected` class and hidden value without `aria-pressed` or radio semantics. The three-step indicator uses mixed links and spans with only `active`/`done` classes and no `aria-current="step"` or ordered-list structure.
- **Impact:** Assistive technology cannot determine the selected node type or current wizard step, weakening two central mentor workflows.
- **Standard:** WCAG 2.2 1.3.1 and 4.1.2.
- **Recommendation:** Implement node type as a labelled radio group or pressed-button group, and render wizard progress as an ordered list with current/completed text and `aria-current="step"`.
- **Suggested command:** `$impeccable harden`

### [P2] Learning-path builder concentrates excessive actions and domain jargon

- **Location:** `lecturer/learning-path/builder.html:86`, `:146`, `:161`, `:169`, `:181`, `:239`, `:316`, `:377`, `:461`, `:514`
- **Category:** Cognitive Load / Anti-Pattern
- **Evidence:** The 60 KB template combines path metadata, node sequencing, branch construction, assessment creation/editing, question editing, destructive actions, and several modal modes. UI copy mixes user language with `node`, `PASS`, `FAIL`, `PUBLISHED`, and `minScore`.
- **Impact:** Mentors must understand implementation concepts and choose among many competing actions before completing the teaching task, pushing the surface toward an expert admin editor.
- **Recommendation:** Separate path outline from assessment authoring, reveal branch controls only when a branch-test node is selected, and translate internal states into consistent Vietnamese task language.
- **Suggested command:** `$impeccable distill`

### [P2] Question-bank DOM grows without pagination and duplicates edit modals

- **Location:** `LecturerQuestionBankController.java:38`, `lecturer/question-bank/list.html:59`, `lecturer/question-bank/list.html:88`
- **Category:** Performance / Scalability
- **Evidence:** The controller retrieves an unpaged active-question collection; the template renders every answer list and a full edit modal for each owned question.
- **Impact:** As the bank grows, response size, DOM nodes, parsing, memory, and modal initialization all grow linearly, slowing the exact workflow intended to reuse many questions.
- **Recommendation:** Add server-side pagination or windowing and use one reusable edit surface populated for the selected question.
- **Suggested command:** `$impeccable optimize`

### [P2] Question-bank styling bypasses the shared token system

- **Location:** `static/assets/css/question-bank.css:1`
- **Category:** Theming / Maintainability
- **Evidence:** The minified stylesheet hard-codes spacing and more than a dozen colors instead of consuming the `--mentora-*` semantic tokens used by shared CSS.
- **Impact:** Focus, contrast, and palette changes can drift from the rest of Mentora, and future theme changes require manual selector-by-selector edits.
- **Recommendation:** Expand the stylesheet for maintainability and map surfaces, text, borders, success states, radius, and spacing to shared semantic tokens.
- **Suggested command:** `$impeccable document`

### [P3] Destructive and publish actions rely on native confirm dialogs

- **Location:** `lecturer/assessment/detail.html:42`, `lecturer/assessment/detail.html:182`, `lecturer/learning-path/builder.html:48`, `lecturer/question-bank/list.html:82`
- **Category:** Interaction / Error Prevention
- **Evidence:** Publish, delete, archive, and cancel flows use browser `confirm` with inconsistent wording and mixed English system terms.
- **Impact:** Consequences are presented without contextual details such as the affected item, reversibility, or downstream classroom impact.
- **Recommendation:** Standardize confirmation copy and use contextual confirmation only for irreversible operations; keep low-risk actions inline.
- **Suggested command:** `$impeccable clarify`

## Positive Findings

- The question-bank page demonstrates the desired semantic baseline with `main`, one `h1`, per-question `article`, `h2`, and ordered answer lists.
- Learning-path accordions bind trigger IDs, expanded state, controls, and panel labels correctly.
- Course creation uses a three-step wizard and a final review page, reducing simultaneous decisions compared with one large form.
- Many forms use native validation, explicit labels, input constraints, and mobile full-screen dialogs.
- The assessment bank picker uses polite live regions, disables unavailable actions, safely builds remote content with `textContent`, and provides empty/error/loading states.
- Content-type fields dynamically update `required` state and provide file type/size guidance.
- Question-bank layout collapses filters, answers, and primary actions cleanly at tablet/mobile breakpoints.

## Verification Limits

- Runtime remained unavailable. Bootstrap focus trapping, modal return focus, keyboard tabs, builder overflow, actual question-bank volume, and computed contrast were not interactively verified.

## Module Summary

Mentor workflows contain several strong foundations: an understandable course wizard, correctly wired accordions, responsive question-bank layout, and thoughtful asynchronous states. The largest gaps are inconsistent form semantics and a builder that exposes too much implementation vocabulary and too many actions at once. The visible-but-`aria-hidden` content modal and unnamed question-bank search controls are release-level accessibility issues; density and scalability should follow immediately after those blockers.
