# Feature Specification: Node Level Learning System
**Status**: Proposed — 2026-06-30 · sửa lần cuối: **2026-07-06 (rev 06d)** · Lịch sử sửa đổi đầy đủ: xem **Changelog** ở cuối file. Nội dung thuộc rev mới nhất được đánh dấu "(rev 2026-07-06d)" ngay tại tiêu đề mục.
**Supersedes**: Branching Learning Path (specs/lecturer/spec.md §User Story 1, specs/student/spec.md §User Stories 6-8)
**Author**: Do Ngoc Hieu

---

## Overview

Replace the current binary PASS/FAIL branching model with a **multi-level mastery system**. Each `LearningNode` no longer forks into branches; instead it contains N ordered **levels** (configured by the lecturer) that a student can ascend. Every level has its own test drawn from weighted material question banks. The student's score for a node is the highest score they achieve across all levels they attempt.

### Why this change

| Old model | Problem |
|---|---|
| BRANCH_TEST → PASS → FAIL | Binary outcome; no nuance in difficulty |
| Assessment attached to branch rule | One test per node, no progression |
| StudentBranchAssignment drives visibility | Complex graph-traversal logic, hard to maintain |
| Magic PASS/FAIL branch nodes litter the path | Path structure coupled to assessment result |

The new model gives students a **mastery ladder**: attempt the level they feel ready for, earn a score in that level's range, and choose whether to attempt a harder level for a potentially higher score.

---

## Ai đọc mục nào — bản đồ đọc cho team

File này dài; mỗi người chỉ cần đọc kỹ phần của mình + entity liên quan. Mapping
story ↔ phần ↔ người theo work-plan §2:

| Người (phần) | Story bắt buộc | Entity cần nắm | Điều phải nhớ (chi tiết ở work-plan §7) |
|---|---|---|---|
| **Minh** (`material`) | L2 | `Material` (đọc cả ghi chú "What a Material means"), `NodeContent` (2 chủ), `BankQuestion.material_id` | PR content soi checklist 3 guard (work-plan §7.5) |
| **Tùng** (`level-config`) | L1, L3, L4 | `NodeLevel`, `LevelMaterial` | Auto-Level-1 + clone: call-site trong `LearningPathService` thuộc PR của Tùng, chốt chữ ký tuần 1; **ẨN field `durationMinutes`** |
| **admin** (`level-test`) | S3, S4, S5 | `NodeLevelAttempt`, `AttemptQuestion`(+Option), `AttemptGrant` (luật đếm S3 §4 + chặn `COMPLETED` §4b) | Method unlock là MỘT chỗ duy nhất trong `NodeLevelAttemptService` (rev 06d: chỉ check `passed = 1`); commit skeleton `isNextLevelUnlocked` tuần 1 |
| **Đức** (`roadmap`) | S1, S2, S6 | `NodeProgress` (bestScore/bestLevelNumber) | `NodeProgressService` thuộc Đức; unlock chỉ GỌI service của admin, không tự tính; thay 2 query native TRƯỚC bước 7; header 2 chỉ số (S1 §6) |
| **Hiếu** (`foundation` + `results`) | L5, L6, L7 + Database Migration Notes | `AttemptGrant`, classroom `COMPLETED` | File migration follow-up merge TRƯỚC submit flow; chỉ Hiếu merge `dev` |

Mục **"Vá nghiệp vụ kèm theo"** (gần cuối file) là việc NGOÀI 5 phần trên — chỉ là
con trỏ sang work-plan §9.4, phân công riêng trong buổi brief, không thuộc scope
5 nhánh node-levels.

---

## Entity Model

### Entities REMOVED

| Entity | Located in | Reason |
|---|---|---|
| `BranchRule` | `domain/branching/` | Replaced by `NodeLevel.passingScore` |
| `StudentBranchAssignment` | `domain/branching/` | Replaced by `NodeLevelAttempt` |
| `BranchTag` enum | `domain/branching/` | No more MAIN/PASS/FAIL tags |
| `AssignedBranch` enum | `domain/branching/` | Same |
| `LearningNodeType` enum | `domain/branching/` | Nodes are no longer typed |

### Fields REMOVED from existing entities

| Entity | Field | Notes |
|---|---|---|
| `LearningNode` | `nodeType`, `branchTag`, `branchOwnerNode` | No more branch structure |
| `LearningNode` | `isBranchTest()`, `isPassBranch()`, `isFailBranch()`, `isMainBranch()` | Methods deleted |
| `LearningNodeForm` | `nodeType`, `branchTag`, `branchOwnerNodeId`, `minScore`, `assessmentId` | Branch form fields |

### Entities ADDED

> **ID convention**: every existing entity in the codebase uses `Integer` PKs
> (`INT IDENTITY` in SQL Server). All new entities below use `INT`, not `BIGINT` —
> SQL Server rejects FKs between mismatched integer types, so `BIGINT` FKs against
> the existing `INT` PKs (`learning_nodes.id`, `classrooms.id`, `users.id`,
> `bank_questions.id`) would fail at migration time.

#### `Material`
A reusable, subject-scoped collection of learning content + question bank. Can be shared across multiple levels and nodes.

**What a Material means** (decided 2026-07-06): a Material is a **study unit** — one
topic packaged as BOTH the content to learn (`NodeContent` items owned via
`node_contents.material_id`) AND the question pool that examines exactly that content
(`BankQuestion.material_id` tags). Attaching a Material to a level says, in one gesture:
*"this level teaches this topic and tests from this topic."* Cutting the content half was
proposed for cost and REJECTED after costing: a questions-only entity is no longer
meaningfully a "Material", and the real cost is small — the existing node-content stack
(`NodeContentService` + `LecturerNodeContentController` + storage, ~385 lines) is an exact
imitation target (storage is owner-agnostic — `store(file, type)` takes no nodeId), and
node-side queries filter `node_id = ?` so material-owned rows can never leak into node
views. Remaining guards (material-side authz, exactly-one-owner check, delete rule) are
the review checklist in work-plan §7.5.

```
id              INT PK
subject         FK → subjects
title           VARCHAR(255) NOT NULL
description     TEXT
createdBy       FK → users
createdAt       DATETIME
```

**Relationship to the existing `QuestionBank`**: Material does NOT replace
`QuestionBank` (one bank per subject, `UQ_question_banks_subject`). A Material is a
*grouping layer* over that single subject bank: `BankQuestion.material_id` tags a
bank question as belonging to a material. Invariant:
`material.subject == bankQuestion.questionBank.subject` — enforced at tag time
("Câu hỏi không thuộc môn học của material này."). Bank questions with
`material_id = NULL` remain usable by the standalone Assessment flow but are
**never selected by level tests**. After migration all existing questions have
`material_id = NULL`, so lecturers must tag questions into materials (see L2 §6)
before any level test can generate questions.

#### `NodeLevel`
An ordered difficulty tier within a `LearningNode`. Configures the score range and test parameters.

```
id              INT PK
learningNode    FK → learning_nodes
levelNumber     INT NOT NULL (1, 2, 3 …)
title           VARCHAR(255)              -- e.g. "Cơ bản", "Nâng cao"
maxScore        DECIMAL(5,2) NOT NULL     -- maximum score for this level; scores scale 0 → maxScore; BẤT BIẾN khi level đã có attempt (rev 2026-07-06d — L3 §9)
passingScore    DECIMAL(5,2) NOT NULL     -- threshold to unlock next level; 0 < passingScore ≤ maxScore; BẤT BIẾN khi level đã có attempt (rev 2026-07-06d — L3 §9)
maxAttempts     INT NULL                  -- NULL = unlimited
durationMinutes INT NULL                  -- NULL = no time limit; enforcement DEFERRED post-defense (2026-07-06) — UI hides the field, S3 §7 / S4 §7 not implemented this pass
```

**No `questionCount` field** — the total question count for a level's test is not
stored on `NodeLevel`; it is derived as `sum(LevelMaterial.questionCount)` for that
level (see below). Keeping a separate stored total would create two sources of
truth for the same number.

**Score rule**: when a student completes a level test with `correctPercent` (0.0–1.0),
their score = `correctPercent × maxScore`. The floor is always 0 — answering nothing
correctly scores 0 regardless of level (no guaranteed floor; see OQ5, decided).

Example — Level 1 (max 6): 60% correct → 3.6 pts. Level 3 (max 10): 60% correct → 6.0 pts.

**No `minScore` field** — an earlier draft had a `minScore` floor
(`score = minScore + pct × (max − min)`), which guaranteed points for a 0%-correct
submission at higher levels. Dropped 2026-07-04: the floor was judged indefensible
("no correct answers but still scores 8.0"), and a display-only `minScore` would be
a decorative field with no behavior.

#### `LevelMaterial`
Bridge between a `NodeLevel` and a `Material`, specifying exactly how many questions
to draw from that material.

```
id              INT PK
nodeLevel       FK → node_levels
material        FK → materials
questionCount   INT NOT NULL              -- exact number of questions drawn from this material
```

**Simplified from an earlier draft**: this field was originally `weightPercent` (a
% of the level's total, requiring proportional rounding across materials) plus
`easyPercent`/`mediumPercent`/`hardPercent` (a second layer of rounding to split
each material's quota by difficulty, with cross-tier borrowing when a difficulty
was short). Both were cut on 2026-07-04 — an exact integer count per material
avoids the rounding/apportionment problem entirely (integers sum exactly; no
"quotas don't add up to the target" edge cases), and difficulty mix is now the
lecturer's job when choosing which material to attach (tag material content by
difficulty if needed), not something the system enforces per test.

At test-generation time: for each `LevelMaterial`, pull exactly `questionCount`
questions at random (without replacement) from that material's `BankQuestion`
pool. If the material has fewer than `questionCount` questions available, take
however many exist and surface a warning — no cross-material or cross-difficulty
borrowing.

#### `NodeLevelAttempt`
One student's single attempt at one level. Replaces `StudentBranchAssignment` + `AssessmentAttempt` for level-based tests.

```
id              INT PK
nodeLevel       FK → node_levels
student         FK → users
classroom       FK → classrooms
attemptNumber   INT NOT NULL              -- 1-based, enforced ≤ nodeLevel.maxAttempts
score           DECIMAL(5,2)             -- NULL until graded
passed          BIT NOT NULL DEFAULT 0    -- score ≥ passingScore TẠI THỜI ĐIỂM submit; bất biến sau đó (rev 2026-07-05b)
timedOut        BIT NOT NULL DEFAULT 0    -- 1 CHỈ khi auto-finalize do hết giờ (rev 2026-07-06)
status          ENUM(IN_PROGRESS, SUBMITTED)   -- grading is immediate at submit, no separate GRADED state (matches existing AttemptStatus)
startedAt       DATETIME NOT NULL
submittedAt     DATETIME
```

**Why `passed` is stored** (rev 2026-07-06d — thay lý do ratchet ở rev 05b): `passed`
là snapshot kết quả tại thời điểm nộp, nhất quán với phần còn lại của attempt. Vì
`passingScore` bất biến một khi level đã có attempt (L3 §9), giá trị lưu và giá trị
suy ra (`score ≥ passingScore`) luôn trùng nhau — cột tồn tại để đọc nhanh (badge
pass/fail, luật unlock S4 §5), không phải để chống lệch ngưỡng. Timed-out attempts
get `passed = 0`.

**Why `timedOut` is stored** (rev 2026-07-06): a timed-out attempt and a normally-submitted
blank attempt produce identical data (`score = 0`, `passed = 0`, zero `selected` flags), so
result and history views cannot tell them apart; deriving from `submittedAt − startedAt >
durationMinutes` mislabels legitimate submissions inside the 30s grace window. The flag is
written in exactly one place — the timeout auto-finalize path (S3 §7 / S4 §7) — and is
display-only: it never affects grading, unlock, or completion logic. Deferred note
(2026-07-06): timeout enforcement itself is deferred post-defense, but the column ships
in the follow-up migration now — additive-only migrations make adding it later cost a
whole extra file + a team-wide DB re-run; until enforcement lands, nothing writes it.

#### `AttemptGrant` (rev 2026-07-06c)
Per-student extra attempts granted by the lecturer for one level in one classroom —
solves "em lỡ tay nộp / ốm, hết lượt" for REAL instead of the class-wide workarounds
(raising `maxAttempts` or lowering `passingScore` affect everyone). Every grant is
auditable: who granted, when.

```
id              INT PK
nodeLevel       FK → node_levels NOT NULL
student         FK → users NOT NULL
classroom       FK → classrooms NOT NULL
extraAttempts   INT NOT NULL              -- ≥ 1; mỗi lần bấm "+1 lượt" tạo 1 row extraAttempts = 1
grantedBy       FK → users NOT NULL       -- lecturer
grantedAt       DATETIME NOT NULL
```

**Effect on the attempt limit** (S3 §4): the block condition becomes existing count
`≥ maxAttempts + Σ extraAttempts` for this (level, student, classroom). Levels with
`maxAttempts = NULL` (unlimited) never need grants — the UI hides the button (L7 §2).
Ships in the SAME follow-up migration file as `selected`/`passed`/`timed_out` (file
not yet merged — no extra DB re-run for the team).

#### `AttemptQuestion`
Per-attempt snapshot of a generated question. The existing `Question` entity cannot
serve this role: `questions.assessment_id` is `NOT NULL` and Question rows are shared
by all students of an Assessment, while a level test generates a *different random
set per attempt*. Copying content at generation time also means later bank edits
don't affect in-flight or historical attempts.

```
id                      INT PK
attempt                 FK → node_level_attempts NOT NULL
sourceBankQuestion      FK → bank_questions (nullable, traceability only)
content                 VARCHAR(2000) NOT NULL   -- copied from BankQuestion
difficulty              VARCHAR(20) NOT NULL     -- copied
displayOrder            INT NOT NULL             -- shuffled order for this attempt
```

#### `AttemptQuestionOption`
Snapshot of one answer option, copied from `BankQuestionOption` at generation time.

```
id                  INT PK
attemptQuestion     FK → attempt_questions NOT NULL
content             VARCHAR(1000) NOT NULL
isCorrect           BIT NOT NULL
selected            BIT NOT NULL DEFAULT 0       -- ghi 1 lần lúc submit; bất biến sau đó (rev 2026-07-05)
displayOrder        INT NOT NULL                 -- shuffled per attempt
```

**Answer handling** (revised 2026-07-05): grading stays exactly like the current
`StudentAssessmentService.grade()` pattern — options are POSTed once at submit and
graded immediately against `AttemptQuestionOption.isCorrect`. But unlike the
standalone assessment flow, the chosen options **are then persisted**: at submit,
each chosen option gets `selected = 1` on its snapshot row (a single one-time write;
immutable afterward, same as the rest of the snapshot). This makes the snapshot a
complete record of the student's work — question, options, choice, correctness —
which is what powers the student's per-question review (S5 §6) and the lecturer's
result view (L5 §3). There is still **no draft saving**: resuming an `IN_PROGRESS`
attempt re-renders the snapshot questions unanswered; `selected` flags exist only
after submit. The standalone Assessment flow keeps its current no-persistence
behavior — this change applies to level tests only.

### Fields ADDED to existing entities

| Entity | New field | Notes |
|---|---|---|
| `NodeProgress` | `bestScore DECIMAL(5,2)` | Highest score across all level attempts for this node |
| `NodeProgress` | `bestLevelNumber INT` | Level at which bestScore was achieved |
| `NodeContent` | `material_id INT FK (nullable)` | Groups this content item under a Material |
| `NodeContent` | `node_id` becomes **nullable** | Currently `NOT NULL` in both DB and `@JoinColumn`; must be relaxed so material-owned content can exist without a node. CHECK: at least one of `node_id`/`material_id` set |
| `BankQuestion` | `material_id INT FK (nullable)` | Question belongs to this material's pool; NULL = not usable by level tests |

`BankQuestion.difficulty` (`QuestionDifficulty` enum: EASY/MEDIUM/HARD) — **already exists** in the current codebase; no change needed.

---

## Graphify Impact Summary

Analysis of `graph_merged.json` (3750 nodes, 8081 edges) identified **~974 nodes** directly or indirectly touched by this change. Key clusters:

| Layer | Files affected | Action |
|---|---|---|
| `domain/branching/` | 5 classes | **DELETE entire package** |
| `service/branching/` | 2 classes | **DELETE** |
| `repository/branching/` | 2 interfaces | **DELETE** |
| `domain/learningpath/LearningNode` | field + methods | **MODIFY** |
| `domain/learning/NodeProgress` | 2 fields added | **MODIFY** |
| `dto/LearningNodeForm` | branch fields removed | **MODIFY** |
| `dto/StudentRoadmapNodeView/View` | branch state removed | **MODIFY** |
| `service/student/StudentRoadmapService` | branch methods removed | **MODIFY** |
| `service/learningpath/LearningPathService` | `applyBranchingFields()` removed | **MODIFY** |
| `service/learning/NodeProgressService` | `markBranchTestCompleted()` removed | **MODIFY** |
| `controller/student/StudentAssessmentController` | branch trigger removed | **MODIFY** |
| `controller/lecturer/PathBuilderViewSupport` | `buildAddBranchForm()` removed | **MODIFY** |
| `controller/lecturer/LecturerClassWizardController` | validation branch-test ở màn Review (rule + assessment PUBLISHED) removed | **MODIFY** (bổ sung rev 06d — bảng trước sót) |
| Templates: `roadmap.html`, `node-detail.html`, `course-setup/review.html`, builder templates | branch UI + branch validation hints removed (Thymeleaf không lỗi compile — grep trước bước 7, xem mục "Chuyển tiếp" trong Migration Notes) | **MODIFY** |
| Domain, Repo, Service, Controller for 6 new entities | — | **ADD** |

Spec nodes in `graph_merged.json` that reference the old model and must be updated:
- `overview.md` — BranchRule Entity, StudentBranchAssignment Entity, LearningNode Entity (LESSON/BRANCH_TEST)
- `specs/lecturer/spec.md` — User Story 1 (branching), User Story 3 (branch-test creation inline)
- `specs/student/spec.md` — User Stories 6, 7, 8 (branch assessment flow)
- `plan.md` — R8: Define Rule for Branch-Undecided PASS/FAIL Nodes (moot after this change)

---

## Lecturer User Stories

### User Story L1 - Build a level-based learning path (P1)
**Replaces**: specs/lecturer/spec.md §User Story 1 (branching path)

A lecturer creates a learning path and assembles ordered `LearningNode` items. Each node is now a flat lesson — no BRANCH_TEST type, no PASS/FAIL children. After creating a node, the lecturer can open it and configure one or more **levels** with score ranges, passing thresholds, retry limits, and material assignments.

**Acceptance Scenarios**:
1. **Given** an ACTIVE subject, **When** the lecturer POSTs to create a path, **Then** an ACTIVE path is created with no branch-related fields.
2. **Given** the path builder, **When** adding a node, **Then** the form has no `nodeType`, `branchTag`, or `branchOwnerNodeId` fields; all nodes are flat lessons.
3. **Given** a path used by a classroom, **When** they try to delete a node, **Then** it is blocked if any `NodeLevelAttempt` exists for that node's levels ("Node đang có dữ liệu tiến trình học sinh, không thể xóa.").
4. **Given** the path builder, **When** they clone the path, **Then** the clone copies all nodes AND their levels + levelMaterials; `NodeLevelAttempt` rows are NOT cloned.

### User Story L2 - Manage Materials (P1)
**New story** (no equivalent in old spec)

A lecturer creates subject-scoped materials that act as reusable content + question sources. Each material holds multiple `NodeContent` items (TEXT/VIDEO/FILE/LINK) and draws from `BankQuestion` entries tagged to it.

**Acceptance Scenarios**:
1. **Given** a valid title + subjectId, **When** they POST to create a material, **Then** a `Material` row is created owned by the lecturer.
2. **Given** a material they own, **When** they add a NodeContent item (TEXT/VIDEO/FILE), **Then** a `NodeContent` row is saved with `material_id` set (and `node_id` nullable).
3. **Given** a material, **When** they browse the question bank filtered by this material, **Then** only `BankQuestion` rows where `material_id = materialId` are shown.
4. **Given** a material used by at least one `LevelMaterial`, **When** they try to delete it, **Then** it is blocked ("Material đang được sử dụng trong cấu hình level, không thể xóa.").
5. **Given** a material from a different subject, **When** a lecturer tries to link it to a level in their path, **Then** it is rejected ("Material không thuộc môn học của lộ trình này.").
6. **Given** existing `BankQuestion` rows in the subject's bank (all have `material_id = NULL` after migration), **When** the lecturer selects questions and assigns them to a material, **Then** `material_id` is set on those rows; a question from a different subject's bank is rejected ("Câu hỏi không thuộc môn học của material này.").
7. **Given** a bank question already tagged to material A, **When** the lecturer re-assigns it to material B (same subject), **Then** `material_id` is updated — a question belongs to at most one material.

### User Story L3 - Configure levels within a node (P1)
**New story**

For any node in their owned path, the lecturer adds and configures levels defining the mastery ladder.

**Acceptance Scenarios**:
1. **Given** a node they own, **When** they POST a new level with `levelNumber`, `maxScore`, `passingScore`, **Then** the level is saved; `levelNumber` must be unique per node and ≥ 1.
2. **Given** `maxScore ≤ 0`, **When** saving, **Then** rejected ("Điểm tối đa phải lớn hơn 0.").
3. **Given** `passingScore` outside `(0, maxScore]`, **When** saving, **Then** rejected ("Ngưỡng qua level phải nằm trong phổ điểm của level đó.").
4. **Given** `maxAttempts = 0`, **When** saving, **Then** rejected ("Số lần thử phải ≥ 1 hoặc để trống (không giới hạn).").
5. **Given** a level with existing `NodeLevelAttempt` rows, **When** they try to delete that level, **Then** blocked ("Level đã có dữ liệu làm bài, không thể xóa.").
6. **Given** a node with levels, **When** they delete the parent node, **Then** all levels + their levelMaterials are cascade-deleted (attempts block delete — see L1 §3).
7. **Given** a new node created in the path builder, **Then** a default Level 1 is auto-created with it (title "Cơ bản", maxScore 10, passingScore 5) — the lecturer can edit or delete it; a node whose levels all have zero questions behaves as a plain lesson (OQ2, decided).
8. **(rev 2026-07-06c — luật khóa `levelNumber`)** **Given** the node has ≥ 1 `NodeLevelAttempt` on ANY of its levels, **Then**: (a) `levelNumber` of every existing level is **immutable** — editing title/maxAttempts/materials stays allowed as before (riêng `maxScore`/`passingScore` xem §9 — rev 06d), only the ordinal is frozen ("Node đã có dữ liệu làm bài — không thể đánh số lại level."); (b) a new level may only be APPENDED at the top, `levelNumber = max + 1` ("Node đã có dữ liệu làm bài — level mới chỉ được thêm vào cuối thang."); (c) deleting an attempt-free level (allowed per §5) does NOT renumber the remaining levels — a gap (1, 3) is acceptable, history changing meaning is not. A node with NO attempts is fully free (insert/reorder/renumber — authoring phase). **Why**: `NodeProgress.bestLevelNumber` and every "Lv. X/N" display store the ORDINAL, not the level id — renumbering would silently repoint students' history at levels they never took (same failure family OQ3/ratchet and the question snapshots already guard against: config edits must never retroactively change what history means). Mid-course insertion is still possible — add the new level on top and accept the untidy ladder order.
9. **(rev 2026-07-06d — ngưỡng bất biến)** **Given** a level with ≥ 1 `NodeLevelAttempt`, **When** the lecturer edits `maxScore` or `passingScore`, **Then** rejected ("Level đã có dữ liệu làm bài — không thể sửa thang điểm."). Title, `maxAttempts`, materials remain editable (per §8). **Why**: điểm đã chấm = `pct × maxScore` và verdict `passed` so với `passingScore` lúc nộp — sửa thang điểm sau khi có attempt làm lịch sử đổi nghĩa (cùng họ với luật khóa `levelNumber` §8). Level chưa có attempt vẫn chỉnh thang điểm tự do.

### User Story L4 - Assign materials to a level (P1)
**New story**

The lecturer links materials to a level and configures exactly how many questions are drawn from each material.

**Acceptance Scenarios**:
1. **Given** a level they own, **When** they add a `LevelMaterial` with `material` + `questionCount`, **Then** it is saved; `questionCount` must be ≥ 1 ("Số câu hỏi phải lớn hơn 0.").
2. **Given** a material already linked to this level, **When** they try to add it again, **Then** rejected — one row per (level, material) pair (`uq_level_material`); edit `questionCount` on the existing row instead.
3. **Given** a material with fewer bank questions available than the configured `questionCount`, **When** a student starts the level test, **Then** the system takes however many questions exist for that material and shows a warning in the builder UI ("Material chỉ có N câu, ít hơn số câu đã cấu hình.") — no cross-material borrowing.
4. **Given** a LevelMaterial, **When** they remove it, **Then** it is deleted; if no LevelMaterials remain for the level, the level is still valid but will produce a 0-question test (show a warning in the builder UI).

### User Story L5 - View student results in a classroom (P2)
**New story** — closes the "lecturer has zero visibility of student results" gap; strictly read-only.

The lecturer of a classroom views per-student progress and level-test results, including the exact snapshot test each student received.

**Acceptance Scenarios**:
1. **Given** an owned classroom, **When** GET the class progress screen (from the members page), **Then** each student row shows: completed-node count (and %), and per node with levels: `bestLevelNumber` + `bestScore` ("Chưa làm" if no attempts).
2. **Given** a student row, **When** the lecturer opens its detail, **Then** attempts are listed per level: `attemptNumber`, `score`, `submittedAt`, pass/fail — from the stored `attempt.passed` flag (rev 2026-07-05b; từ rev 06d ngưỡng bất biến nên cờ lưu và điểm-so-ngưỡng luôn trùng). Timed-out attempts are labeled "Hết thời gian" via the stored `timedOut` flag (rev 2026-07-06 — applies only once timeout enforcement ships; deferred).
3. **Given** a single attempt, **When** the lecturer opens it, **Then** the snapshot questions + options are rendered read-only with correct answers marked AND the student's chosen answers marked (`selected` — rev 2026-07-05), so the lecturer sees per question what the student picked and where they went wrong — the exact test that student received. Snapshots are immutable; there is no edit/regrade affordance (by design — grading integrity).
4. **Given** a classroom not owned by this lecturer, **Then** rejected.
5. **(rev 2026-07-06c)** **Given** the class progress screen (§1), **Then** below the student rows there is ONE aggregate row per node: class average `bestScore` and the percentage of active students with ≥ 1 attempt `passed = 1` on that node — the lecturer sees at a glance which node the whole class is stuck on.

### User Story L6 - Kết thúc lớp học (P1) — rev 2026-07-06c
**New story** — gives the classroom lifecycle a real END (before this, a class had a
beginning and a middle but no end: semester expiry did nothing, scores flowed nowhere).
`COMPLETED` is a new value of the existing string `status` column — **no migration**;
`ClassroomService.ALLOWED_STATUSES` and guards extend to accept it.

**Acceptance Scenarios**:
1. **Given** an owned classroom with status OPEN or CLOSE, **When** the lecturer confirms "Kết thúc lớp", **Then** status becomes `COMPLETED` and they are redirected to the class-summary screen (§3).
2. **Given** a `COMPLETED` classroom, **Then**: students cannot START a new level-test attempt ("Lớp học đã kết thúc." — S3 §4b); joining was already impossible (join requires OPEN); an `IN_PROGRESS` attempt can still be resumed and submitted (work in flight is never destroyed); all read screens (roadmap, results, QnA, node content) keep working — the class becomes a living gradebook, not a deleted room.
3. **Given** the class-summary screen, **Then** it is the L5 progress table + the per-node aggregate row (L5 §5) + a **"Xuất CSV"** button producing one row per student × node: student name/email, node title, bestLevelNumber, bestScore, attempt count — the lecturer takes the semester's quá trình grades OUT of the system ("căn cứ điểm quá trình").
4. **Given** a `COMPLETED` classroom, **When** the lecturer reopens it (→ OPEN or CLOSE), **Then** all §2 blocks lift — completion is reversible by its owner (mistake recovery), no data is touched either way.
5. **Definition "đã qua môn X"** (consumed by the prerequisite label — see "Vá nghiệp vụ kèm theo"): student is an ACTIVE member of at least one classroom of subject X whose status is `COMPLETED`.

### User Story L7 - Cấp thêm lượt cho một học sinh (P2) — rev 2026-07-06c
**New story** — the real fix for "em lỡ tay nộp / ốm hôm đó": a per-student, audited
grant instead of the class-wide workarounds. This button is the ONLY write affordance
on the otherwise read-only L5 screens.

**Acceptance Scenarios**:
1. **Given** the L5 student-detail screen, a level with `maxAttempts` set, and the student's remaining attempts (per S3 §4 including prior grants) = 0, **Then** a "+1 lượt" button shows; **When** clicked, **Then** an `AttemptGrant` row is created (`extraAttempts = 1`, `grantedBy` = lecturer, `grantedAt = now`) and a flash confirms "Đã cấp thêm 1 lượt.". The button may also show while attempts remain (granting ahead is allowed) — the disabled state is only for unlimited levels (§2).
2. **Given** a level with `maxAttempts = NULL` (unlimited), **Then** no grant button is rendered — grants are meaningless there.
3. **Given** a grant exists, **Then** S3 §4 admits `maxAttempts + Σ extraAttempts` attempts for that (level, student, classroom).
4. **Given** a classroom not owned by this lecturer, **Then** granting is rejected.
5. Grants are never deleted by the UI (audit trail); a mis-click is neutralized by the student simply not using the extra attempt.

---

## Student User Stories

### User Story S1 - View the learning roadmap (P1)
**Updates**: specs/student/spec.md §User Story 3

The roadmap is now a **flat ordered list** of nodes — no PASS/FAIL branch nodes, no `branchDecided` state. Each node shows its highest achieved score and how many levels it has.

**Acceptance Scenarios**:
1. **Given** an active member, **When** GET roadmap, **Then** nodes are ordered by `nodeOrder`; no branch filtering occurs.
2. **Given** a node where `prerequisite` is not completed, **Then** state is `LOCKED`.
3. **Given** a node with `NodeProgress.completed = true`, **Then** state is `COMPLETED`, showing `bestScore` and `bestLevelNumber`.
4. **Given** N visible nodes of which K are completed, **Then** `completionPercent = (K × 100) / N` (unchanged formula; now all visible nodes count equally regardless of levels).
5. **Given** a node with multiple levels configured, **Then** the roadmap card shows "Lv. X/N" where X = bestLevelNumber achieved, N = total levels configured.
6. **(rev 2026-07-06c)** **Given** the roadmap header, **Then** it shows BOTH measures side by side: `Tiến độ: K/N bài` (coverage — unchanged formula §4) AND `Điểm: X/Y` (mastery — X = Σ `bestScore` over the student's nodes, Y = Σ per node of `max(maxScore)` among that node's levels **that have ≥ 1 question**; nodes with no startable level contribute to neither X nor Y). Rationale: with both measures visible on screen, OQ6's "submitted-but-failed still counts as đã học" stops looking like a bug — full progress next to an empty score column is self-explanatory. Không dùng chữ "hoàn thành xuất sắc"; tiến độ dùng chữ "đã học" (OQ6).

### User Story S2 - View a node and its levels (P1)
**Updates**: specs/student/spec.md §User Story 4

On the node detail page the student sees the content for each accessible level and a "Start Level Test" button per level.

**Acceptance Scenarios**:
1. **Given** an accessible node, **When** GET node detail, **Then** all levels are shown in order; materials + their contents are listed per level.
2. **Given** a level where the student has not yet met `passingScore` on any prior attempt, **Then** the next level's "Start Test" button is disabled ("Bạn cần đạt `passingScore` điểm ở Level N trước.").
3. **Given** a student who has already used `maxAttempts` for a level, **Then** "Start Test" for that level is disabled ("Bạn đã hết số lần thử cho level này.").
4. **Given** level 1 is the only level or the student has not yet attempted level 1, **Then** "Start Level 1 Test" is always enabled (no prerequisite check).
5. **Given** a node with no *startable* level (every level has zero questions configured — including the auto-created default Level 1), **Then** the node detail shows only content (no test affordance); the student marks it complete manually (same as current lesson flow). A node with at least one level that has ≥ 1 question is completed by submitting a test, not manually (OQ2, decided).

### User Story S3 - Start a level test (P1)
**Replaces**: specs/student/spec.md §User Story 6 (Take branch test)

The student starts a level test; the system generates a question set from weighted material banks and creates a `NodeLevelAttempt`.

**Acceptance Scenarios**:
1. **Given** the student is not an active member, **Then** rejected ("Bạn chưa được duyệt vào lớp.").
2. **Given** `canAccessNode` fails, **Then** rejected ("Bạn không có quyền truy cập node này.").
3. **Given** the student has an `IN_PROGRESS` attempt for this level, **Then** that attempt is resumed (no new row created).
4. **Given** `maxAttempts` is set and the student's **existing attempt count** for this level + classroom (all statuses, timed-out included) is `≥ maxAttempts + Σ AttemptGrant.extraAttempts` for this (level, student, classroom) (grants — rev 2026-07-06c, see L7), **Then** rejected ("Bạn đã hết số lần thử cho level này."). Clarified rev 2026-07-06: the check compares the existing count, NOT the would-be `attemptNumber` — since `attemptNumber = count + 1` (§5), testing `attemptNumber ≥ maxAttempts` would wrongly deny the last allowed attempt. (IN_PROGRESS rows count but never trigger this: §3 resumes them before this check runs.)
   4b. **Given** the classroom has status `COMPLETED` (rev 2026-07-06c — L6), **Then** starting a NEW attempt is rejected ("Lớp học đã kết thúc."). Resuming an existing `IN_PROGRESS` attempt (§3) and submitting it (S4) remain allowed — ending the class never destroys work in flight.
5. **Given** a valid start, **Then** a `NodeLevelAttempt` is created (`status=IN_PROGRESS`, `startedAt=now`, `attemptNumber` = prior attempt count + 1) and questions are generated:
   - For each `LevelMaterial` linked to this level, pull exactly `questionCount` questions at random (without replacement) from that material's `BankQuestion` pool.
   - If a material has fewer than `questionCount` questions available, take however many exist (no borrowing from other materials).
   - Questions are shuffled; options are shuffled per question.
   - The generated set is copied into `AttemptQuestion` + `AttemptQuestionOption` snapshot rows attached to the attempt (so future bank edits don't affect in-flight or historical tests). The existing `Question`/`Assessment` tables are not touched.
6. **Given** the generated set has 0 questions (no bank questions for any material), **Then** rejected ("Level này chưa có câu hỏi trong ngân hàng.").
7. **[DEFERRED post-defense — 2026-07-06]** **Given** the level has `durationMinutes` set and the student resumes an `IN_PROGRESS` attempt past its deadline (`startedAt + durationMinutes`, +30s grace), **Then** the attempt is auto-finalized (`status = SUBMITTED`, `score = 0`, `timedOut = 1` — rev 2026-07-06) and the student is redirected to the result page ("Hết thời gian làm bài."). A new attempt may then be started if attempts remain. The take page shows a countdown timer that auto-submits at 0 (client-side convenience; the server check is authoritative).

### User Story S4 - Submit level test and get scored (P1)
**Replaces**: specs/student/spec.md §User Stories 7 + 8

The student submits answers; the system grades, computes the scaled score, updates `NodeProgress`, and optionally unlocks the next level.

**Acceptance Scenarios**:
1. **Given** an attempt not owned by this student, **Then** rejected ("Bạn không có quyền nộp lượt làm bài này.").
2. **Given** an attempt from a different classroom, **Then** rejected ("Lượt làm bài không thuộc lớp học này.").
3. **Given** an already-`SUBMITTED` attempt, **Then** return it unchanged (idempotent).
4. **Given** valid submission, **Then**:
   a. `correctPercent = (number of correct answers) / (total questions)` — graded against the attempt's `AttemptQuestionOption.isCorrect` snapshot; the chosen options are then marked `selected = 1` on their snapshot rows (one-time write at submit — rev 2026-07-05).
   b. `score = correctPercent × nodeLevel.maxScore`, rounded to 2 decimal places (floor is 0 — see OQ5, decided).
   c. `attempt.score = score`, `attempt.status = SUBMITTED`, `attempt.submittedAt = now`.
   d. Upsert `NodeProgress` for (student, classroom, learningNode): set `completed = true`, update `bestScore = max(existing bestScore, score)`, update `bestLevelNumber` if this level produced the new best. A tie (`score` equal to the existing `bestScore`) is NOT a new best — `bestLevelNumber` keeps its current value (clarified rev 2026-07-06).
   e. Guard (rev 2026-07-06): if the attempt's snapshot somehow has 0 questions (unreachable via the happy path — S3 §6 blocks it at start; defensive only), finalize with `score = 0`, `passed = false` instead of dividing by zero, and log a warning.
5. **Given** the attempt score ≥ `nodeLevel.passingScore`, **Then** `attempt.passed = true` is stored on the attempt. The next level (if any) is unlocked for this student in this classroom ⟺ any SUBMITTED attempt on the current level has `passed = true` (rev 2026-07-06d — luật ratchet rev 05b đã gỡ: `passingScore` bất biến khi level có attempt, L3 §9, nên không còn tình huống nâng/hạ ngưỡng). This check lives in ONE shared method in `NodeLevelAttemptService` — the roadmap side calls it, never re-implements it.
6. **Given** the node's prerequisite-dependent successor node, **Then** `NodeProgress.completed = true` for this node means the successor is now unlockable (no change to this existing gate logic).
7. **[DEFERRED post-defense — 2026-07-06]** **Given** the level has `durationMinutes` set and the submission arrives after `startedAt + durationMinutes` (+30s network grace), **Then** the answers are NOT graded and no `selected` flags are written; the attempt is finalized with `score = 0`, `status = SUBMITTED`, `timedOut = 1` ("Hết thời gian làm bài." — rev 2026-07-06). `NodeProgress.completed` is still set to `true` (a timed-out attempt counts as a submitted attempt — OQ6 semantics).

### User Story S5 - View level test result (P2)
**Replaces**: specs/student/spec.md §User Story 9

**Acceptance Scenarios**:
1. **Given** an owned, submitted attempt, **When** GET result page, **Then** score, the level's max score, passing threshold, and whether the next level is now unlocked are shown.
2. **Given** the attempt is not owned or belongs to a different classroom, **Then** rejected.
3. **Given** `score >= nodeLevel.passingScore` AND a next level exists AND attempts remain, **Then** a "Thử Level tiếp theo" CTA is shown.
4. **Given** `score < nodeLevel.passingScore` AND attempts remain on the current level, **Then** "Thử lại Level này" CTA is shown.
5. **Given** no attempts remain (hit `maxAttempts`), **Then** both CTAs are hidden; show "Bạn đã hết số lần thử cho level này."
6. **Given** an owned, submitted attempt, **When** GET result page, **Then** below the score each snapshot question is rendered read-only with the student's chosen option and the correct option both marked (per-question right/wrong review — rev 2026-07-05). For a timed-out attempt (identified by the stored `timedOut` flag — rev 2026-07-06; NOT inferred from `score = 0` + missing `selected` flags, since a blank normal submit produces identical data), questions render without chosen marks and the page shows "Hết thời gian làm bài." *(applies only once timeout enforcement ships — deferred 2026-07-06)*

### User Story S6 - View node progress across levels (P2)
**New story**

On the node detail page the student sees a level-by-level history of their attempts and scores.

**Acceptance Scenarios**:
1. **Given** a node with 3 levels, **When** the student has attempted levels 1 and 2, **Then** each attempted level shows: attempt count, best score for that level, and pass/fail indicator — read from the stored `attempt.passed` flag (verdict at submit time — rev 2026-07-05b/06d). Timed-out attempts are labeled "Hết thời gian" via the stored `timedOut` flag (rev 2026-07-06 — applies only once timeout enforcement ships; deferred).
2. **Given** no attempts on a level, **Then** that level row shows "Chưa làm".
3. **Given** `NodeProgress.bestScore` and `bestLevelNumber`, **Then** the node header shows "Điểm cao nhất: X (Level N)".

---

## Database Migration Notes

**No backfill**: pre-existing `learning_nodes` rows do NOT get an auto-created
Level 1 — auto-creation (L3 §7) applies only to nodes created in the path builder
after this feature ships. Pre-existing nodes therefore have zero levels and behave
as plain lessons (manual complete) until a lecturer adds levels. Likewise, all
existing `bank_questions` start with `material_id = NULL` and are invisible to
level tests until tagged — a seed script (subject + materials + tagged questions +
a demo path/classroom) is required for demos and manual testing.

### Tables to CREATE

All PKs are `INT IDENTITY` and all FKs are `INT`, matching the existing schema
(every current table uses INT ids; SQL Server rejects FKs across INT/BIGINT).

```sql
CREATE TABLE materials (
    id INT IDENTITY PRIMARY KEY,
    subject_id INT NOT NULL REFERENCES subjects(id),
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    created_by INT NOT NULL REFERENCES users(id),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

CREATE TABLE node_levels (
    id INT IDENTITY PRIMARY KEY,
    learning_node_id INT NOT NULL REFERENCES learning_nodes(id),
    level_number INT NOT NULL,
    title NVARCHAR(255),
    max_score DECIMAL(5,2) NOT NULL,
    passing_score DECIMAL(5,2) NOT NULL,
    max_attempts INT NULL,
    duration_minutes INT NULL,
    CONSTRAINT uq_node_level UNIQUE (learning_node_id, level_number)
);

CREATE TABLE level_materials (
    id INT IDENTITY PRIMARY KEY,
    node_level_id INT NOT NULL REFERENCES node_levels(id),
    material_id INT NOT NULL REFERENCES materials(id),
    question_count INT NOT NULL,
    CONSTRAINT uq_level_material UNIQUE (node_level_id, material_id)
);

CREATE TABLE node_level_attempts (
    id INT IDENTITY PRIMARY KEY,
    node_level_id INT NOT NULL REFERENCES node_levels(id),
    student_id INT NOT NULL REFERENCES users(id),
    classroom_id INT NOT NULL REFERENCES classrooms(id),
    attempt_number INT NOT NULL,
    score DECIMAL(5,2) NULL,
    passed BIT NOT NULL DEFAULT 0,   -- rev 2026-07-05b
    timed_out BIT NOT NULL DEFAULT 0,   -- rev 2026-07-06
    status NVARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at DATETIME2 NOT NULL,
    submitted_at DATETIME2 NULL,
    CONSTRAINT uq_level_attempt UNIQUE (node_level_id, student_id, classroom_id, attempt_number)
);

CREATE TABLE attempt_questions (
    id INT IDENTITY PRIMARY KEY,
    attempt_id INT NOT NULL REFERENCES node_level_attempts(id),
    source_bank_question_id INT NULL REFERENCES bank_questions(id),
    content NVARCHAR(2000) NOT NULL,
    difficulty NVARCHAR(20) NOT NULL,
    display_order INT NOT NULL
);

CREATE TABLE attempt_question_options (
    id INT IDENTITY PRIMARY KEY,
    attempt_question_id INT NOT NULL REFERENCES attempt_questions(id),
    content NVARCHAR(1000) NOT NULL,
    is_correct BIT NOT NULL,
    selected BIT NOT NULL DEFAULT 0,   -- rev 2026-07-05
    display_order INT NOT NULL
);
```

> **Follow-up ALTER (rev 2026-07-05 + 05b + 06 + 06c)**: the base migration was merged to `dev`
> on 2026-07-04 *without* the `selected`, `passed` and `timed_out` columns. Per the
> work-plan §5 additive-only rule, do NOT edit `db/migration-node-levels.sql` — ship ONE new file:
> ```sql
> ALTER TABLE attempt_question_options ADD selected BIT NOT NULL DEFAULT 0;
> ALTER TABLE node_level_attempts ADD passed BIT NOT NULL DEFAULT 0;
> ALTER TABLE node_level_attempts ADD timed_out BIT NOT NULL DEFAULT 0;
>
> -- rev 2026-07-06c: cấp thêm lượt per-student (L7)
> CREATE TABLE attempt_grants (
>     id INT IDENTITY PRIMARY KEY,
>     node_level_id INT NOT NULL REFERENCES node_levels(id),
>     student_id INT NOT NULL REFERENCES users(id),
>     classroom_id INT NOT NULL REFERENCES classrooms(id),
>     extra_attempts INT NOT NULL,
>     granted_by INT NOT NULL REFERENCES users(id),
>     granted_at DATETIME2 NOT NULL DEFAULT GETDATE()
> );
> ```
> plus the matching entity fields/classes: `selected` on `AttemptQuestionOption`; `passed` and
> `timedOut` on `NodeLevelAttempt`; new `AttemptGrant` entity + repository. Classroom
> `COMPLETED` (L6) needs **no schema change** — the status column already stores strings.

### Tables to ALTER
```sql
-- NodeProgress: add score tracking
ALTER TABLE node_progress ADD best_score DECIMAL(5,2) NULL;
ALTER TABLE node_progress ADD best_level_number INT NULL;

-- NodeContent: optional material grouping; node_id becomes nullable so a
-- content item can belong to a material without a node (see L2 §2).
-- Current schema has node_id NOT NULL — this ALTER is required.
ALTER TABLE node_contents ADD material_id INT NULL REFERENCES materials(id);
ALTER TABLE node_contents ALTER COLUMN node_id INT NULL;
ALTER TABLE node_contents ADD CONSTRAINT ck_node_content_owner
    CHECK (node_id IS NOT NULL OR material_id IS NOT NULL);

-- BankQuestion: optional material tag
ALTER TABLE bank_questions ADD material_id INT NULL REFERENCES materials(id);
```

### Tables to DROP (after migration, once old code is removed)
```sql
DROP TABLE student_branch_assignments;
DROP TABLE branch_rules;
```

### Columns to DROP from learning_nodes
```sql
ALTER TABLE learning_nodes DROP COLUMN node_type;
ALTER TABLE learning_nodes DROP COLUMN branch_tag;
ALTER TABLE learning_nodes DROP COLUMN branch_owner_node_id;
```

### Chuyển tiếp cho lớp/dữ liệu phân nhánh cũ (rev 2026-07-06d)

Spec gốc chỉ nói "tạo mới trước, DROP sau" mà bỏ trống khoảng giữa. Bốn luật sau
đóng khoảng trống đó:

1. **Node nhánh PASS/FAIL cũ vẫn nằm trong `learning_nodes`** theo `nodeOrder` — DROP
   COLUMN chỉ xóa cái nhãn, không xóa cái node. Khi roadmap phẳng merge (phần
   `roadmap`, tuần 2), học sinh lớp cũ sẽ thấy CẢ node PASS lẫn FAIL (nội dung trùng
   lặp/mâu thuẫn) và mẫu số `completionPercent` phình ra. Xử lý: kèm đợt merge PR
   roadmap một script một-lần set `classroom_node_status = HIDDEN` cho mọi node có
   `branch_tag` IN (PASS, FAIL) ở mọi classroom; giảng viên muốn tái dùng nội dung
   thì tự bật node lên và tự dọn trùng lặp.
2. **Node BRANCH_TEST cũ** sau chuyển đổi có 0 level → hành xử như bài học thường
   (hoàn thành tay — OQ2). Chấp nhận việc hạ chuẩn này; thông báo cho giảng viên
   các lớp đang chạy.
3. **Trước khi chạy DROP** (bước 7): dump `student_branch_assignments` + `branch_rules`
   ra file backup ngoài repo. DROP không dump là hủy lịch sử không vết — trái nguyên
   tắc "lịch sử không đổi nghĩa" của chính spec này (L3 §8).
4. **Template Thymeleaf không lỗi compile**: trước khi merge PR bước 7, grep toàn bộ
   `src/main/resources/templates/` tìm `branch`/`nodeType`/`isBranchTest` — cùng họ
   rủi ro với 2 query native đã bắt được (work-plan phần `roadmap`).

---

## Vá nghiệp vụ kèm theo — ngoài node-levels (rev 2026-07-06c)

Rà toàn dự án 2026-07-06 tìm thấy một họ "tính năng mồ côi" (tồn tại nhưng không nối
vào quyết định nào). Nguyên tắc xử lý: **cắm dây vào hoặc rút hẳn ra, không để lơ lửng**.
Đây là spec ngắn cho các việc đó (chi tiết phân công ở work-plan §9):

1. **Nhãn tiên quyết trên trang duyệt thành viên** (cắm dây — môn tiên quyết hiện
   không được đọc ở đâu ngoài CRUD admin): với mỗi yêu cầu PENDING, nếu môn của lớp
   có tiên quyết mà học sinh chưa đạt (định nghĩa "đã qua môn" — L6 §5), hiển thị
   nhãn cảnh báo **"Chưa đạt tiên quyết: <tên môn>"** trên dòng đó. KHÔNG chặn cứng
   ở bước join — học sinh có thể đã học môn đó ngoài Mentora; máy cảnh báo, giảng
   viên quyết (đúng bản chất bước duyệt).
2. **Gỡ học sinh khỏi lớp** (cắm dây — enum `MemberStatus.BANNED` có sẵn nhưng chưa
   code nào set): nút "Gỡ khỏi lớp" cho thành viên ACTIVE → set `BANNED` (không xóa
   row — giữ vết). Mọi guard hiện có đều check ACTIVE nên BANNED tự mất quyền toàn
   hệ thống. Confirm trước khi gỡ; message "Đã gỡ học sinh khỏi lớp.".
3. **Nút "Hiện tất cả node"** trên trang node của lớp + empty-state roadmap học sinh
   "Giảng viên chưa mở bài học nào." (lớp mới mặc định mọi node HIDDEN — bẫy demo).
4. **Bỏ badge "đã xác thực email"** khỏi trang admin accounts (rút ra — không tồn tại
   luồng verify; badge đang hiển thị dữ liệu giả). Xác thực email thật (SMTP) là hạng
   mục SAU bảo vệ — demo không được phụ thuộc dịch vụ mail bên thứ ba.
5. **Label UI**: "Đóng lớp" → **"Đóng ghi danh"** (CLOSE chỉ chặn join mới — code
   đang đúng với cái tên mới); nút kết thúc thật là "Kết thúc lớp" (L6).

---

## Implementation Order

1. **DB migration** — add new tables + new columns; do NOT drop old tables yet. ✅ base đã merge (`b64d1fd`). Còn lại: **file follow-up** (`selected` + `passed` + `timed_out` + bảng `attempt_grants` — xem khối SQL trong Migration Notes).
2. **Domain layer** — add `Material`, `NodeLevel`, `LevelMaterial`, `NodeLevelAttempt`, `AttemptQuestion`, `AttemptQuestionOption`, `AttemptGrant` (rev 06c) (all with `Integer` ids, per codebase convention); modify `LearningNode`, `NodeProgress`, `NodeContent` (relax `node_id` to nullable), `BankQuestion`. ✅ 6 entity đầu đã merge; `AttemptGrant` + các field `selected`/`passed`/`timedOut` đi cùng file follow-up.
3. **Repository layer** — add repos for the 7 new entities; add queries to `NodeLevelAttemptRepository` (find by student+level, count attempts, find best score) and `AttemptQuestionRepository` (find by attempt with options, ordered). ✅ 6 repo đầu đã merge; `AttemptGrantRepository` đi cùng follow-up.
4. **Service layer**:
   - `MaterialService` (CRUD, ownership check)
   - `NodeLevelService` (CRUD, validation of score ranges; khóa `maxScore`/`passingScore` khi level đã có attempt — L3 §9, rev 06d)
   - `NodeLevelQuestionService` (per-material exact-count random selection; writes `AttemptQuestion`/`AttemptQuestionOption` snapshots at attempt start)
   - `NodeLevelAttemptService` (start — gồm luật đếm `maxAttempts + Σ extraAttempts` S3 §4 và chặn lớp `COMPLETED` S3 §4b; submit; grade; score calculation; method unlock dùng chung — chỉ check `passed = 1`, S4 §5 rev 06d)
   - Modify `StudentRoadmapService` — remove all branch methods (`isOnCorrectBranch`, `loadBranchMap`, `resolveBranchDecided`, `shouldShowToStudent`)
   - Modify `NodeProgressService` — remove `markBranchTestCompleted`, update `markNodeCompleted` to accept a score
   - Modify `LearningPathService` — remove `applyBranchingFields()`, `attachAssessment()` (branch-inline creation)
   - Modify `ClassroomService` (rev 06c) — thêm `COMPLETED` vào `ALLOWED_STATUSES` + action "Kết thúc lớp" (L6); logic grant "+1 lượt" (L7) đặt trong service phía kết quả
5. **Controller layer**:
   - `LecturerMaterialController` (`/lecturer/materials`)
   - `LecturerNodeLevelController` (`/lecturer/nodes/{nodeId}/levels`)
   - `StudentLevelTestController` (`/student/classrooms/{classroomId}/levels/{levelId}/take`)
   - Modify `StudentAssessmentController` — remove branch-trigger logic
   - Modify `PathBuilderViewSupport` — remove `buildAddBranchForm()`
   - Controller kết quả giảng viên (rev 04d + 06c — L5/L6/L7, đi từ trang members có sẵn): bảng tiến độ lớp + dòng tổng hợp, chi tiết học sinh + nút "+1 lượt", xem snapshot attempt, nút "Kết thúc lớp" + trang tổng kết + xuất CSV
6. **Templates** — new Thymeleaf pages for material management, level config, level test taking, level result, lecturer results + class summary (L5/L6); update roadmap (header 2 chỉ số — S1 §6) and node-detail.
7. **Remove branching** — delete `domain/branching/`, `service/branching/`, `repository/branching/`; drop old columns from DB. Trước khi DROP làm đủ mục "Chuyển tiếp cho lớp/dữ liệu phân nhánh cũ" (dump bảng + grep template — rev 06d).
8. **Update specs** — amend `specs/lecturer/spec.md` §US1 + §US3; amend `specs/student/spec.md` §US6-8; update `overview.md`.

---

## Open Questions

| # | Question | Default if not resolved |
|---|---|---|
| OQ1 | Should `Assessment` still exist as a standalone entity (for non-level quizzes), or is it fully replaced by the level-test flow? | **DECIDED 2026-07-04: Keep Assessment** for standalone use; level tests generate questions dynamically at attempt start |
| OQ2 | Can a `LearningNode` have BOTH levels AND the old manual-complete flow (for text-only nodes)? | **DECIDED 2026-07-04: The node itself IS its Level 1.** Creating a node auto-creates a default Level 1 (title "Cơ bản", maxScore 10, passingScore 5 — editable). Roadmap runs vertically node→node; a node's levels extend horizontally within it. Completion rule: a node with ≥ 1 level having ≥ 1 question is completed by submitting a test; a node whose levels ALL have zero questions behaves as a plain lesson (manual complete) so content-only nodes never dead-lock the roadmap. Deleting all of a node's levels is allowed and yields the same plain-lesson behavior. |
| OQ3 | When a lecturer edits `passingScore` after students have attempted, does it retroactively re-evaluate? | **DECIDED — thay thế 2026-07-06d: ngưỡng BẤT BIẾN.** `passingScore`/`maxScore` không sửa được một khi level đã có attempt (L3 §9), nên tình huống "sửa ngưỡng sau khi có bài làm" không còn tồn tại. Cơ chế ratchet (rev 05b) đã gỡ; unlock chỉ còn một điều kiện: có attempt `passed = 1` (S4 §5). Cờ `passed` vẫn lưu lúc nộp làm snapshot/đọc nhanh. Level chưa có attempt vẫn chỉnh thang điểm tự do. |
| OQ4 | Should question order within a level be randomized per attempt or fixed per level? | **DECIDED 2026-07-04: Re-drawn + re-shuffled per attempt.** Each new attempt generates a fresh random selection; no per-lecturer toggle for "reuse same test" (considered, rejected to keep the mechanism simplest — revisit only on concrete demand). |
| OQ5 | Score floor at high levels: with the original `score = minScore + pct × (maxScore − minScore)`, a student who unlocked Level 3 (8–10) earned 8.0 by submitting 0% correct — more than a perfect Level 1. Is the guaranteed floor intended? | **DECIDED 2026-07-04: No floor.** Rule is `score = pct × maxScore`; 0% correct = 0 points at every level. `minScore` removed from the entity entirely (a display-only field would be decorative). Higher levels still score higher via larger `maxScore`. |
| OQ6 | `NodeProgress.completed = true` on ANY submitted attempt (S4 §4d), even a 0-score fail of Level 1 — so roadmap `completionPercent` measures "attempted", not "passed". Intended? | **DECIDED 2026-07-04: Yes** — node completion = engaged with the node (submit any attempt); mastery depth is expressed separately via `bestScore`/`bestLevelNumber`. Student-facing UI copy should say "đã học"/neutral wording, not "hoàn thành xuất sắc"-style praise, for a failed-but-submitted state. |
| OQ7 | Question selection was originally `weightPercent` (% of level total, per material) × `easyPercent`/`mediumPercent`/`hardPercent` (% split by difficulty within each material's quota), with cross-tier borrowing when a difficulty tier was short. This is a two-layer rounding/apportionment problem (percentages of percentages must sum back to exact integer totals) plus combinatorial fallback logic — assessed as disproportionately complex for the value delivered. Replaced with an exact `LevelMaterial.questionCount` per material (no percentages, no rounding, no difficulty enforcement). Is losing per-test difficulty-mix control and cross-material proportional weighting acceptable? | **DECIDED 2026-07-04: Yes, dropped** — lecturers control difficulty by choice of material (tag/curate materials by difficulty if needed) rather than the system enforcing a ratio per test. Revisit only if lecturers report a concrete need to mix difficulty within a single material's contribution. |

---

## Changelog

| Rev | Ngày | Nội dung chính | Mục ảnh hưởng |
|---|---|---|---|
| — | 2026-06-30 | Bản đề xuất đầu tiên | toàn bộ |
| 04a | 2026-07-04 | ID kiểu INT khớp schema; snapshot bằng `AttemptQuestion`/`AttemptQuestionOption` (Question không dùng được — `assessment_id NOT NULL`); Material = lớp tag trên `QuestionBank`; `node_contents.node_id` phải nullable; thêm OQ5–OQ6 | Entity Model, SQL |
| 04b | 2026-07-04 | Đơn giản hóa chọn câu hỏi: bỏ weight% + easy/medium/hard% + borrowing, thay bằng `LevelMaterial.questionCount`; bỏ `NodeLevel.questionCount`; thêm OQ7 | LevelMaterial, S3 §5 |
| 04c | 2026-07-04 | OQ5 chốt: KHÔNG sàn điểm — `score = pct × maxScore`, xóa `minScore` | NodeLevel |
| 04d | 2026-07-04 | Chốt toàn bộ OQ1–OQ7; node tự sinh Level 1 mặc định (L3 §7); thêm story L5; enforce `durationMinutes`; bỏ status GRADED; no-backfill + seed script | L3, L5, S3/S4, Migration Notes |
| 05 | 2026-07-05 | Lưu đáp án đã chọn: `AttemptQuestionOption.selected` (ghi 1 lần lúc submit); mở review từng câu (S5 §6) + L5 §3 | S4 §4a, S5 §6, L5 §3, SQL follow-up |
| 05b | 2026-07-05 | Unlock thành state lưu trữ: `NodeLevelAttempt.passed` + luật ratchet (nâng ngưỡng không re-lock, hạ ngưỡng hưởng ngay) | NodeLevelAttempt, S4 §5, OQ3 |
| 06 | 2026-07-06 | Thêm `timedOut` (chỉ đường auto-finalize ghi); luật đếm attempt = count hiện tại ≥ maxAttempts (chống off-by-one); guard 0 câu hỏi; hòa điểm giữ `bestLevelNumber` | NodeLevelAttempt, S3 §4, S4 §4d/4e |
| 06b | 2026-07-06 | **HOÃN enforce `durationMinutes` sau bảo vệ** (UI ẩn field; cột `timed_out` vẫn ship); **GIỮ vai content của Material** sau khi định lượng ~2,5 ngày công | NodeLevel, S3 §7, S4 §7, Material |
| 06c | 2026-07-06 | **Giải thật nghiệp vụ**: L6 Kết thúc lớp (`COMPLETED` — không cần migration + trang tổng kết + xuất CSV); L7 cấp lượt per-student (bảng `attempt_grants`, luật S3 §4 mới, chặn §4b); S1 §6 header 2 chỉ số; L5 §5 dòng tổng hợp lớp; **L3 §8 luật khóa `levelNumber`** (node có attempt → không đánh số lại, level mới chỉ thêm ở đỉnh, xóa không dồn số); mục "Vá nghiệp vụ kèm theo" (nhãn tiên quyết, BANNED, hiện-tất-cả-node, bỏ badge email, label) | L3, L5–L7, S1, S3, SQL follow-up, mục Vá |
| 06d | 2026-07-06 | **Ngưỡng bất biến**: khóa `maxScore`/`passingScore` khi level đã có attempt (L3 §9 mới); **gỡ luật ratchet** — unlock chỉ còn `passed = 1` (S4 §5); OQ3 thay quyết định; mục "Chuyển tiếp cho lớp/dữ liệu phân nhánh cũ" (HIDE node PASS/FAIL cũ, dump trước DROP, grep template); bổ sung `LecturerClassWizardController` vào bảng impact; ẩn form branch từ tuần 2 (work-plan §7.2) | NodeLevel, L3 §8–9, S4 §5, OQ3, L5 §2, S6 §1, Migration Notes, Graphify Impact |
