# Module 5: Dashboard Module

## Inspected

- `resources/templates/student/dashboard.html`
- `resources/templates/lecturer/dashboard.html`
- `resources/templates/admin/dashboard.html`
- `java/com/edunac/mentora/controller/DashboardController.java`
- Shared dashboard selectors from `resources/static/assets/css/admin-custom.css`

## Findings

### [P1] Admin dashboard presents hard-coded operational metrics as live data

- **Location:** `admin/dashboard.html:33`, `:46`, `:59`, `:72`; `DashboardController.java:12`
- **Category:** Data Integrity / Trust
- **Evidence:** Counts `133`, `12`, `24`, and `5` are literal HTML. The controller supplies only `user` and `activePage`, with no metric model.
- **Impact:** Administrators can make account or academic decisions from fabricated or stale numbers, directly undermining the trustworthy product goal.
- **Recommendation:** Bind metrics to authoritative service queries, add an explicit unavailable state when a query fails, and remove the cards until real data is provided.
- **Suggested command:** `$impeccable harden`

### [P2] Student and mentor dashboards are static shortcut hubs

- **Location:** `student/dashboard.html:22`, `:30`, `lecturer/dashboard.html:21`, `:29`; `DashboardController.java:19`, `:26`
- **Category:** Information Architecture / Task Completion
- **Evidence:** Student dashboard contains one link to classrooms; mentor dashboard contains three links already present in the sidebar. Neither route receives progress, pending work, recent context, or status data.
- **Impact:** The first screen does not answer “What should I do next?” Students must enter a class to find current learning, while mentors must visit several modules to discover pending approvals or unfinished setup.
- **Recommendation:** Make each dashboard a role-specific task overview: student continue-learning and pending assessment; mentor pending join requests, draft course work, and recent classes. Keep navigation in the sidebar.
- **Suggested command:** `$impeccable shape`

### [P2] Dashboards lack a main landmark and top-level heading

- **Location:** `student/dashboard.html:19`, `:25`, `lecturer/dashboard.html:19`, `:24`, `admin/dashboard.html:19`, `:22`
- **Category:** Accessibility / Information Architecture
- **Evidence:** All dashboards use a generic content wrapper and begin with `h4` or `h5`; none uses `main` or `h1`.
- **Impact:** Landmark and heading navigation cannot identify the primary dashboard content or page title consistently.
- **Standard:** WCAG 2.2 1.3.1, 2.4.1, and 2.4.6.
- **Recommendation:** Introduce one `main` region and one role-appropriate `h1`, then structure dashboard sections with `h2`.
- **Suggested command:** `$impeccable layout`

### [P2] Dashboard composition relies on repeated generic cards

- **Location:** `student/dashboard.html:23`, `:34`, `lecturer/dashboard.html:22`, `:33`, `:48`, `:63`, `admin/dashboard.html:25`, `:27`, `:40`, `:53`, `:66`
- **Category:** Anti-Pattern / Cognitive Load
- **Evidence:** Welcome, quick links, and admin metrics are all expressed as similar elevated cards. Admin uses a four-card hero-metric grid; mentor uses three identical icon-heading-description cards.
- **Impact:** Every block receives similar visual weight, reducing prioritization and producing the generic admin/SaaS pattern the product explicitly wants to avoid.
- **Recommendation:** Lead with one next-action area, use compact text links for secondary navigation, and reserve containers/elevation for content that truly needs grouping.
- **Suggested command:** `$impeccable distill`

### [P2] Dashboard copy is generic and mixes product vocabulary

- **Location:** `student/dashboard.html:6`, `:25`, `:26`, `lecturer/dashboard.html:6`, `:25`, `admin/dashboard.html:22`, `:73`
- **Category:** Content Design
- **Evidence:** Every title is “Dashboard”; welcome copy says only “Chọn chức năng bên dưới”, student fallback text is English, and admin abbreviates “TK bị cấm”.
- **Impact:** Copy does not establish role, urgency, or next step and feels like an administrative template rather than an academic learning product.
- **Recommendation:** Use role-specific Vietnamese titles and action-oriented summaries, spell out account states, and describe why the recommended next task matters.
- **Suggested command:** `$impeccable clarify`

### [P3] Mentor dashboard omits an existing primary module

- **Location:** `lecturer/dashboard.html:29`; compare `layout/lecturer-sidebar.html:51`
- **Category:** Navigation Consistency
- **Evidence:** Quick links include learning paths, assessments, and classes but omit the question bank exposed in the mentor sidebar.
- **Impact:** The shortcut grid communicates an incomplete mental model of mentor capabilities.
- **Recommendation:** Do not expand the shortcut grid. Replace it with task/status content and keep the complete module list in one consistent navigation location.
- **Suggested command:** `$impeccable distill`

## Positive Findings

- Dashboards are technically light: no chart library, animation sequence, large image, or page-specific JavaScript is loaded.
- Shortcut cards are real links with meaningful descendant text, and shared CSS provides a visible focus treatment for linked cards.
- Bootstrap columns collapse predictably for tablet and mobile.
- The layouts avoid dense charts and tables, so they provide a clean foundation for a task-focused redesign.
- Admin quick actions use direct, understandable labels rather than icon-only controls.

## Verification Limits

- Runtime remained unavailable. Visual balance, focus outline clipping, perceived loading, and actual role workflows were not interactively tested.

## Module Summary

The dashboards are clean but functionally thin. Student and mentor routes duplicate navigation instead of reducing cognitive load through prioritized tasks, while the admin route crosses a trust boundary by presenting hard-coded counts as operational data. The redesign should resist adding more metric cards: first establish accurate data, then foreground one or two role-specific next actions with compact supporting context.
