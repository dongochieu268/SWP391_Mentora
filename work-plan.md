# Node Levels — Kế hoạch phân công (5 người)

**Ngày**: 2026-07-04 · **Cập nhật**: 2026-07-06 (nhận các mục hợp lệ từ đề xuất thành viên:
cờ `timedOut`, task thay 2 query native branching, luật đếm attempt, thứ tự merge/deploy;
thêm §8 kỷ luật bảo vệ; **hoãn enforce `durationMinutes` sau bảo vệ** — cắt chủ động;
vai content của Material GIỮ NGUYÊN (chốt cùng ngày sau khi định lượng ~2,5 ngày công,
xem ghi chú dưới entity `Material` trong spec); cùng ngày thêm **§9 kịch bản bảo vệ nghiệp vụ**
sau khi rà toàn dự án, rồi nâng lên hướng **"giải thật"** (spec rev 2026-07-06c):
L6 Kết thúc lớp + tổng kết + CSV, L7 cấp lượt per-student (`attempt_grants`),
S1 §6 header 2 chỉ số, nhãn tiên quyết + vá kèm theo — xem §9.4.
Trước đó 2026-07-05: Nền móng merge `dev` — tick xong; tách L5 +
bước 7 sang Hiếu) · **Căn cứ**: `spec.md` (rev 2026-07-06d — gỡ ratchet, ngưỡng bất biến; mục Chuyển tiếp branching) + phân tích git toàn bộ lịch sử repo

---

## 1. Phân tích khối lượng đã làm (git, mọi branch, bỏ merge commit)

Alias đã gộp: `dongochieu268` = Do Ngoc Hieu; `Zlnf-se` = admin (cùng email dlnf2k6mt).
Số dòng static/templates bị nhiễu nặng do commit vendor assets (Bootstrap, FontAwesome…) nên
xếp hạng dựa trên **code Java** và số commit.

| Thành viên | Commits | Java (dòng +) | Mảng sở trường (theo git) |
|---|---|---|---|
| **Do Ngoc Hieu** | 96 | ~6 000 | Lecturer controllers, path builder, learningpath/node, docs, merge/release |
| **admin (dlnf2k6mt)** | 45 | ~3 800 (+ refactor xóa ~17k dòng) | Learningpath, student roadmap, **branching**, refactor lớn |
| **Kieu Tuan Minh** | 29 | ~4 800 | **Assessment/question bank** (nhiều nhất), learningpath, lecturer UI |
| **Le Tung** | 22 | ~4 000 | **Assessment/quiz**, classroom, subject/semester |
| **Pham Minh Duc** | 19 | ~4 000 | Q&A, **student-facing UI**, classroom |

Nhận xét dùng để chia việc:
- Hieu là người code nhiều nhất và giữ vai trò merge/tích hợp → giao phần **nền móng chặn đầu** (ai cũng phụ thuộc) + điều phối. *(Cập nhật 2026-07-05: nền móng xong sớm → nhận thêm L5 kết quả giảng viên — việc chỉ-đọc, không phụ thuộc ai — và bước 7 gỡ branching, để Material và Làm bài không phình to nhất nhóm.)*
- admin là người hiểu branching + student flow sâu nhất → giao phần **sinh đề/chấm điểm** (lõi thuật toán khó nhất); gỡ điểm gọi branching trong student flow thuộc scope này, còn cú xóa toàn cục là bước 7 của Hiếu.
- Minh và Tung là 2 người mạnh nhất về assessment/question bank → chia nhau 2 vertical phía lecturer (Material và Level config).
- Duc mạnh về UI phía student, khối lượng Java thấp nhất → giao vertical **roadmap/node-detail** (nặng template, service vừa phải).

---

## 2. Phân công

Slug trong ngoặc dùng cho tên nhánh + prefix commit (xem §5).

### Nền móng — DB + Domain + Repository (`foundation`) — **Do Ngoc Hieu** ✅ ĐÃ MERGE
> Bước 1–3 của Implementation Order. **Đã merge vào `dev` 2026-07-04 (`b64d1fd`,
> nhánh `feat/foundation-db-domain-repo`)** — 4 phần còn lại rebase lên `dev` và bắt đầu.

- [x] Script migration: 6 bảng CREATE + các ALTER (`node_progress`, `node_contents` gồm cả `node_id` nullable + CHECK, `bank_questions`) — theo đúng SQL trong spec (INT, không BIGINT) → `db/migration-node-levels.sql`
- [x] 6 entity mới: `Material`, `NodeLevel`, `LevelMaterial`, `NodeLevelAttempt`, `AttemptQuestion`, `AttemptQuestionOption` (id `Integer`) → `domain/level/`
- [x] Sửa entity: `NodeProgress` (+bestScore, +bestLevelNumber), `NodeContent` (node nullable + material), `BankQuestion` (+material), `LearningNode` (chưa xóa field branch — để bước 7)
- [x] 6 repository + query đặt hàng trước: `NodeLevelAttemptRepository` (find by student+level, count, best score), `AttemptQuestionRepository` (by attempt, kèm options, ordered) → `repository/level/`
- [x] ~~Chốt OQ1–OQ7 với team~~ — **đã chốt toàn bộ 2026-07-04** (xem bảng Open Questions trong spec); việc còn lại: phổ biến cho team trong buổi brief
- [x] Script seed dữ liệu demo: 1 subject + 2–3 material × 15–20 câu đã tag + 1 path 3 node × 2 level + 1 classroom → `db/seed-node-levels-demo.sql`
- [ ] **Follow-up rev 2026-07-05/05b/06/06c**: MỘT file migration MỚI (không sửa file đã merge — quy tắc §5) gồm 3 cột: `attempt_question_options ADD selected`, `node_level_attempts ADD passed`, `node_level_attempts ADD timed_out` (đều `BIT NOT NULL DEFAULT 0`) **+ bảng `attempt_grants`** (cấp thêm lượt per-student — L7, rev 06c; đi ké chuyến "chạy lại DB" này, không tốn đợt riêng); kèm field `selected` trong `AttemptQuestionOption`, `passed` + `timedOut` trong `NodeLevelAttempt`, entity + repo `AttemptGrant`. Làm TRƯỚC khi admin viết submit flow; **ping nhóm "chạy lại DB" ngay khi merge** — admin không bắt đầu `submitAttempt()` trước khi có xác nhận. Cột `timed_out` giữ trong file dù enforcement đã hoãn (2026-07-06) — thêm sau sẽ tốn file migration mới
- [ ] Bổ sung seed: vài `node_level_attempts` SUBMITTED + snapshot `attempt_questions`/`attempt_question_options` (có `selected`) — seed hiện tại KHÔNG có attempt nào nên L5 không có gì để hiển thị khi dev/test UI
- Vai trò xuyên suốt: review + merge tất cả PR, giữ tích hợp

### Kết quả giảng viên + gỡ branching (`results`) — **Do Ngoc Hieu**
> User Story L5 + Bước 7. Nhận thêm vì Nền móng xong sớm; hai việc này trước đây
> gộp vào Material (Minh) và Làm bài (admin) làm 2 phần đó phình to nhất nhóm.

**L5 — Màn hình kết quả cho giảng viên** (view-only, spec §L5):
- [ ] Từ trang members (`LecturerMemberController`, `/lecturer/classes/{classroomId}/members`) → bảng học sinh × node: bestLevel + bestScore, "Chưa làm" (L5 §1)
- [ ] Bấm học sinh → lịch sử attempt per level: attemptNumber, score, submittedAt, pass/fail đọc từ cờ `passed` đã lưu (L5 §2, rev 2026-07-05b)
- [ ] Bấm attempt → xem snapshot đề read-only, đáp án đúng + **đáp án sinh viên đã chọn** được đánh dấu (thấy sai câu nào — L5 §3, rev 2026-07-05). Không có sửa/chấm lại — snapshot bất biến theo thiết kế
- [ ] Chặn classroom không thuộc sở hữu (L5 §4)
- [ ] **Dòng tổng hợp lớp** trên bảng L5 (L5 §5, rev 06c): mỗi node một dòng aggregate — điểm trung bình + tỷ lệ đã qua (`passed`) của cả lớp. Trả lời câu "cả lớp gãy ở node nào để dạy lại" — giá trị cho NGƯỜI DẠY, không chỉ xem

**L6 — Kết thúc lớp học** (rev 06c — cho vòng đời lớp một cái CUỐI thật, xem §9):
- [ ] Trạng thái `COMPLETED` cho classroom — **không cần migration** (cột status là string validate trong `ClassroomService.ALLOWED_STATUSES`); nút "Kết thúc lớp" (confirm) từ OPEN/CLOSE, mở lại được (L6 §4)
- [ ] Trang tổng kết lớp = bảng L5 + dòng tổng hợp + nút **"Xuất CSV"** (học sinh × node: bestLevel, bestScore, số lượt — L6 §3); redirect tới đây sau khi kết thúc lớp
- [ ] Banner nhắc trên dashboard giảng viên khi học kỳ quá `endDate` mà lớp chưa COMPLETED: "Học kỳ đã kết thúc — tổng kết lớp" (không tự đóng lớp thay người dạy)
- [ ] Phối hợp admin: check chặn start attempt khi lớp COMPLETED nằm trong `NodeLevelAttemptService` (S3 §4b — admin viết, Hiếu cung cấp trạng thái)

**L7 — Cấp thêm lượt** (rev 06c — fix thật cho "em lỡ tay nộp/ốm, hết lượt"):
- [ ] Nút **"+1 lượt"** trên màn chi tiết học sinh của L5 → tạo `AttemptGrant(extraAttempts=1, grantedBy, grantedAt)`; ẩn với level `maxAttempts = NULL`; chỉ lecturer sở hữu lớp; flash "Đã cấp thêm 1 lượt." (L7)
- **Không bị chặn bởi người khác**, nhưng có 2 tiền đề đều là việc của chính Hiếu, làm theo thứ tự (cập nhật 2026-07-06 — câu "không bị chặn bởi ai" cũ đã hết đúng sau rev 05/05b): (1) migration follow-up `selected`/`passed`/`timed_out` merge trước — L5 §2–§3 đọc các cờ này; (2) seed attempt SUBMITTED có trước — không thì màn hình trắng. Vẫn không cần chờ service ghi của phần Làm bài hay `NodeProgressService` của Roadmap. Follow-up trễ quá ngày đầu tuần 2 → báo nhóm để ưu tiên.

**Bước 7 — gỡ branching** (SAU khi cả 5 phần merge, tuần 3):
- [ ] PR riêng `chore/remove-branching`: xóa `domain/branching/`, `service/branching/`, `repository/branching/`, field branch trong `LearningNode` + `LearningNodeForm`, `buildAddBranchForm()` (phối hợp Tùng), migration DROP bảng/cột cũ
- Admin chỉ hỗ trợ rà lại các điểm gọi trong student flow — không còn là đầu việc của phần Làm bài

### Material — phía giảng viên (`material`) — **Kieu Tuan Minh**
> User Story L2. Phụ thuộc Nền móng.

- [ ] `MaterialService`: CRUD + ownership + chặn xóa khi có LevelMaterial + validate subject
- [ ] Gắn/gỡ `BankQuestion.material_id` (L2 §6–7, gồm chặn khác subject: "Câu hỏi không thuộc môn học của material này.")
- [ ] Thêm `NodeContent` thuộc material (node_id NULL) — **GIỮ, đã chốt 2026-07-06** sau khi định lượng: nhân bản stack node-content có sẵn (`NodeContentService` + `LecturerNodeContentController` + `NodeContentStorageService`, ~385 dòng; storage dùng lại nguyên xi — `store(file, type)` không key theo node). Làm SAU CRUD + gắn câu hỏi; PR này soi checklist §7.5
- [ ] `LecturerMaterialController` (`/lecturer/materials`) + templates: danh sách, form, gắn câu hỏi (lọc bank theo material), quản lý content
- [ ] Unit test service (validate + block-delete)
- ~~L5 — Màn hình kết quả cho giảng viên~~ → **chuyển sang phần `results` (Hiếu)** 2026-07-05

### Cấu hình Level — phía giảng viên (`level-config`) — **Le Tung**
> User Stories L3 + L4. Phụ thuộc Nền móng; giao diện chung với phần Material là entity `Material` (chỉ cần id + title).

- [ ] `NodeLevelService`: CRUD level + toàn bộ validation L3 (maxScore>0, 0<passingScore≤maxScore, maxAttempts≥1, levelNumber unique, chặn xóa khi có attempt, cascade xóa node, **luật khóa `levelNumber` — L3 §8 rev 06c**: node có attempt → không đánh số lại + level mới chỉ thêm ở đỉnh `max+1` + xóa không dồn số; **luật khóa thang điểm — L3 §9 rev 06d**: level đã có attempt → không sửa `maxScore`/`passingScore` ("Level đã có dữ liệu làm bài — không thể sửa thang điểm."); message nguyên văn trong spec)
- [ ] Quản lý `LevelMaterial`: `questionCount` trực tiếp mỗi material (không còn %, không còn easy/medium/hard), chặn thêm trùng material vào cùng 1 level (L4 §2), cảnh báo level 0 câu hỏi (L4 §4)
- [ ] `LecturerNodeLevelController` (`/lecturer/nodes/{nodeId}/levels`) + templates cấu hình level trong path builder — **ẨN field `durationMinutes`** khỏi form (enforcement hoãn sau bảo vệ, 2026-07-06; cột DB giữ nguyên, NULL = không giới hạn)
- [ ] Tự sinh Level 1 mặc định khi tạo node ("Cơ bản", maxScore 10, passingScore 5 — sửa/xóa được; L3 §7) — gồm cả call-site trong `LearningPathService.addNode()`, Tùng sửa trong PR của mình; chốt chữ ký method (vd `createDefaultLevel(nodeId)`) với Hiếu tuần 1, không chờ UI
- [ ] Clone path copy levels + levelMaterials, KHÔNG copy attempts (L1 §4) — gồm cả call-site trong `LearningPathService.clonePath()`; nên tách PR nhỏ riêng
- [ ] Gỡ form branch khỏi path builder: `LearningNodeForm` (bỏ nodeType/branchTag/…), `PathBuilderViewSupport.buildAddBranchForm()` (phối hợp Hiếu ở bước 7 — dời tuần 3, xem §7.2)

### Làm bài + chấm điểm — phía học sinh (`level-test`) — **admin (dlnf2k6mt)**
> User Stories S3 + S4 + S5 — phần thuật toán lõi. Phụ thuộc Nền móng.

- [ ] `NodeLevelQuestionService`: với mỗi `LevelMaterial`, lấy đúng `questionCount` câu ngẫu nhiên (không tính quota %, không phân bổ độ khó); nếu material thiếu câu thì lấy hết bấy nhiêu (S3 §5); shuffle; ghi snapshot `AttemptQuestion`/`AttemptQuestionOption`
- [ ] `NodeLevelAttemptService`: start (resume IN_PROGRESS, chặn maxAttempts theo **count hiện tại** `≥ maxAttempts + Σ extraAttempts` từ `attempt_grants` — S3 §4 rev 2026-07-06c, tránh off-by-one; **chặn start mới khi classroom `COMPLETED`** "Lớp học đã kết thúc." — S3 §4b, resume/submit attempt đang dở vẫn cho; chặn 0 câu hỏi), submit (idempotent, chấm từ snapshot, điểm = `pct × maxScore` — sàn là 0, làm tròn 2 chữ số; guard snapshot 0 câu → điểm 0 không chia 0 — S4 §4e; **sau khi chấm, ghi `selected = 1` lên option đã chọn** — rev 2026-07-05, S4 §4a — và ghi `passed = (score ≥ passingScore)` — rev 2026-07-05b/06d (ngưỡng bất biến); đường timeout HOÃN sau bảo vệ — khi bật lại mới có: không ghi selected, `passed = 0`, `timedOut = 1`), upsert `NodeProgress.bestScore/bestLevelNumber` (hòa điểm → giữ nguyên bestLevelNumber — S4 §4d), **method unlock chung**: unlocked ⟺ có attempt `passed = 1` (S4 §5 rev 06d — ratchet đã gỡ, ngưỡng bất biến)
- ~~Enforce `durationMinutes` (S3 §7 + S4 §7)~~ → **HOÃN sau bảo vệ (quyết 2026-07-06)**: form cấu hình ẩn field duration nên không level nào đặt giờ được; admin bỏ cả 2 đường auto-finalize lẫn timer đếm ngược. Cột `timed_out` vẫn nằm trong migration follow-up (migration chỉ-thêm, bổ sung sau tốn 1 file mới + cả nhóm chạy lại DB) nhưng đợt này không ai ghi nó
- [ ] `StudentLevelTestController` (`/student/classrooms/{classroomId}/levels/{levelId}/take`) + templates làm bài & kết quả (CTA theo S5; trang kết quả có review từng câu: đáp án đã chọn vs đáp án đúng — S5 §6, rev 2026-07-05. Timer đếm ngược + nhãn "Hết thời gian" theo cờ `timedOut`: HOÃN cùng enforcement duration)
- [ ] Gỡ trigger branch khỏi `StudentAssessmentService`/`StudentAssessmentController` (chỉ gỡ điểm gọi trong scope này — KHÔNG xóa file branching, xem quy tắc §5)
- [ ] Unit test: chọn câu hỏi khi material đủ/thiếu `questionCount` + công thức điểm
- ~~Bước 7 — gỡ branching~~ → **chuyển sang phần `results` (Hiếu)** 2026-07-05; admin chỉ hỗ trợ rà điểm gọi trong student flow

### Roadmap + Node detail — phía học sinh (`roadmap`) — **Pham Minh Duc**
> User Stories S1 + S2 + S6. Phụ thuộc Nền móng; cần luật unlock thống nhất với phần Làm bài.

- [ ] `StudentRoadmapService`: bỏ branch (`isOnCorrectBranch`, `loadBranchMap`, `resolveBranchDecided`, `shouldShowToStudent`) → danh sách phẳng theo `nodeOrder`; giữ LOCKED theo prerequisite; completionPercent giữ công thức cũ
- [ ] `NodeProgressService`: bỏ `markBranchTestCompleted`, `markNodeCompleted` nhận score
- [ ] **Thay 2 query native SQL** trong `NodeProgressRepository` (`countRelevantNodesForStudent`, `countCompletedNodesForStudent`) — cả 2 đang join `student_branch_assignments`. Viết lại không đụng bảng branching (đếm node VISIBLE / node_progress completed). **Bắt buộc xong TRƯỚC bước 7**: native SQL không lỗi compile nên rule "không còn tham chiếu branching" trong DoD không bắt được — DROP TABLE xong là crash lúc chạy (phát hiện 2026-07-06)
- [ ] `roadmap.html`: bỏ UI branch, thêm badge "Lv. X/N" + bestScore
- [ ] **Header roadmap 2 chỉ số** (S1 §6, rev 06c): `Tiến độ: K/N bài` (chữ "đã học", không "hoàn thành") + `Điểm: X/Y` (X = Σ bestScore, Y = Σ maxScore của level cao nhất mỗi node có câu hỏi) — hai thước đo coverage/mastery cùng hiện diện thì "trượt hết vẫn 100% tiến độ" tự giải thích, không cần đỡ bằng miệng
- [ ] Empty-state roadmap khi chưa node nào VISIBLE: "Giảng viên chưa mở bài học nào." (vá §9, chống bẫy demo lớp mới)
- [ ] `node-detail.html`: danh sách level + trạng thái nút Start Test (khóa theo passingScore level trước / hết lượt / level 1 luôn mở — S2), node mà mọi level đều 0 câu hỏi = lesson thường, hoàn thành tay (OQ2 đã chốt)
- [ ] Bảng lịch sử attempt per level (S6): số lượt, best score, pass/fail đọc từ cờ `passed` đã lưu (không so lại passingScore hiện tại — rev 2026-07-05b), "Chưa làm"; nhãn "Hết thời gian" theo cờ `timedOut` — HOÃN cùng enforcement duration (2026-07-06)

---

## 3. Trình tự & phụ thuộc

```
Tuần 1            Tuần 2                       Tuần 3
────────────────  ───────────────────────────  ────────────────────
Nền móng (Hieu)   Material (Minh)       ┐
  ✅ đã merge     Cấu hình Level (Tung) ├─ song song, mock qua repo
  dev b64d1fd     Làm bài (admin)       │  của Nền móng
  ↓               Roadmap (Duc)         ┘
                  Kết quả GV L5 (Hieu) — chỉ đọc repo + seed, độc lập
                  tích hợp Làm bài↔Roadmap (unlock, NodeProgress)
                  tích hợp Material↔Cấu hình Level (chọn Material)
                                               Bước 7: gỡ branching
                                               (Hiếu; admin+Tùng hỗ trợ,
                                               SAU CÙNG)
                                               Test e2e cả 2 role
```

Quy tắc:
1. **Không ai đụng bước 7 (xóa branching) cho tới khi cả 5 phần merge xong** — spec đã cố ý tách "add trước, drop sau".
2. Điểm giao Làm bài ↔ Roadmap: định nghĩa "level unlocked" (rev 06d — ratchet đã gỡ, ngưỡng bất biến L3 §9) = có attempt SUBMITTED của level N−1 với `passed = 1` — đặt 1 method chung trong `NodeLevelAttemptService`, phần Roadmap chỉ gọi, không tự tính lại. Badge pass/fail (S6, L5) đọc cờ `passed` đã lưu.
3. Điểm giao Material ↔ Cấu hình Level: phần Cấu hình Level chỉ cần `MaterialRepository.findBySubjectId()` — Nền móng cung cấp sẵn, không chờ phần Material xong UI.
4. Error message dùng **nguyên văn** chuỗi tiếng Việt trong spec — không tự chế.

## 4. Definition of Done (mỗi phần)

- Acceptance scenarios của user story tương ứng chạy được bằng tay trên UI
- Unit test cho validation/thuật toán của phần mình
- Không còn tham chiếu compile-time tới class branching mới phát sinh trong code mới
- PR được Hieu review + merge vào `dev`

## 5. Git workflow

- Nhánh: `feat/<slug>-<tên ngắn>` tách từ `dev`, slug lấy theo §2
  (`foundation`, `material`, `level-config`, `level-test`, `roadmap`, `results`).
  Phần lớn nên chia nhiều PR nhỏ (`feat/level-test-question-service`,
  `feat/level-test-attempt-service`…), mỗi PR ≤ ~400 dòng logic.
- **Chỉ Hiếu merge vào `dev`** — kể cả PR của chính mình cũng chờ Hiếu bấm.
  Không force-push `dev`/`main`; nhánh cá nhân tự do.
- Tuần 1 chỉ Nền móng được merge. 4 người còn lại code trước trên nhánh riêng được,
  nhưng **rebase lên `dev` ngay khi Nền móng merge** và mỗi khi `dev` đổi.
- **Migration chỉ-thêm**: không sửa file migration đã merge; cần đổi schema thì
  thêm file mới + nhắn nhóm "chạy lại DB". Đây là nguồn lỗi "máy tao chạy được" tệ nhất.
- Bước 7 là PR riêng `chore/remove-branching` **do Hiếu mở**, SAU khi cả 5 phần nằm trong `dev`.
  **PR tuần 1–2 không được xóa bất kỳ file branching nào** — chỉ gỡ điểm gọi trong
  phạm vi phần mình. Hiếu soi mục này đầu tiên khi review.
- Hiếu merge PR bằng `--no-ff` (`git merge --no-ff feat/<slug>-…`), message dạng
  `merge feat/<slug>: <tóm tắt ngắn>` — `git log dev --merges --oneline` đủ trace từng phần.
- **Thứ tự merge tuần 2** khi các phần chạm file chung: `roadmap` (Đức — đổi
  `StudentRoadmapService` nhiều nhất) → `level-test` (admin) → `results` (Hiếu, chỉ đọc).
- **Thứ tự bước 7**: merge PR code xóa branching TRƯỚC → chạy migration DROP SAU
  (trên máy mỗi người cũng theo thứ tự đó) — schema và code phải đồng bộ; smoke test
  roadmap + node detail ngay sau khi DROP.
- `dev` → `main` chỉ merge 2 mốc: sau tích hợp tuần 2 chạy ổn, và bản chốt trước bảo vệ.
- Commit message tiếng Việt, viết thường, không prefix — lọc lịch sử theo phần
  thì dùng tên nhánh (`feat/<slug>-…`), không dựa vào message.

## 6. Bản đồ code tham khảo (bắt chước class có sẵn trong repo chính)

| Phần | Code mới | Bắt chước |
|---|---|---|
| Material | `MaterialService` + `LecturerMaterialController` | `QuestionBankService` + `LecturerQuestionBankController` (CRUD + ownership cùng kiểu) |
| Cấu hình Level | `NodeLevelService` | `AssessmentService` (validate + ném `IllegalArgumentException`/`IllegalStateException` message tiếng Việt) |
| Làm bài + chấm điểm | `NodeLevelAttemptService` | `StudentAssessmentService` — copy cấu trúc: resume IN_PROGRESS, submit idempotent, chấm ngay không lưu answer |
| Roadmap | sửa `StudentRoadmapService` | chính file đó — việc chủ yếu là *xóa* logic branch, giữ LOCKED theo prerequisite |
| Kết quả GV (L5) | controller kết quả + templates | `LecturerMemberController` (`/lecturer/classes/{classroomId}/members`) — cùng kiểu ownership check + bảng theo classroom; đọc trực tiếp `NodeLevelAttemptRepository`/`AttemptQuestionRepository` |

## 7. Thỏa thuận giao diện giữa các phần (chốt tuần 1)

1. **`NodeProgressService` thuộc sở hữu phần Roadmap (Đức)**. Phần Làm bài chỉ *gọi*
   method upsert `bestScore`/`bestLevelNumber` — Đức + admin thống nhất chữ ký method
   ngay tuần 1 (chưa cần code, chỉ cần interface).
2. Việc **gỡ form branch khỏi path builder** (`LearningNodeForm`,
   `buildAddBranchForm()`) của phần Cấu hình Level **dời sang tuần 3**, làm cùng
   bước 7 (PR `chore/remove-branching` của Hiếu) — gỡ sớm sẽ gãy chức năng
   branching đang còn sống trong tuần 1–2. **Bổ sung rev 06d**: ngay trong đợt merge
   tuần 2, SAU khi PR `roadmap` merge (từ đó nhánh soạn mới không còn tác dụng phía
   học sinh), **ẨN form branch trên UI** — không xóa code, 1 PR nhỏ (Tùng hoặc gộp
   vào PR roadmap của Đức, chốt buổi brief) — để không lọt cửa sổ "soạn được nhưng
   chết" vào bản demo nếu bước 7 bị cắt (§8 mục 1); kèm script HIDE node PASS/FAIL
   cũ (spec mục "Chuyển tiếp cho lớp/dữ liệu phân nhánh cũ").
3. URL route giữa Làm bài ↔ Roadmap (nút Start Test, CTA trang kết quả) dùng
   **nguyên văn** route trong spec, không tự chế biến thể.
4. **Unlock cho Roadmap** (thêm 2026-07-06): tuần 1 admin commit sớm skeleton
   `NodeLevelAttemptService` với method `isNextLevelUnlocked(levelId, studentId,
   classroomId)` stub (tạm return false) để Đức inject và code song song — KHÔNG
   tạo interface riêng (service trong repo đều là concrete class), KHÔNG để Đức
   tự tính lại luật unlock (§3 quy tắc 2 — rev 06d chỉ còn check `passed = 1`).
5. **Checklist review PR content-thuộc-material của Minh** (thêm 2026-07-06 — bảng
   `node_contents` có 2 chủ; phía node đã an toàn theo cấu trúc vì mọi query lọc
   `node_id = ?`): (a) đường authz mới `findByIdAndMaterial_Id` + check ownership
   qua material→subject; (b) service chặn row có CẢ node_id và material_id — DB
   CHECK chỉ ép "ít nhất một"; (c) xóa material phải xử content con — chặn xóa,
   nhất quán với L2 §4.

## 8. Kỷ luật bảo vệ (chốt 2026-07-06)

- **Ngày freeze**: trước bảo vệ 4–5 ngày merge `dev` → `main`; sau mốc đó chỉ nhận
  bugfix, không nhận feature.
- **Bước 7 chỉ làm nếu còn ≥ 1 tuần đệm** sau khi nó merge. Không đủ → bỏ, khai với
  hội đồng là "deprecated, có kế hoạch gỡ" — hội đồng không nhìn thấy code chết,
  branching nằm im trong repo không làm bẩn demo.
- **Thứ tự cắt khi trễ tiến độ** (cắt từ trên xuống; L6/L7 bổ sung rev 06c —
  cả hai cắt được SẠCH: bảng `attempt_grants` không có nút thì Σ = 0, công thức
  chặn lượt tự về như cũ; không ai bấm "Kết thúc lớp" thì check `COMPLETED`
  không bao giờ kích hoạt — code lõi không phải sửa gì khi cắt):
  1. Bước 7 gỡ branching — vô hình với hội đồng
  2. ~~Enforce `durationMinutes`/timeout~~ — **đã cắt chủ động 2026-07-06** (hoãn sau bảo vệ, form ẩn field duration)
  3. L7 nút "+1 lượt" — câu trả lời "hết lượt" lùi về workaround cấp lớp (nới `maxAttempts`; hạ `passingScore` KHÔNG còn là workaround — ngưỡng bất biến khi level có attempt, rev 06d)
  4. Review từng câu (S5 §6) + drill-down snapshot của L5 §3 — giữ bảng điểm tổng
  5. L6 "Kết thúc lớp"/`COMPLETED` + trang tổng kết — nếu cắt, **chuyển nút xuất CSV + dòng tổng hợp về thẳng trang L5** (2 thứ đó thuộc L5 §5, không phụ thuộc COMPLETED) để đoạn kết demo vẫn còn bảng điểm
  6. Clone path copy levels (L1 §4) — khai "hạn chế hiện tại"
  - **KHÔNG cắt** (P1, mất là mất câu chuyện): cấu hình level (L3/L4), làm bài +
    chấm + unlock (S3/S4), roadmap hiển thị level (S1/S2).
- **Demo chạy trên DB seed sẵn** kịch bản đẹp — không nhập dữ liệu tay trước hội
  đồng; seed attempt làm ngay sau migration follow-up, không dồn cuối tuần 2.

## 9. Kịch bản bảo vệ — nghiệp vụ (thêm 2026-07-06, sửa cùng ngày theo hướng "giải thật")

Đánh giá tổng thể trước bảo vệ kết luận: dự án không thiếu giá trị lõi — nó thiếu
**phần kết của vòng đời lớp học** và các vòng nghiệp vụ chưa khép ở rìa. Quyết định
(chốt với Hiếu 2026-07-06): **giải bằng tính năng thật, không chỉ chuẩn bị lời** —
spec rev 2026-07-06c thêm L6 (Kết thúc lớp + trang tổng kết + CSV), L7 (cấp lượt
per-student qua `attempt_grants`), S1 §6 (header 2 chỉ số), nhãn tiên quyết + các
vá kèm theo (mục "Vá nghiệp vụ kèm theo" trong spec). Lời kể (§9.1–9.3) giờ đứng
TRÊN nền tính năng thật, không thay cho nó.

### 9.1 Câu định vị (mở đầu buổi bảo vệ, mọi phần demo quy về nó)

> "Google Classroom quản lý **giao tiếp** của lớp học; Mentora quản lý **con đường
> học**: giảng viên thiết kế lộ trình có trạm, mỗi trạm có thang trình độ (level),
> học sinh leo từng bậc, giảng viên nhìn thấy cả lớp đang kẹt ở đâu."

Câu này trả lời trước câu hỏi tử huyệt "sao không dùng Google Classroom/Moodle?".
Phần lớp học/mã mời/QnA giới thiệu là **hạ tầng phục vụ lộ trình**, không nhận là
điểm khác biệt.

### 9.2 Mạch demo — khép vòng đời ĐẦU → GIỮA → CUỐI

1. **Đầu (nhập học)**: tạo lớp bằng wizard → học sinh join mã mời → giảng viên duyệt.
2. **Giữa (dạy-học, phần hay nhất — dành nhiều thời lượng nhất)**: giảng viên mở
   node theo tiến độ dạy (kể như tính năng sư phạm "mở bài dần") → học sinh học,
   làm level test, trượt → thử lại → qua (unlock level tiếp theo) → review từng câu sai.
3. **Cuối (tổng kết — trước đây KHÔNG tồn tại, nay là tính năng thật L6)**: mở L5
   → dòng tổng hợp "cả lớp yếu node X" (dạy bù được) → một em hết lượt, bấm
   **"+1 lượt"** ngay tại chỗ (L7 — tình huống đời thường giải bằng nút, không bằng
   miệng) → bấm **"Kết thúc lớp"** → trang tổng kết → **xuất CSV** "căn cứ điểm
   quá trình cuối kỳ".

Lưu ý demo: PHẢI có bước bật hiển thị node — lớp mới tạo mặc định mọi node ẨN,
quên bật là roadmap trống trước hội đồng. Chạy trên DB seed sẵn (§8).

### 9.3 Câu hỏi dự kiến của hội đồng + trả lời chuẩn (cả nhóm thuộc, trả lời một hơi)

| Câu hỏi | Trả lời |
|---|---|
| Trượt hết mà tiến độ vẫn 100%? | Chỉ vào header roadmap (S1 §6): hai chỉ số đứng cạnh nhau — `Tiến độ K/N` (coverage, đã-đi-hết-con-đường) và `Điểm X/Y` (mastery, giỏi-đến-đâu). Trượt hết = tiến độ đầy nhưng cột điểm trống — màn hình tự nói. Cố ý không giam học sinh ở một bài vì trượt; UI dùng chữ "đã học" (OQ6). |
| Sao không dùng Google Classroom? | Câu định vị §9.1. |
| Đóng lớp mà học sinh vẫn làm bài được? | Hai trạng thái khác nhau: CLOSE = **đóng ghi danh** (chặn join mới, lớp vẫn dạy-học); còn kết thúc thật là **"Kết thúc lớp"** (L6) — chặn lượt làm mới, dẫn tới trang tổng kết + xuất bảng điểm; demo cho xem luôn. |
| Môn tiên quyết dùng làm gì? | Hệ thống **kiểm tra thật** tại bước duyệt: yêu cầu join của em chưa qua môn tiên quyết (chưa từng ACTIVE trong một lớp COMPLETED của môn đó — L6 §5) bị gắn nhãn đỏ "Chưa đạt tiên quyết: X" cho giảng viên quyết. Không chặn cứng vì học sinh có thể đã học môn đó ngoài hệ thống — máy cảnh báo, người quyết. |
| Soạn bài 5 bước có rườm không? | Material là **đầu tư tái sử dụng**: một chủ đề soạn một lần (nội dung + pool câu hỏi), dùng cho nhiều node, nhiều lớp, nhiều kỳ — chi phí soạn khấu hao theo số lớp. |
| Em lỡ tay nộp / ốm, hết lượt thì sao? | Bấm nút **"+1 lượt"** cho đúng em đó ngay trên màn kết quả (L7) — có vết ai cấp, lúc nào (`granted_by`, `granted_at`). Kèm công cụ cấp lớp: nới `maxAttempts` (hạ `passingScore` không dùng được — ngưỡng bất biến khi level đã có attempt, rev 06d; đây là câu chuyện liêm chính dữ liệu, kể như điểm mạnh). **Demo nút này** — đừng trả lời bằng miệng. |
| Có giới hạn thời gian làm bài không? | Cột `duration_minutes` có sẵn trong schema; enforcement là hạng mục sau bảo vệ (cắt chủ động — §8 mục 2). |
| Sửa đề / chấm lại bài đã nộp? | Không, **theo thiết kế**: đề mỗi lượt là snapshot bất biến — sửa ngân hàng câu hỏi không đổi bài đã phát, điểm đã chấm không bao giờ dịch. Đây là câu chuyện **liêm chính dữ liệu** — chủ động kể như điểm mạnh. |
| Hệ thống giúp gì cho người dạy? | Dòng tổng hợp L5 ("node nào cả lớp gãy → dạy bù") + CSV bảng điểm + mở bài theo tiến độ + xem đúng bài từng em đã nhận. |

### 9.4 Các gói giải-thật (rev 2026-07-06c) — vị trí trong kế hoạch

Nguyên tắc chốt: **cắm dây vào hoặc rút hẳn ra, không để tính năng lơ lửng**;
chỉ mục 6 (định vị) là thuần lời kể — và nó đứng được vì 5 gói kia là bằng chứng
vật chất. Tổng thêm ~4–5 ngày công, KHÔNG chạm file migration đã merge (chỉ
`attempt_grants` chạm schema và đi ké file follow-up chưa viết).

| Gói | Nội dung | Nằm ở đâu | Ai |
|---|---|---|---|
| 1. Vòng đời có CUỐI | `COMPLETED` + "Kết thúc lớp" + trang tổng kết + CSV (L6) | phần `results` | Hiếu (+1 check của admin) |
| 2. Tiên quyết thật | nhãn "Chưa đạt tiên quyết: X" trên trang duyệt (spec mục "Vá nghiệp vụ kèm theo" §1; cần định nghĩa "đã qua môn" từ gói 1) | ngoài node-levels, PR nhỏ riêng | Minh hoặc Tùng (chốt buổi brief) |
| 3. Cấp lượt per-student | bảng `attempt_grants` + luật S3 §4 + nút "+1 lượt" (L7) | migration: nền móng (Hiếu); luật: `level-test` (admin); nút: `results` (Hiếu) | 3 người, mỗi người phần mình |
| 4. Hai thước đo hiển thị | header roadmap `Tiến độ K/N` + `Điểm X/Y` (S1 §6) + empty-state | phần `roadmap` | Đức |
| 5. Cắm/rút việc vặt | gỡ thành viên (BANNED có sẵn); nút "Hiện tất cả node"; bỏ badge email giả; label "Đóng ghi danh" (spec mục "Vá nghiệp vụ kèm theo" §2–5) | ngoài node-levels, PR nhỏ | Minh/Tùng/Đức chia theo tải, chốt buổi brief |
| 6. Định vị (§9.1) | thuần trình bày — kể SAU khi 1–5 đã xây | slide + tập demo | cả nhóm |

Thứ tự bắt buộc: **gói 3 phần migration chốt TRƯỚC khi Hiếu viết file follow-up**
(tuần này — đã gộp vào đầu việc nền móng); gói 1 → 2 nối nhau tuần 2 (nhãn tiên
quyết cần trạng thái COMPLETED tồn tại); gói 4 nằm trong PR roadmap sẵn có của Đức.
Xác thực email thật (SMTP) chốt là hạng mục **SAU bảo vệ** — không đưa dịch vụ
mail bên thứ ba vào đường demo; trước bảo vệ chỉ gỡ badge giả (trung thực dữ liệu).
