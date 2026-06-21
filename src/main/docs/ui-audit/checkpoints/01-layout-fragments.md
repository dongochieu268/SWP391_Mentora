# Module 1: Layout and Fragments

## Inspected

- `resources/templates/layout/student-sidebar.html`
- `resources/templates/layout/lecturer-sidebar.html`
- `resources/templates/layout/admin-sidebar.html`
- `resources/templates/layout/admin-header.html`
- `resources/templates/layout/admin-scripts.html`
- `resources/templates/components/status-badge.html`
- `resources/templates/components/empty-state.html`
- `resources/templates/components/back-button.html`
- `resources/templates/components/assessment-card.html`
- Direct shared dependencies: `admin-custom.css`, `sidebarmenu.js`, and `app.min.js`
- Reference-only search of fragment usage across templates; page bodies were not opened in this module

## Findings

### [P1] Mobile navigation does not expose or manage its open state

- **Location:** `layout/admin-header.html:13`, `layout/admin-header.html:14`, `static/assets/js/app.min.js:24`, `static/assets/js/app.min.js:34`
- **Category:** Accessibility / Responsive
- **Evidence:** The menu button has an accessible name but no `aria-controls` or `aria-expanded`. Both click handlers only toggle CSS classes and an irrelevant `checked` property; there is no focus move, focus return, or Escape handling.
- **Impact:** Screen-reader and keyboard users cannot determine whether the navigation is open and may tab into an obscured page or lose their place when closing it.
- **Standard:** WCAG 2.2 2.1.1, 2.4.3, 4.1.2.
- **Recommendation:** Model the sidebar as one disclosure/off-canvas component. Bind button and panel with stable IDs, update `aria-expanded`, move focus into the opened panel, close on Escape, and return focus to the opener.
- **Suggested command:** `$impeccable harden`

### [P1] Global overflow suppression can hide reflow failures

- **Location:** `static/assets/css/admin-custom.css:5`, `static/assets/css/admin-custom.css:12`, `static/assets/css/admin-custom.css:17`
- **Category:** Accessibility / Responsive
- **Evidence:** `overflow-x: hidden` is applied to `html`, `body`, `.body-wrapper`, and `.body-wrapper-inner` instead of correcting the overflowing component.
- **Impact:** At narrow widths or 400% zoom, clipped controls and content can become unreachable because the page is forbidden from exposing horizontal overflow.
- **Standard:** WCAG 2.2 1.4.10 and 1.4.4.
- **Recommendation:** Remove page-level suppression, identify the responsible component, and constrain only intentional scrollers such as tables or tab bars.
- **Suggested command:** `$impeccable adapt`

### [P2] Current navigation state is visual-only and uses incorrect expansion semantics

- **Location:** `layout/student-sidebar.html:31`, `layout/student-sidebar.html:44`, `layout/lecturer-sidebar.html:31`, `layout/admin-sidebar.html:31`, `static/assets/js/sidebarmenu.js:17`
- **Category:** Accessibility / Navigation
- **Evidence:** Active links receive only an `active` class. Plain destination links carry `aria-expanded="false"` even though they control no expandable region, and no active link receives `aria-current="page"`.
- **Impact:** Assistive technology does not announce the user's current location, while the expansion state suggests behavior that does not exist.
- **Standard:** WCAG 2.2 1.3.1 and 4.1.2.
- **Recommendation:** Render `aria-current="page"` from `activePage`; remove `aria-expanded` from ordinary links and reserve it for real submenu disclosures.
- **Suggested command:** `$impeccable harden`

### [P2] Active-link detection is brittle for nested routes and query parameters

- **Location:** `static/assets/js/sidebarmenu.js:12`, `static/assets/js/sidebarmenu.js:17`, `static/assets/js/sidebarmenu.js:22`, `static/assets/js/sidebarmenu.js:56`
- **Category:** Navigation / Maintainability
- **Evidence:** The script requires exact URL equality, then attaches submenu-style active toggling to every sidebar link even though the current fragments contain ordinary navigation links.
- **Impact:** Detail pages and filtered URLs can lose their section highlight; clicking a plain link also clears sibling active state before navigation, producing inconsistent orientation.
- **Recommendation:** Make server-rendered `activePage` authoritative. Limit JavaScript to genuine expandable groups and match nested routes by an explicit section key rather than raw URL equality.
- **Suggested command:** `$impeccable harden`

### [P2] Shared visual rules create border-plus-shadow cards and side-stripe accents

- **Location:** `static/assets/css/admin-custom.css:247`, `static/assets/css/admin-custom.css:254`, `static/assets/css/admin-custom.css:609`, `static/assets/css/admin-custom.css:814`
- **Category:** Anti-Pattern / Cognitive Load
- **Evidence:** Shared cards combine a border with a 24px blur shadow, while learning and path patterns use 3-4px colored left borders as decoration.
- **Impact:** Repetition makes most containers compete as elevated objects and pulls the product toward a generic administrative card system rather than a calm, content-first learning surface.
- **Recommendation:** Use elevation or border according to hierarchy, not both by default. Replace decorative side stripes with spacing, type hierarchy, or restrained surface contrast.
- **Suggested command:** `$impeccable quieter`

### [P2] Shared CSS is requested at the end of the document

- **Location:** `layout/admin-scripts.html:12`; fragment usages such as `student/dashboard.html:53` and `lecturer/dashboard.html:81`
- **Category:** Performance
- **Evidence:** `admin-custom.css` is included by a scripts fragment placed at the end of each page body, after the main stylesheet and visible content.
- **Impact:** The first render can show base-theme styling before shared Mentora overrides arrive, causing visible restyling and avoidable render delay.
- **Recommendation:** Split shared head assets from scripts and load the stylesheet in `<head>`; keep executable scripts deferred at the end or use `defer`.
- **Suggested command:** `$impeccable optimize`

### [P3] Brand image metadata is too generic

- **Location:** `layout/student-sidebar.html:14`, `layout/lecturer-sidebar.html:14`, `layout/admin-sidebar.html:14`
- **Category:** Accessibility / Performance
- **Evidence:** Linked logo images use `alt="logo"` and omit intrinsic dimensions.
- **Impact:** The link purpose is vague to screen-reader users, and the browser cannot reserve the logo's exact layout space before the SVG loads.
- **Standard:** WCAG 2.2 1.1.1 and 2.4.4.
- **Recommendation:** Use `alt="Mentora"` when the image names the home link and provide intrinsic `width` and `height` matching the asset ratio.
- **Suggested command:** `$impeccable polish`

## Positive Findings

- Sidebars use semantic `aside` and `nav` regions, and the header uses real buttons for menu and account controls.
- Shared CSS provides visible `:focus-visible` treatment, restrained design tokens, 44px sidebar targets, and a global reduced-motion fallback.
- Status badges include readable text instead of relying on color alone.
- Empty states distinguish an empty dataset from filtered no-results and provide a clear-filter action.
- The back action is centralized as a consistent link component.
- The scripts fragment adds keyboard activation to legacy `role="button"` sidebar controls, reducing an existing keyboard gap.

## Verification Limits

- `http://localhost:8080` was unavailable, and the local browser runtime could not be started in the current environment.
- Focus order, overlay behavior, computed contrast, layout shift, and 320px/400% reflow remain static-code findings or risks until runtime verification is available.

## Module Summary

The shared shell has a promising accessibility and token baseline, but navigation state management is incomplete and responsive fixes rely on hiding overflow. Because these patterns are shared by all roles, the two P1 issues should be treated as platform-level work before page-by-page polish. The component library should retain its focus, touch-target, status-text, and empty-state practices while reducing default card elevation and decorative side stripes.
