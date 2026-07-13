# Mentora Design System Master

Nguon chuan giao dien cho Mentora LMS. File nay chuyen hoa baseline UI UX Pro Max sang stack hien tai: Spring Boot, Thymeleaf, Bootstrap, `assets/css/styles.min.css`, va override chung `assets/css/admin-custom.css`.

Raw generated baseline tu UI UX Pro Max duoc luu tai `design-system/mentora/MASTER.md`.

## Design Direction

- Product: education LMS dashboard cho Admin, Lecturer, Student.
- Cam giac: professional, clean, data-dense, de doc, khong phai landing/marketing site.
- Uu tien: clarity, consistency, keyboard accessibility, mobile touch targets, table/card scanability.
- Pattern: dashboard shell voi fixed/off-canvas sidebar, header nho gon, content card/table/forms.

## Approved Style Direction

Mentora chot style: Accessible Education Dashboard.

- He thong phai nhin nhu LMS/dashboard van hanh hoc tap, khong phai landing page hay marketing site.
- Student duoc phep mem hon mot chut qua progress cards, learning cards, empty states than thien.
- Lecturer va Admin can compact hon, data-dense hon, uu tien table, filter, bulk/action controls ro rang.
- Thanh phan dung chung phai dung chung token: mau, radius, shadow, spacing, focus, hover.
- Moi UI moi nen uu tien Bootstrap semantics co san, sau do override bang `admin-custom.css`.
- Khong tao style moi bang inline CSS neu co the dung class/token chung.

## Tokens

| Role | Value | CSS Variable |
| --- | --- | --- |
| Primary | `#1e4db7` | `--mentora-primary` |
| Primary hover | `#173f9e` | `--mentora-primary-hover` |
| Accent | `#f59e0b` | `--mentora-accent` |
| Success | `#067647` | `--mentora-success` |
| Warning | `#b54708` | `--mentora-warning` |
| Danger | `#b42318` | `--mentora-danger` |
| Info | `#175cd3` | `--mentora-info` |
| Text | `#111827` | `--mentora-text` |
| Muted text | `#475467` | `--mentora-muted` |
| Border | `#d9e2ec` | `--mentora-border` |
| Soft border | `#e7edf4` | `--mentora-soft-border` |
| Page bg | `#f6f8fb` | `--mentora-bg` |
| Surface | `#ffffff` | `--mentora-surface` |
| Radius | `8px` | `--mentora-radius` |
| Transition | `180ms ease` | `--mentora-transition` |

## Component Rules

- Buttons: visible hover/focus, `min-height: 44px` for primary touch targets, icon and text aligned with `inline-flex`.
- Cards: radius 8px, subtle border, restrained shadow, stronger shadow only for interactive card links.
- Tables: sticky-feeling visual hierarchy via muted header background, strong header text, internal horizontal scroll on smaller viewports.
- Forms: every field must have label or associated hidden label; focus ring must be visible; placeholders are examples, not labels.
- Badges/status: text plus color, never color alone; use shared `status-badge`/`role-badge` when possible.
- Alerts/modals/offcanvas: preserve Bootstrap semantics; add close button accessible names.
- Sidebar/header: icons are decorative, links must expose clean text labels; mobile sidebar controls need keyboard support.
- Empty states: use clear text and optional decorative icon hidden from assistive tech.

## Accessibility Baseline

- WCAG AA contrast for normal body text.
- Visible `:focus-visible` ring on links, buttons, inputs, selects, nav links, sidebar links, and action buttons.
- Touch targets near 44px on mobile/touch contexts.
- Respect `prefers-reduced-motion`.
- Decorative icon fonts and `iconify-icon` must be `aria-hidden`.
- Icon-only buttons need `aria-label` or meaningful `title` converted to `aria-label`.

## Responsive Checks

Required browser evidence before delivery:

- `375px` mobile
- `768px` tablet
- `1024px` laptop
- `1440px` desktop

Pass criteria:

- No page-level horizontal scroll.
- Sidebar/header does not cover content.
- Tables scroll inside `.table-responsive` or become mobile cards.
- Text does not overlap or escape buttons/cards.
- Form and modal controls remain reachable by touch and keyboard.

## Current CSS Source Of Truth

- Primary override file: `src/main/resources/static/assets/css/admin-custom.css`
- Base vendor/theme CSS: `src/main/resources/static/assets/css/styles.min.css`
- Note: `src/main/resources/static/style.css` is not present in the current project tree.
