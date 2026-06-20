# Module 4: Classroom Module

## Inspected

- Student: `student/classroom/list.html` and `student/classroom/roadmap.html`
- Student lesson behavior reused from the completed Student checkpoint for `student/learning/node-detail.html`
- Mentor: `lecturer/class/list.html`, `form.html`, `members.html`, and `nodes.html`
- Direct shared responsive selectors in `static/assets/css/admin-custom.css`

## Findings

### [P1] Classroom form labels are not bound to controls

- **Location:** `lecturer/class/form.html:43`, `:44`, `:50`, `:62`, `:77`, `:90`
- **Category:** Accessibility / Forms
- **Evidence:** All visible labels omit `for`; the class-name input and semester/status selects also have no IDs. Subject and learning-path selects have IDs, but their labels do not reference them.
- **Impact:** Screen-reader users can encounter fields without names, and clicking the visible labels does not consistently focus their controls.
- **Standard:** WCAG 2.2 1.3.1 and 3.3.2.
- **Recommendation:** Give every field a stable ID and explicit `for`, preserving the relationship in both create and edit modes.
- **Suggested command:** `$impeccable harden`

### [P1] Classroom progressbar has no accessible name

- **Location:** `student/classroom/roadmap.html:32`, `:37`, `:38`
- **Category:** Accessibility / Progress
- **Evidence:** The progressbar exposes min, max, and current values but is not labelled by the visible “Tiến độ hoàn thành” text and has no `aria-label`.
- **Impact:** Screen readers may announce only an unlabeled percentage, without identifying it as classroom completion progress.
- **Standard:** WCAG 2.2 1.3.1 and 4.1.2.
- **Recommendation:** Give the visible progress label an ID and reference it with `aria-labelledby`; include the completed/total description when useful.
- **Suggested command:** `$impeccable harden`

### [P2] Student classroom list remains table-heavy on desktop

- **Location:** `student/classroom/list.html:41`, `:47`, `:75`, `:119`
- **Category:** Cognitive Load / Anti-Pattern
- **Evidence:** Pending requests use a five-column table and joined classrooms use an eight-column table. The more approachable classroom cards exist only below the `md` breakpoint.
- **Impact:** The primary student surface resembles an administrative record view on common laptop widths, making the next learning action compete with semester, role, teacher, route, and index metadata.
- **Recommendation:** Use a content-first list or wider cards at all student breakpoints, with class name, subject, progress/status, and the next action visible; move secondary metadata into a detail disclosure.
- **Suggested command:** `$impeccable distill`

### [P2] Classroom items are rendered twice for responsive presentation

- **Location:** `student/classroom/list.html:81`, `:119`, `:139`
- **Category:** Performance / Maintainability
- **Evidence:** Every joined classroom is emitted once as a mobile card and again as a desktop table row, with breakpoint utilities hiding one copy.
- **Impact:** Large enrollments duplicate template work and DOM size, while two independent markups can drift in labels, actions, or accessibility.
- **Recommendation:** Use one semantic list with CSS reflow, or extract one shared item model and render a single responsive structure.
- **Suggested command:** `$impeccable optimize`

### [P2] Roadmap exposes internal branching vocabulary and irrelevant paths

- **Location:** `student/classroom/roadmap.html:73`, `:83`, `:97`, `:120`, `:131`, `:168`, `:179`, `:214`, `:238`
- **Category:** Cognitive Load / Content Design
- **Evidence:** Students see `BRANCH TEST`, `PASS`, `FAIL`, and `node`, including hidden pass/fail branch items before branch assignment.
- **Impact:** Learners must translate implementation concepts and inspect paths they cannot yet use, increasing cognitive load and weakening focus on the next lesson.
- **Recommendation:** Use learner language such as “Bài kiểm tra phân nhánh”, “Lộ trình củng cố”, and “Lộ trình tiếp theo”; disclose only the assigned path after the result while summarizing that alternate paths exist.
- **Suggested command:** `$impeccable clarify`

### [P2] Roadmap sequence and branches lack structural semantics

- **Location:** `student/classroom/roadmap.html:62`, `:63`, `:73`, `:120`, `:168`, `:215`
- **Category:** Accessibility / Information Architecture
- **Evidence:** The ordered learning sequence and branch groups are nested generic `div` elements. Item titles start at `h6`, and four near-identical blocks duplicate state/action markup.
- **Impact:** Assistive technology cannot announce list position or branch grouping; duplicated markup also increases the chance that one state receives different labels or behavior.
- **Standard:** WCAG 2.2 1.3.1.
- **Recommendation:** Render the roadmap as an ordered list with labelled nested branch lists, and extract one node item fragment driven by state/type data.
- **Suggested command:** `$impeccable layout`

### [P2] Dependent learning-path choices change silently

- **Location:** `lecturer/class/form.html:114`, `:127`, `:131`, `:152`
- **Category:** Accessibility / Forms
- **Evidence:** Selecting a subject rebuilds all learning-path options and can clear an earlier selection, but no status message announces the update; before a subject is chosen, every path is shown.
- **Impact:** Screen-reader users may not know the available choices changed, and all users can make a path selection that is silently discarded after changing subject.
- **Standard:** WCAG 2.2 4.1.3.
- **Recommendation:** Disable the path selector until a subject is chosen, then announce the result count and selection reset in a polite live region.
- **Suggested command:** `$impeccable harden`

### [P2] Member and node management depend on wide, unpaged tables

- **Location:** `lecturer/class/members.html:67`, `:125`, `lecturer/class/nodes.html:39`
- **Category:** Responsive / Cognitive Load / Performance
- **Evidence:** Pending members, active members, and class nodes each use horizontally scrolling five- or six-column tables without visible search, pagination, or a compact mobile alternative.
- **Impact:** A large class creates long, dense pages; mobile users must pan between identity and action columns, and repeated management actions compete visually.
- **Recommendation:** Keep tables for desktop mentor work, but add server pagination/search, sticky identity columns where appropriate, and a compact mobile row/card that keeps the action adjacent to the member or lesson name.
- **Suggested command:** `$impeccable adapt`

### [P2] Classroom management exposes raw system states

- **Location:** `lecturer/class/form.html:58`, `:92`, `:94`, `lecturer/class/nodes.html:44`, `:72`, `:73`, `lecturer/class/members.html:143`
- **Category:** Content Design / Consistency
- **Evidence:** UI copy includes `ACTIVE`, `OPEN`, `CLOSE`, `VISIBLE`, `HIDDEN`, `node`, and `TA`, sometimes followed by Vietnamese and sometimes not.
- **Impact:** Mentors must learn the domain model instead of receiving consistent task language, while student-facing copy uses a different vocabulary.
- **Recommendation:** Centralize localized status labels and use “bài học/nội dung”, “đang mở/đã đóng”, “đang hiển thị/đang ẩn”, and “trợ giảng” consistently.
- **Suggested command:** `$impeccable clarify`

### [P3] Invite code has no direct sharing action

- **Location:** `lecturer/class/list.html:70`, `lecturer/class/members.html:35`, `lecturer/class/form.html:31`
- **Category:** Task Efficiency
- **Evidence:** The invitation code is displayed as text in three places without a copy/share control or copied confirmation.
- **Impact:** Mentors must select the code manually and can easily omit or alter characters when sharing it.
- **Recommendation:** Provide one labelled copy action with a polite “Đã sao chép mã lớp” confirmation and keep the code selectable as fallback.
- **Suggested command:** `$impeccable delight`

## Positive Findings

- The join-class form has an explicit label, native required validation, clear example format, and mobile stacking.
- Student classroom cards expose one dominant “Xem lộ trình” action and secondary metadata in a quiet hierarchy.
- Roadmap states use text and icons in addition to color; locked actions are truly disabled and prerequisite explanations name the required lesson.
- Progress exposes numeric current/min/max values and visible completed/total context.
- Mentor class cards reduce the list view to two primary actions and include an instructional empty state.
- Member and node action buttons have explicit accessible names rather than relying on icons.
- Tables use real headers and responsive scroll containers, preserving data relationships better than arbitrary grids.

## Verification Limits

- Runtime remained unavailable. Actual table overflow, screen-reader output for progress, live updates to dependent selects, copy workflow, large-class performance, and mobile action proximity were not interactively verified.

## Module Summary

Classroom flows communicate progress and availability more clearly than a conventional admin system, especially on student mobile cards and locked roadmap states. However, desktop student views and mentor management still lean heavily on dense tables, while the roadmap exposes internal branching concepts and paths that are not yet actionable. Accessibility fixes for form labels and progress naming should lead, followed by vocabulary cleanup and progressive disclosure of classroom data.
